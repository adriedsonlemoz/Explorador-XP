package com.exploradorxp.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private data class ArchiveBrowserItem(
    val path: String,
    val name: String,
    val directory: Boolean,
    val size: Long,
    val compressedSize: Long,
    val modifiedAt: Long,
    val source: ArchiveEntryInfo?,
)

private enum class ArchivePasswordAction { PREVIEW, VERIFY }
private data class ArchivePasswordRequest(val action: ArchivePasswordAction, val entryPath: String? = null)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ArchiveZipViewer(
    file: File,
    onPreviewFile: (File) -> Unit,
    onOpenExternal: (File) -> Unit,
    onOpenFolder: (File) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var infoResult by remember(file.absolutePath, file.lastModified()) { mutableStateOf<Result<ZipArchiveInfo>?>(null) }
    var currentPath by remember(file.absolutePath) { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(ArchiveSortMode.NAME) }
    var ascending by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var detailsEntry by remember { mutableStateOf<ArchiveBrowserItem?>(null) }
    var showArchiveDetails by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showExtraction by remember { mutableStateOf(false) }
    var verifyProgress by remember { mutableStateOf<ArchiveProgress?>(null) }
    var verifyMessage by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf<CharArray?>(null) }
    var passwordRequest by remember { mutableStateOf<ArchivePasswordRequest?>(null) }

    LaunchedEffect(file.absolutePath, file.lastModified()) {
        infoResult = ArchiveManager.readZip(file)
    }

    val archiveInfo = infoResult?.getOrNull()
    val visibleItems = remember(archiveInfo, currentPath, query, sortMode, ascending) {
        archiveInfo?.let { buildBrowserItems(it.entries, currentPath, query, sortMode, ascending) }.orEmpty()
    }

    BackHandler(enabled = selected.isNotEmpty() || currentPath.isNotBlank() || query.isNotBlank()) {
        when {
            selected.isNotEmpty() -> selected = emptySet()
            query.isNotBlank() -> query = ""
            currentPath.isNotBlank() -> currentPath = currentPath.substringBeforeLast('/', "")
        }
    }

    fun previewEntry(path: String) {
        val encrypted = archiveInfo?.entries?.firstOrNull { it.path == path }?.encrypted == true
        if (encrypted && password == null) {
            passwordRequest = ArchivePasswordRequest(ArchivePasswordAction.PREVIEW, path)
            return
        }
        scope.launch {
            val result = ArchiveManager.extractEntryToCache(file, path, context.cacheDir, password)
            result.onSuccess { extracted ->
                if (supportsInternalViewer(extracted)) onPreviewFile(extracted) else onOpenExternal(extracted)
            }.onFailure { error ->
                if (error is ArchivePasswordRequiredException || error is ArchivePasswordIncorrectException) {
                    password = null
                    passwordRequest = ArchivePasswordRequest(ArchivePasswordAction.PREVIEW, path)
                } else {
                    verifyMessage = error.message ?: "Não foi possível abrir o item."
                }
            }
        }
    }

    fun verifyArchive() {
        if (archiveInfo?.encrypted == true && password == null) {
            passwordRequest = ArchivePasswordRequest(ArchivePasswordAction.VERIFY)
            return
        }
        verifyMessage = null
        scope.launch {
            val result = ArchiveManager.verifyIntegrity(file, password) { verifyProgress = it }
            verifyProgress = null
            result.onSuccess { verifyMessage = "Integridade verificada: nenhuma falha encontrada." }
                .onFailure { error ->
                    if (error is ArchivePasswordRequiredException || error is ArchivePasswordIncorrectException) {
                        password = null
                        passwordRequest = ArchivePasswordRequest(ArchivePasswordAction.VERIFY)
                    } else {
                        verifyMessage = "Falha na verificação: ${error.message ?: "arquivo inválido"}"
                    }
                }
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().background(XpPanel).padding(horizontal = 7.dp, vertical = 5.dp),
        ) {
            ArchiveSmallButton("↑", enabled = currentPath.isNotBlank()) {
                currentPath = currentPath.substringBeforeLast('/', "")
                selected = emptySet()
            }
            Spacer(Modifier.width(5.dp))
            Column(Modifier.weight(1f)) {
                val countText = archiveInfo?.let { "${it.fileCount} arquivos • ${it.directoryCount} pastas" } ?: "Lendo conteúdo..."
                Text(countText, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (archiveInfo != null) {
                    val ratio = archiveInfo.compressionRatio?.let { " • compressão $it%" }.orEmpty()
                    Text(
                        "${archiveFormatBytes(file.length())} compactado • ${archiveFormatBytes(archiveInfo.uncompressedBytes)} descompactado$ratio",
                        fontSize = 10.sp,
                        color = XpTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            ArchiveSmallButton("Info") { showArchiveDetails = true }
            Spacer(Modifier.width(4.dp))
            ArchiveSmallButton("Verificar", enabled = archiveInfo != null) { verifyArchive() }
            Spacer(Modifier.width(4.dp))
            ArchiveSmallButton(if (selected.isEmpty()) "Extrair" else "Extrair ${selected.size}", enabled = archiveInfo != null) { showExtraction = true }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(38.dp).background(Color(0xFFF8FAFD)).border(1.dp, XpControlBorder).padding(horizontal = 7.dp),
        ) {
            Text(
                if (currentPath.isBlank()) "ZIP:/" else "ZIP:/$currentPath",
                fontSize = 11.sp,
                color = XpTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box {
                ArchiveSmallButton("Ordenar") { showSortMenu = true }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(Color(0xFFF8F8F2)).border(1.dp, XpControlBorder),
                ) {
                    ArchiveMenuText("Nome", sortMode == ArchiveSortMode.NAME) { sortMode = ArchiveSortMode.NAME; showSortMenu = false }
                    ArchiveMenuText("Tamanho", sortMode == ArchiveSortMode.SIZE) { sortMode = ArchiveSortMode.SIZE; showSortMenu = false }
                    ArchiveMenuText("Tipo", sortMode == ArchiveSortMode.TYPE) { sortMode = ArchiveSortMode.TYPE; showSortMenu = false }
                    ArchiveMenuText("Data", sortMode == ArchiveSortMode.DATE) { sortMode = ArchiveSortMode.DATE; showSortMenu = false }
                    HorizontalDivider()
                    ArchiveMenuText(if (ascending) "Crescente ✓" else "Crescente") { ascending = true; showSortMenu = false }
                    ArchiveMenuText(if (!ascending) "Decrescente ✓" else "Decrescente") { ascending = false; showSortMenu = false }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 7.dp, vertical = 5.dp),
        ) {
            Box(
                modifier = Modifier.weight(1f).height(30.dp).background(Color.White).border(1.dp, Color(0xFF8EA6C7)).padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isBlank()) Text("Pesquisar dentro do ZIP", color = Color(0xFF888888), fontSize = 12.sp)
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFF202020)),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotBlank()) {
                Spacer(Modifier.width(5.dp))
                ArchiveSmallButton("Limpar") { query = "" }
            }
        }

        if (selected.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().background(Color(0xFFEAF2FB)).padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                Text("${selected.size} selecionado(s)", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                ArchiveSmallButton("Limpar seleção") { selected = emptySet() }
            }
        }

        if (verifyMessage != null) {
            Text(
                verifyMessage.orEmpty(),
                fontSize = 11.sp,
                color = Color(0xFF334455),
                modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF8D9)).padding(horizontal = 9.dp, vertical = 6.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(28.dp).background(Color(0xFFEAF2FB)).border(1.dp, Color(0xFFC9D8E8)).padding(horizontal = 10.dp),
        ) {
            Text("Nome", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Tamanho", fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(80.dp))
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                infoResult == null -> LoadingArchivePanel("Lendo conteúdo...")
                infoResult?.isFailure == true -> Text(
                    infoResult?.exceptionOrNull()?.message ?: "Não foi possível ler o ZIP.",
                    modifier = Modifier.align(Alignment.Center).padding(18.dp),
                    textAlign = TextAlign.Center,
                )
                visibleItems.isEmpty() -> Text(
                    if (query.isBlank()) "Esta pasta do ZIP está vazia." else "Nenhum item encontrado.",
                    modifier = Modifier.align(Alignment.Center).padding(18.dp),
                    color = XpTextSecondary,
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(visibleItems, key = { it.path }) { item ->
                        var menu by remember(item.path) { mutableStateOf(false) }
                        val isSelected = item.path in selected
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) XpSelection else Color.White)
                                .combinedClickable(
                                    onClick = {
                                        if (selected.isNotEmpty()) {
                                            selected = togglePath(selected, item.path)
                                        } else if (item.directory) {
                                            currentPath = item.path
                                            query = ""
                                        } else {
                                            previewEntry(item.path)
                                        }
                                    },
                                    onLongClick = { selected = togglePath(selected, item.path) },
                                )
                                .padding(horizontal = 9.dp, vertical = 7.dp),
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(FileIconMapper.iconFor(File(item.name), item.directory)),
                                contentDescription = null,
                                modifier = Modifier.size(29.dp),
                                contentScale = ContentScale.Fit,
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val detail = when {
                                    item.directory -> "Pasta"
                                    item.source?.encrypted == true -> "${item.source.compressionMethod} • protegido por senha"
                                    else -> item.source?.compressionMethod ?: "Arquivo"
                                }
                                Text(detail, fontSize = 10.sp, color = XpTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(
                                if (item.directory) "Pasta" else archiveFormatBytes(item.size),
                                fontSize = 11.sp,
                                color = XpTextSecondary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(74.dp),
                            )
                            Box {
                                Text(
                                    "⋮",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { menu = true }.padding(horizontal = 7.dp, vertical = 2.dp),
                                )
                                DropdownMenu(
                                    expanded = menu,
                                    onDismissRequest = { menu = false },
                                    modifier = Modifier.background(Color(0xFFF8F8F2)).border(1.dp, XpControlBorder),
                                ) {
                                    ArchiveMenuText(if (item.directory) "Abrir pasta" else "Visualizar") {
                                        menu = false
                                        if (item.directory) {
                                            currentPath = item.path
                                            query = ""
                                        } else previewEntry(item.path)
                                    }
                                    ArchiveMenuText("Extrair este item") {
                                        menu = false
                                        selected = setOf(item.path)
                                        showExtraction = true
                                    }
                                    ArchiveMenuText("Informações") { menu = false; detailsEntry = item }
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFE0E6EE))
                    }
                }
            }
        }
    }

    if (showExtraction && archiveInfo != null) {
        ArchiveExtractionFlow(
            zipFile = file,
            archiveInfo = archiveInfo,
            selectedPaths = selected.takeIf { it.isNotEmpty() },
            initialQuick = false,
            initialPassword = password,
            onPasswordAccepted = { password = it },
            onDismiss = { showExtraction = false },
            onOpenFolder = onOpenFolder,
            onOpenFile = onPreviewFile,
        )
    }

    detailsEntry?.let { entry ->
        ArchiveEntryDetailsDialog(entry) { detailsEntry = null }
    }

    if (showArchiveDetails && archiveInfo != null) {
        ArchiveInfoDialog(file, archiveInfo) { showArchiveDetails = false }
    }

    verifyProgress?.let { progress ->
        ArchiveProgressDialog(
            title = "Verificando integridade",
            progress = progress,
            cancellable = false,
            onCancel = {},
        )
    }

    passwordRequest?.let { request ->
        ArchivePasswordDialog(
            onDismiss = { passwordRequest = null },
            onConfirm = { entered ->
                password = entered.toCharArray()
                passwordRequest = null
                when (request.action) {
                    ArchivePasswordAction.PREVIEW -> request.entryPath?.let(::previewEntry)
                    ArchivePasswordAction.VERIFY -> verifyArchive()
                }
            },
        )
    }
}

@Composable
internal fun ArchiveExtractionFlow(
    zipFile: File,
    archiveInfo: ZipArchiveInfo,
    selectedPaths: Set<String>? = null,
    initialQuick: Boolean,
    initialPassword: CharArray? = null,
    onPasswordAccepted: (CharArray) -> Unit = {},
    onDismiss: () -> Unit,
    onOpenFolder: (File) -> Unit,
    onOpenFile: (File) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val archivePrefs = remember(context) {
        context.getSharedPreferences("archive_extraction", android.content.Context.MODE_PRIVATE)
    }
    var destination by remember(zipFile.absolutePath, initialQuick) {
        val archiveParent = zipFile.parentFile ?: Environment.getExternalStorageDirectory()
        val remembered = archivePrefs.getString("last_destination", null)
            ?.let(::File)
            ?.takeIf { it.exists() && it.isDirectory }
        mutableStateOf(if (initialQuick) archiveParent else remembered ?: archiveParent)
    }
    var createNamedFolder by remember { mutableStateOf(true) }
    var openWhenDone by remember { mutableStateOf(false) }
    var conflictMode by remember { mutableStateOf(ArchiveConflictMode.RENAME) }
    var showPicker by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf(if (initialQuick) "progress" else "config") }
    var progress by remember { mutableStateOf(ArchiveProgress(0, 0, 0, 0, ArchiveManager.requiredBytes(archiveInfo, selectedPaths), 0, "")) }
    var summary by remember { mutableStateOf<ArchiveExtractionSummary?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf(initialPassword) }
    var askPassword by remember { mutableStateOf(initialQuick && archiveInfo.encrypted && initialPassword == null) }
    var extractionJob by remember { mutableStateOf<Job?>(null) }
    val notifier = remember { ArchiveProgressNotifier(context) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* A extração continua mesmo se a notificação for recusada. */ }

    fun startExtraction() {
        if (archiveInfo.encrypted && password == null) {
            askPassword = true
            return
        }
        if (!initialQuick) {
            archivePrefs.edit().putString("last_destination", destination.absolutePath).apply()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        stage = "progress"
        errorText = null
        extractionJob?.cancel()
        extractionJob = scope.launch {
            notifier.start(zipFile.name)
            val result = ArchiveManager.extract(
                zipFile = zipFile,
                destinationParent = destination,
                createNamedFolder = createNamedFolder,
                selectedPaths = selectedPaths,
                conflictMode = conflictMode,
                password = password,
                onProgress = {
                    progress = it
                    notifier.update(it)
                },
            )
            notifier.finish()
            result.onSuccess {
                summary = it
                stage = "done"
                if (openWhenDone) onOpenFolder(it.destination)
            }.onFailure { error ->
                if (error is ArchivePasswordRequiredException || error is ArchivePasswordIncorrectException) {
                    password = null
                    askPassword = true
                    stage = "config"
                } else if (error is kotlinx.coroutines.CancellationException) {
                    errorText = "Extração cancelada. Arquivos já concluídos foram mantidos."
                    stage = "config"
                } else {
                    errorText = error.message ?: "Falha ao extrair o arquivo."
                    stage = "config"
                }
            }
        }
    }

    LaunchedEffect(initialQuick) {
        if (initialQuick && !askPassword) startExtraction()
    }

    when (stage) {
        "config" -> XpDialogFrame(title = "Extrair arquivo ZIP", onDismiss = onDismiss) {
            val required = ArchiveManager.requiredBytes(archiveInfo, selectedPaths)
            Text("Destino da extração", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, XpControlBorder).padding(7.dp),
            ) {
                Text(destination.absolutePath, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(6.dp))
                XpDialogButton("Alterar", onClick = { showPicker = true })
            }
            Spacer(Modifier.height(8.dp))
            ArchiveToggleRow("Criar pasta com o nome do ZIP", createNamedFolder) { createNamedFolder = !createNamedFolder }
            ArchiveToggleRow("Abrir pasta quando terminar", openWhenDone) { openWhenDone = !openWhenDone }
            Spacer(Modifier.height(7.dp))
            Text("Se já existir um arquivo com o mesmo nome:", fontSize = 11.sp, color = XpTextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth().padding(top = 5.dp)) {
                ArchiveChoiceButton("Renomear", conflictMode == ArchiveConflictMode.RENAME, Modifier.weight(1f)) { conflictMode = ArchiveConflictMode.RENAME }
                ArchiveChoiceButton("Substituir", conflictMode == ArchiveConflictMode.REPLACE, Modifier.weight(1f)) { conflictMode = ArchiveConflictMode.REPLACE }
                ArchiveChoiceButton("Ignorar", conflictMode == ArchiveConflictMode.SKIP, Modifier.weight(1f)) { conflictMode = ArchiveConflictMode.SKIP }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Necessário: ${archiveFormatBytes(required)} • Livre no destino: ${archiveFormatBytes(destination.usableSpace)}",
                fontSize = 11.sp,
                color = if (destination.usableSpace in 1 until required) Color(0xFF9C1B12) else XpTextSecondary,
            )
            if (errorText != null) {
                Spacer(Modifier.height(7.dp))
                Text(errorText.orEmpty(), fontSize = 11.sp, color = Color(0xFF9C1B12))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                XpDialogButton("Cancelar", onClick = onDismiss, modifier = Modifier.weight(1f))
                XpDialogButton("Extrair", enabled = destination.usableSpace <= 0L || destination.usableSpace >= required, onClick = { startExtraction() }, modifier = Modifier.weight(1f))
            }
        }
        "progress" -> ArchiveProgressDialog(
            title = "Extraindo ${zipFile.name}",
            progress = progress,
            cancellable = true,
            onCancel = {
                extractionJob?.cancel()
                notifier.finish()
                stage = "config"
                errorText = "Extração cancelada."
            },
        )
        "done" -> XpDialogFrame(title = "Extração concluída", onDismiss = onDismiss) {
            val result = summary
            if (result != null) {
                Text(
                    "${result.extracted} extraídos • ${result.renamed} renomeados • ${result.skipped} ignorados • ${result.errors} erros",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(result.destination.absolutePath, fontSize = 11.sp, color = XpTextSecondary)
                if (result.errorMessages.isNotEmpty()) {
                    Spacer(Modifier.height(7.dp))
                    Text(result.errorMessages.joinToString("\n"), fontSize = 10.sp, color = Color(0xFF8B3A30), maxLines = 5, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    XpDialogButton("Fechar", onClick = onDismiss, modifier = Modifier.weight(1f))
                    if (result.extractedFiles == 1 && result.primaryExtractedFile?.isFile == true) {
                        XpDialogButton(
                            "Abrir arquivo",
                            onClick = { result.primaryExtractedFile?.let(onOpenFile) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    XpDialogButton("Abrir pasta", onClick = { onOpenFolder(result.destination) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    if (showPicker) {
        ArchiveFolderPickerDialog(
            initial = destination,
            onDismiss = { showPicker = false },
            onChoose = {
                destination = it
                archivePrefs.edit().putString("last_destination", it.absolutePath).apply()
                showPicker = false
            },
        )
    }

    if (askPassword) {
        ArchivePasswordDialog(
            onDismiss = { askPassword = false; if (initialQuick) onDismiss() },
            onConfirm = { text ->
                password = text.toCharArray()
                onPasswordAccepted(password!!)
                askPassword = false
                startExtraction()
            },
        )
    }
}

@Composable
private fun ArchiveProgressDialog(
    title: String,
    progress: ArchiveProgress,
    cancellable: Boolean,
    onCancel: () -> Unit,
) {
    Dialog(onDismissRequest = { if (cancellable) onCancel() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0x99000000)).safeDrawingPadding(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 560.dp).background(Color(0xFFF8F8F2)).border(1.dp, XpBorder).padding(14.dp),
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF173A67))
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { progress.percent / 100f }, modifier = Modifier.fillMaxWidth().height(9.dp), color = XpBlue, trackColor = Color(0xFFDDE8F5))
                Spacer(Modifier.height(7.dp))
                Text("${progress.percent}% • ${progress.completedEntries}/${progress.totalEntries} itens", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    "${archiveFormatBytes(progress.extractedBytes)} / ${archiveFormatBytes(progress.totalBytes)} • ${archiveFormatBytes(progress.speedBytesPerSecond)}/s",
                    fontSize = 11.sp,
                    color = XpTextSecondary,
                )
                Text(progress.currentEntry, fontSize = 10.sp, color = XpTextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (cancellable) {
                    Spacer(Modifier.height(10.dp))
                    XpDialogButton("Cancelar extração", onClick = onCancel, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun ArchiveFolderPickerDialog(initial: File, onDismiss: () -> Unit, onChoose: (File) -> Unit) {
    var current by remember(initial.absolutePath) { mutableStateOf(initial.takeIf { it.exists() && it.isDirectory } ?: Environment.getExternalStorageDirectory()) }
    val folders = remember(current.absolutePath, current.lastModified()) {
        runCatching { current.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.sortedBy { it.name.lowercase(Locale.getDefault()) }.orEmpty() }.getOrDefault(emptyList())
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0x99000000)).safeDrawingPadding(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = 620.dp).background(Color(0xFFF8F8F2)).border(1.dp, XpBorder).padding(12.dp),
            ) {
                Text("Escolher pasta de destino", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF173A67))
                Spacer(Modifier.height(7.dp))
                Text(current.absolutePath, fontSize = 10.5.sp, color = XpTextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    XpDialogButton("Armazenamento", onClick = { current = Environment.getExternalStorageDirectory() }, modifier = Modifier.weight(1f))
                    XpDialogButton("Subir", enabled = current.parentFile != null, onClick = { current.parentFile?.let { current = it } }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(7.dp))
                LazyColumn(Modifier.weight(1f).fillMaxWidth().background(Color.White).border(1.dp, XpControlBorder)) {
                    items(folders, key = { it.absolutePath }) { folder ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { current = folder }.padding(horizontal = 8.dp, vertical = 8.dp),
                        ) {
                            androidx.compose.foundation.Image(painterResource(R.drawable.folder), null, Modifier.size(25.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(folder.name, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text("›", fontSize = 18.sp, color = XpBlue)
                        }
                        HorizontalDivider(color = Color(0xFFE0E6EE))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    XpDialogButton("Cancelar", onClick = onDismiss, modifier = Modifier.weight(1f))
                    XpDialogButton("Usar esta pasta", onClick = { onChoose(current) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ArchivePasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    XpDialogFrame(title = "ZIP protegido por senha", onDismiss = onDismiss) {
        Text("Digite a senha para visualizar, verificar ou extrair o conteúdo protegido.", fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(36.dp).background(Color.White).border(1.dp, XpControlBorder).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF202020)),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            XpDialogButton("Cancelar", onClick = onDismiss, modifier = Modifier.weight(1f))
            XpDialogButton("Continuar", enabled = value.isNotEmpty(), onClick = { onConfirm(value) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ArchiveInfoDialog(file: File, info: ZipArchiveInfo, onDismiss: () -> Unit) {
    XpDialogFrame(title = "Informações do ZIP", onDismiss = onDismiss) {
        ArchiveInfoLine("Arquivo", file.name)
        ArchiveInfoLine("Tamanho", archiveFormatBytes(file.length()))
        ArchiveInfoLine("Descompactado", archiveFormatBytes(info.uncompressedBytes))
        ArchiveInfoLine("Arquivos", info.fileCount.toString())
        ArchiveInfoLine("Pastas", info.directoryCount.toString())
        ArchiveInfoLine("Criptografado", if (info.encrypted) "Sim" else "Não")
        ArchiveInfoLine("ZIP dividido", if (info.splitArchive) "Sim" else "Não")
        ArchiveInfoLine("Cabeçalhos", if (info.validHeaders) "Válidos" else "Inválidos")
        info.compressionRatio?.let { ArchiveInfoLine("Taxa de compressão", "$it%") }
        Spacer(Modifier.height(9.dp))
        XpDialogButton("Fechar", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ArchiveEntryDetailsDialog(item: ArchiveBrowserItem, onDismiss: () -> Unit) {
    XpDialogFrame(title = "Informações do item", onDismiss = onDismiss) {
        ArchiveInfoLine("Nome", item.name)
        ArchiveInfoLine("Caminho", item.path)
        ArchiveInfoLine("Tipo", if (item.directory) "Pasta" else FileTypeClassifier.labelFor(File(item.name), false))
        if (!item.directory) {
            ArchiveInfoLine("Tamanho", archiveFormatBytes(item.size))
            ArchiveInfoLine("Compactado", archiveFormatBytes(item.compressedSize))
            item.source?.let {
                ArchiveInfoLine("Método", it.compressionMethod)
                ArchiveInfoLine("Criptografado", if (it.encrypted) "Sim" else "Não")
            }
        }
        if (item.modifiedAt > 0L) ArchiveInfoLine("Modificado", DateFormat.getDateTimeInstance().format(Date(item.modifiedAt)))
        Spacer(Modifier.height(9.dp))
        XpDialogButton("Fechar", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ArchiveInfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text("$label:", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(112.dp))
        Text(value, fontSize = 11.sp, color = Color(0xFF303030), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ArchiveToggleRow(label: String, checked: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 5.dp)) {
        Box(
            modifier = Modifier.size(18.dp).background(if (checked) XpBlue else Color.White).border(1.dp, XpControlBorder),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(7.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun ArchiveChoiceButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.background(if (selected) Color(0xFFDCEBFC) else XpControlBackground).border(1.dp, if (selected) XpBlue else XpControlBorder).clickable(onClick = onClick).padding(horizontal = 5.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 10.5.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
    }
}

@Composable
private fun ArchiveSmallButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier.background(if (enabled) XpControlBackground else Color(0xFFF0F0F0)).border(1.dp, XpControlBorder).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 10.5.sp, color = if (enabled) Color(0xFF202020) else Color(0xFF999999), fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun ArchiveMenuText(label: String, selected: Boolean = false, onClick: () -> Unit) {
    Row(modifier = Modifier.widthIn(min = 170.dp).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(if (selected) "✓" else "", color = XpBlue, modifier = Modifier.width(18.dp), fontSize = 11.sp)
        Text(label, fontSize = 11.5.sp)
    }
}

@Composable
private fun LoadingArchivePanel(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(color = XpBlue)
        Spacer(Modifier.height(8.dp))
        Text(text, fontSize = 11.sp, color = XpTextSecondary)
    }
}

private fun togglePath(current: Set<String>, path: String): Set<String> = current.toMutableSet().apply {
    if (!add(path)) remove(path)
}

private fun buildBrowserItems(
    entries: List<ArchiveEntryInfo>,
    currentPath: String,
    query: String,
    sortMode: ArchiveSortMode,
    ascending: Boolean,
): List<ArchiveBrowserItem> {
    val normalizedCurrent = currentPath.trim('/')
    val prefix = if (normalizedCurrent.isBlank()) "" else "$normalizedCurrent/"
    val folderMap = linkedMapOf<String, ArchiveBrowserItem>()

    entries.forEach { entry ->
        val path = entry.path.trim('/')
        if (query.isNotBlank()) {
            if (!path.contains(query, ignoreCase = true)) return@forEach
            val name = path.substringAfterLast('/')
            folderMap[path] = ArchiveBrowserItem(path, name, entry.isDirectory, entry.size, entry.compressedSize, entry.modifiedAt, entry)
            return@forEach
        }
        if (!path.startsWith(prefix) || path == normalizedCurrent) return@forEach
        val remainder = path.removePrefix(prefix)
        if (remainder.isBlank()) return@forEach
        val first = remainder.substringBefore('/')
        val childPath = if (prefix.isBlank()) first else "$normalizedCurrent/$first"
        val hasNested = remainder.contains('/')
        if (hasNested) {
            val existing = folderMap[childPath]
            if (existing == null) {
                folderMap[childPath] = ArchiveBrowserItem(childPath, first, true, 0L, 0L, 0L, null)
            }
        } else {
            folderMap[childPath] = ArchiveBrowserItem(childPath, first, entry.isDirectory, entry.size, entry.compressedSize, entry.modifiedAt, entry)
        }
    }

    val comparator = when (sortMode) {
        ArchiveSortMode.NAME -> compareBy<ArchiveBrowserItem> { !it.directory }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        ArchiveSortMode.SIZE -> compareBy<ArchiveBrowserItem> { !it.directory }.thenBy { it.size }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        ArchiveSortMode.TYPE -> compareBy<ArchiveBrowserItem> { !it.directory }.thenBy { File(it.name).extension.lowercase() }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        ArchiveSortMode.DATE -> compareBy<ArchiveBrowserItem> { !it.directory }.thenBy { it.modifiedAt }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    }
    val sorted = folderMap.values.sortedWith(comparator)
    return if (ascending) sorted else sorted.reversed()
}

@Composable
internal fun ArchiveExtractionLauncher(
    zipFile: File,
    initialQuick: Boolean,
    onDismiss: () -> Unit,
    onOpenFolder: (File) -> Unit,
    onOpenFile: (File) -> Unit,
) {
    var infoResult by remember(zipFile.absolutePath, zipFile.lastModified()) { mutableStateOf<Result<ZipArchiveInfo>?>(null) }
    LaunchedEffect(zipFile.absolutePath, zipFile.lastModified()) {
        infoResult = ArchiveManager.readZip(zipFile)
    }
    when (val result = infoResult) {
        null -> XpDialogFrame(title = "Preparando ZIP", onDismiss = onDismiss) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = XpBlue, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(9.dp))
                Text("Lendo conteúdo e calculando o espaço necessário...", fontSize = 12.sp)
            }
        }
        else -> result.fold(
            onSuccess = { info ->
                ArchiveExtractionFlow(
                    zipFile = zipFile,
                    archiveInfo = info,
                    initialQuick = initialQuick,
                    onDismiss = onDismiss,
                    onOpenFolder = onOpenFolder,
                    onOpenFile = onOpenFile,
                )
            },
            onFailure = { error ->
                XpDialogFrame(title = "Não foi possível abrir o ZIP", onDismiss = onDismiss) {
                    Text(error.message ?: "Arquivo compactado inválido ou danificado.", fontSize = 12.sp, color = Color(0xFF8B3028))
                    Spacer(Modifier.height(10.dp))
                    XpDialogButton("Fechar", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                }
            },
        )
    }
}
