package com.exploradorxp.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.core.content.FileProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

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
    var previewProgress by remember { mutableStateOf<ArchiveProgress?>(null) }
    var previewJob by remember { mutableStateOf<Job?>(null) }
    var previewRequestId by remember { mutableStateOf(0L) }

    LaunchedEffect(file.absolutePath, file.lastModified()) {
        infoResult = ArchiveManager.readZip(file)
    }

    val archiveInfo = infoResult?.getOrNull()
    val browserIndex = remember(archiveInfo) {
        archiveInfo?.let { buildArchiveBrowserIndex(it.entries) }
    }
    val visibleItems = remember(browserIndex, currentPath, query, sortMode, ascending) {
        browserIndex?.let { buildArchiveBrowserItems(it, currentPath, query, sortMode, ascending) }.orEmpty()
    }
    val selectedBytes = remember(archiveInfo, selected) {
        archiveInfo?.let { ArchiveManager.requiredBytes(it, selected.takeIf { paths -> paths.isNotEmpty() }) } ?: 0L
    }
    val archiveType = remember(file.absolutePath, file.length(), file.lastModified()) { archiveTypeSummary(file) }

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
        previewJob?.cancel()
        val requestId = previewRequestId + 1L
        previewRequestId = requestId
        previewProgress = ArchiveProgress(0, 0, 1, 0L, 0L, 0L, path)
        previewJob = scope.launch {
            val result = ArchiveManager.extractEntryToCache(
                zipFile = file,
                entryPath = path,
                cacheRoot = context.cacheDir,
                password = password,
                onProgress = { progress ->
                    if (previewRequestId == requestId) previewProgress = progress
                },
            )
            if (previewRequestId != requestId) return@launch
            previewProgress = null
            result.onSuccess { extracted ->
                if (supportsInternalViewer(extracted)) onPreviewFile(extracted) else onOpenExternal(extracted)
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) return@onFailure
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF3F7FC))
                .border(1.dp, XpChromeBorder)
                .padding(horizontal = 9.dp, vertical = 9.dp),
        ) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                ArchiveHeaderIconButton(
                    icon = R.drawable.up,
                    label = "Subir",
                    enabled = currentPath.isNotBlank(),
                ) {
                    currentPath = currentPath.substringBeforeLast('/', "")
                    selected = emptySet()
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        file.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF173A67),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    ) {
                        ArchiveBadge(archiveType.typeLabel)
                        if (archiveType.detectedByContent) ArchiveBadge("Detectado pelo conteúdo", emphasized = true)
                    }
                }
            }
            if (archiveInfo != null) {
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    ArchiveMetric("Tamanho", archiveFormatBytes(file.length()), Modifier.weight(1f))
                    ArchiveMetric("Arquivos", (browserIndex?.totalFiles ?: archiveInfo.fileCount).toString(), Modifier.weight(1f))
                    ArchiveMetric("Pastas", (browserIndex?.totalDirectories ?: archiveInfo.directoryCount).toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    ArchiveMetric("Compactado", archiveFormatBytes(archiveInfo.compressedBytes), Modifier.weight(1f))
                    ArchiveMetric("Descompactado", archiveFormatBytes(archiveInfo.uncompressedBytes), Modifier.weight(1f))
                    ArchiveMetric("Compressão", archiveInfo.compressionRatio?.let { "$it%" } ?: "N/D", Modifier.weight(1f))
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text("Lendo conteúdo do arquivo compactado…", fontSize = 10.5.sp, color = XpTextSecondary)
            }
            Spacer(Modifier.height(9.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ArchivePrimaryActionButton(
                    if (selected.isEmpty()) "Extrair" else "Extrair ${selected.size}",
                    R.drawable.folder_open,
                    modifier = Modifier.weight(1f),
                    enabled = archiveInfo != null,
                ) { showExtraction = true }
                ArchivePrimaryActionButton(
                    "Abrir com",
                    R.drawable.folder_open,
                    modifier = Modifier.weight(1f),
                    enabled = true,
                ) { onOpenExternal(file) }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                ArchiveSecondaryActionButton("Verificar", R.drawable.check, Modifier.weight(1f), archiveInfo != null) { verifyArchive() }
                ArchiveSecondaryActionButton("Info", R.drawable.info, Modifier.weight(1f), archiveInfo != null) { showArchiveDetails = true }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(38.dp).background(Color(0xFFF8FAFD)).border(1.dp, XpControlBorder).padding(horizontal = 7.dp),
        ) {
            Box(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                contentAlignment = Alignment.CenterStart,
            ) {
                ArchiveBreadcrumb(currentPath = currentPath) { target ->
                    currentPath = target
                    query = ""
                    selected = emptySet()
                }
            }
            Box(modifier = Modifier.clickable(enabled = archiveInfo != null) { showSortMenu = true }.padding(vertical = 5.dp)) {
                Text(
                    "Classificação: ${when (sortMode) {
                        ArchiveSortMode.NAME -> "Nome"
                        ArchiveSortMode.SIZE -> "Tamanho"
                        ArchiveSortMode.TYPE -> "Tipo"
                        ArchiveSortMode.DATE -> "Data"
                    }} • ${if (ascending) "↑" else "↓"}",
                    fontSize = 10.5.sp,
                    color = XpTextSecondary,
                )
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

        if (visibleItems.isNotEmpty()) {
            val visiblePaths = visibleItems.mapTo(linkedSetOf()) { it.path }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().background(Color(0xFFEAF2FB)).padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                Text(
                    if (selected.isEmpty()) {
                        if (visibleItems.size == 1) "1 item visível" else "${visibleItems.size} itens visíveis"
                    } else {
                        val selectionLabel = if (selected.size == 1) "1 selecionado" else "${selected.size} selecionados"
                        "$selectionLabel • ${archiveFormatBytes(selectedBytes)}"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.widthIn(max = 210.dp).horizontalScroll(rememberScrollState()),
                ) {
                    if (selected.isEmpty()) {
                        ArchiveSmallButton("Selecionar tudo") { selected = visiblePaths }
                    } else {
                        ArchiveSmallButton("Tudo") { selected = visiblePaths }
                        ArchiveSmallButton("Inverter") { selected = visiblePaths.filterNotTo(linkedSetOf()) { it in selected } }
                        ArchiveSmallButton("Limpar") { selected = emptySet() }
                    }
                }
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
                                    item.directory -> {
                                        val folders = if (item.descendantDirectories > 0) " • ${item.descendantDirectories} pastas" else ""
                                        "${item.descendantFiles} arquivos$folders • ${archiveFormatBytes(item.size)}"
                                    }
                                    item.source?.encrypted == true -> "${item.source.compressionMethod} • ${archiveFormatBytes(item.compressedSize)} compactado • senha"
                                    else -> "${item.source?.compressionMethod ?: "Arquivo"} • ${archiveFormatBytes(item.compressedSize)} compactado"
                                }
                                Text(detail, fontSize = 10.sp, color = XpTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (query.isNotBlank()) {
                                    val parent = item.path.substringBeforeLast('/', "ZIP:/")
                                    Text(
                                        if (parent == "ZIP:/") parent else "ZIP:/$parent",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF667788),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Text(
                                archiveFormatBytes(item.size),
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

        val folderInfo = browserIndex?.itemsByPath?.get(currentPath.trim('/'))
        val statusText = when {
            query.isNotBlank() -> if (visibleItems.size == 1) "1 resultado • pesquisa em todo o ZIP" else "${visibleItems.size} resultados • pesquisa em todo o ZIP"
            folderInfo != null ->
                "${folderInfo.descendantFiles} arquivos • ${folderInfo.descendantDirectories} pastas • ${archiveFormatBytes(folderInfo.size)}"
            else -> ""
        }
        if (statusText.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(28.dp).background(Color(0xFFEAF2FB)).border(1.dp, Color(0xFFC9D8E8)).padding(horizontal = 9.dp),
            ) {
                Text(statusText, fontSize = 10.5.sp, color = XpTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        ArchiveInfoDialog(
            file = file,
            info = archiveInfo,
            index = browserIndex,
            archiveType = archiveType,
            onDismiss = { showArchiveDetails = false },
            onOpenFolder = {
                showArchiveDetails = false
                file.parentFile?.let(onOpenFolder)
            },
            onExtract = {
                showArchiveDetails = false
                showExtraction = true
            },
        )
    }

    verifyProgress?.let { progress ->
        ArchiveProgressDialog(
            title = "Verificando integridade",
            progress = progress,
            cancellable = false,
            onCancel = {},
        )
    }

    previewProgress?.let { progress ->
        ArchiveProgressDialog(
            title = "Preparando ${File(progress.currentEntry).name.ifBlank { "arquivo" }}",
            progress = progress,
            cancellable = true,
            cancelLabel = "Cancelar abertura",
            onCancel = {
                previewRequestId += 1L
                previewJob?.cancel()
                previewProgress = null
            },
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
internal fun ArchiveCreationFlow(
    sources: List<File>,
    initialDestination: File,
    onDismiss: () -> Unit,
    onOpenFolder: (File) -> Unit,
    onOpenArchive: (File) -> Unit,
    onCreated: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("archive_creation", android.content.Context.MODE_PRIVATE)
    }
    val defaultName = remember(sources) {
        when {
            sources.size == 1 -> {
                val source = sources.first()
                val base = if (source.isDirectory) source.name else source.nameWithoutExtension
                "${base.ifBlank { "Arquivo" }}.zip"
            }
            else -> "Arquivos.zip"
        }
    }
    var archiveName by remember(sources) { mutableStateOf(defaultName) }
    var destination by remember(initialDestination.absolutePath, sources) {
        val remembered = prefs.getString("last_destination", null)
            ?.let(::File)
            ?.takeIf { it.exists() && it.isDirectory }
        val commonParent = sources.mapNotNull { it.parentFile?.absoluteFile }.distinctBy { it.absolutePath }.singleOrNull()
        mutableStateOf(remembered ?: commonParent ?: initialDestination)
    }
    var showPicker by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf("config") }
    var progress by remember { mutableStateOf(ArchiveProgress(0, 0, 0, 0, 0, 0, "Preparando…")) }
    var summary by remember { mutableStateOf<ArchiveCreationSummary?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }

    fun startCompression() {
        if (archiveName.isBlank()) {
            errorText = "Informe um nome para o arquivo ZIP."
            return
        }
        prefs.edit().putString("last_destination", destination.absolutePath).apply()
        errorText = null
        stage = "progress"
        job?.cancel()
        job = scope.launch {
            val result = ArchiveManager.createZip(
                sources = sources,
                destinationParent = destination,
                archiveName = archiveName,
                onProgress = { progress = it },
            )
            result.onSuccess {
                summary = it
                stage = "done"
                onCreated()
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) {
                    stage = "config"
                    errorText = "Compactação cancelada."
                } else {
                    stage = "config"
                    errorText = error.message ?: "Não foi possível criar o arquivo ZIP."
                }
            }
        }
    }

    when (stage) {
        "config" -> XpDialogFrame(title = "Compactar em ZIP", onDismiss = onDismiss) {
            Text(
                if (sources.size == 1) "1 item selecionado" else "${sources.size} itens selecionados",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF173A67),
            )
            Spacer(Modifier.height(8.dp))
            Text("Nome do arquivo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = XpTextSecondary)
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(Color.White)
                    .border(1.dp, XpControlBorder)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = archiveName,
                    onValueChange = { archiveName = it; errorText = null },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF202020)),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(9.dp))
            Text("Destino", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = XpTextSecondary)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    destination.absolutePath,
                    fontSize = 10.5.sp,
                    color = XpTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(7.dp))
                XpDialogButton("Alterar", onClick = { showPicker = true })
            }
            Spacer(Modifier.height(7.dp))
            Text(
                "Pastas são incluídas com toda a estrutura interna. Se já existir um ZIP com esse nome, será criado um nome livre automaticamente.",
                fontSize = 10.5.sp,
                color = XpTextSecondary,
            )
            if (errorText != null) {
                Spacer(Modifier.height(7.dp))
                Text(errorText.orEmpty(), fontSize = 11.sp, color = Color(0xFF9C1B12))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                XpDialogButton("Cancelar", onClick = onDismiss, modifier = Modifier.weight(1f))
                XpDialogButton("Compactar", enabled = archiveName.isNotBlank(), onClick = { startCompression() }, modifier = Modifier.weight(1f))
            }
        }
        "progress" -> ArchiveProgressDialog(
            title = "Compactando arquivos",
            progress = progress,
            cancellable = true,
            cancelLabel = "Cancelar compactação",
            onCancel = {
                job?.cancel()
                stage = "config"
                errorText = "Compactação cancelada."
            },
        )
        "done" -> XpDialogFrame(title = "Compactação concluída", onDismiss = onDismiss) {
            val result = summary
            if (result != null) {
                Text(
                    "${result.selectedItems} selecionados • ${result.archivedEntries} itens no ZIP",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(result.archive.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF173A67))
                Text(result.archive.parentFile?.absolutePath.orEmpty(), fontSize = 10.5.sp, color = XpTextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Original: ${archiveFormatBytes(result.inputBytes)} • ZIP: ${archiveFormatBytes(result.outputBytes)}",
                    fontSize = 11.sp,
                    color = XpTextSecondary,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    XpDialogButton("Fechar", onClick = onDismiss, modifier = Modifier.weight(1f))
                    XpDialogButton("Abrir ZIP", onClick = { onOpenArchive(result.archive) }, modifier = Modifier.weight(1f))
                    XpDialogButton("Abrir pasta", onClick = { result.archive.parentFile?.let(onOpenFolder) }, modifier = Modifier.weight(1f))
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
                prefs.edit().putString("last_destination", it.absolutePath).apply()
                showPicker = false
            },
        )
    }
}

@Composable
private fun ArchiveProgressDialog(
    title: String,
    progress: ArchiveProgress,
    cancellable: Boolean,
    cancelLabel: String = "Cancelar extração",
    onCancel: () -> Unit,
) {
    Dialog(onDismissRequest = { if (cancellable) onCancel() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0x99000000)).padding(12.dp), contentAlignment = Alignment.Center) {
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
                    XpDialogButton(cancelLabel, onClick = onCancel, modifier = Modifier.fillMaxWidth())
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
        Box(Modifier.fillMaxSize().background(Color(0x99000000)).padding(12.dp), contentAlignment = Alignment.Center) {
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
private fun ArchiveInfoDialog(
    file: File,
    info: ZipArchiveInfo,
    index: ArchiveBrowserIndex?,
    archiveType: ArchiveTypeSummary,
    onDismiss: () -> Unit,
    onOpenFolder: () -> Unit,
    onExtract: () -> Unit,
) {
    val context = LocalContext.current
    var pathCopied by remember(file.absolutePath) { mutableStateOf(false) }
    val fileCount = index?.totalFiles ?: info.fileCount
    val directoryCount = index?.totalDirectories ?: info.directoryCount
    val modified = file.lastModified().takeIf { it > 0L }
        ?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)) }
        ?: "Não disponível"

    XpDialogFrame(title = "Informações do arquivo compactado", onDismiss = onDismiss, maxWidth = 390) {
        Text(file.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF173A67), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(3.dp))
        Text(archiveType.typeLabel, fontSize = 10.5.sp, color = XpTextSecondary)

        Spacer(Modifier.height(12.dp))
        ArchiveInfoSection("Informações gerais") {
            ArchiveInfoLine("Nome", file.name)
            ArchiveInfoLine("Caminho", file.absolutePath)
            ArchiveInfoLine("Tipo", archiveType.typeLabel)
            ArchiveInfoLine("Tamanho", archiveFormatBytes(file.length()))
            ArchiveInfoLine("Compactado", archiveFormatBytes(info.compressedBytes))
            ArchiveInfoLine("Descompactado", archiveFormatBytes(info.uncompressedBytes))
        }

        Spacer(Modifier.height(9.dp))
        ArchiveInfoSection("Conteúdo") {
            ArchiveInfoLine("Arquivos", fileCount.toString())
            ArchiveInfoLine("Pastas", directoryCount.toString())
            ArchiveInfoLine("Método", archiveCompressionMethod(info))
            ArchiveInfoLine("Taxa de compressão", info.compressionRatio?.let { "$it%" } ?: "Não disponível")
            ArchiveInfoLine("Criptografado", if (info.encrypted) "Sim" else "Não")
        }

        Spacer(Modifier.height(9.dp))
        ArchiveInfoSection("Origem") {
            ArchiveInfoLine("Identificação", archiveType.originLabel)
            ArchiveInfoLine("Última modificação", modified)
            ArchiveInfoLine("ZIP dividido", if (info.splitArchive) "Sim" else "Não")
            ArchiveInfoLine("Estrutura", if (info.validHeaders) "Cabeçalhos válidos" else "Cabeçalhos inválidos")
        }

        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            XpDialogButton(
                label = if (pathCopied) "Copiado" else "Copiar caminho",
                iconRes = if (pathCopied) R.drawable.check else R.drawable.copy,
                modifier = Modifier.weight(1f),
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Caminho do arquivo", file.absolutePath))
                    pathCopied = true
                },
            )
            XpDialogButton(
                label = "Compartilhar",
                iconRes = R.drawable.share,
                modifier = Modifier.weight(1f),
                onClick = { shareArchiveFile(context, file) },
            )
        }
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            XpDialogButton("Abrir pasta", iconRes = R.drawable.folder_open, onClick = onOpenFolder, modifier = Modifier.weight(1f))
            XpDialogButton("Extrair", iconRes = R.drawable.folder_open, onClick = onExtract, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(7.dp))
        XpDialogButton("Fechar", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ArchiveInfoSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFFD6E0EC), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF173A67))
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun ArchiveEntryDetailsDialog(item: ArchiveBrowserItem, onDismiss: () -> Unit) {
    XpDialogFrame(title = "Informações do item", onDismiss = onDismiss) {
        ArchiveInfoLine("Nome", item.name)
        ArchiveInfoLine("Caminho", item.path)
        ArchiveInfoLine("Tipo", if (item.directory) "Pasta" else FileTypeClassifier.labelFor(File(item.name), false))
        if (item.directory) {
            ArchiveInfoLine("Conteúdo", "${item.descendantFiles} arquivos • ${item.descendantDirectories} pastas")
            ArchiveInfoLine("Tamanho total", archiveFormatBytes(item.size))
            ArchiveInfoLine("Compactado", archiveFormatBytes(item.compressedSize))
        } else {
            ArchiveInfoLine("Tamanho", archiveFormatBytes(item.size))
            ArchiveInfoLine("Compactado", archiveFormatBytes(item.compressedSize))
            val ratio = if (item.size > 0L) ((1.0 - item.compressedSize.toDouble() / item.size.toDouble()) * 100.0).toInt().coerceIn(-999, 100) else null
            ratio?.let { ArchiveInfoLine("Compressão", "$it%") }
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
private fun ArchiveHeaderIconButton(
    icon: Int,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(if (enabled) Color.White else Color(0xFFF0F0F0))
            .border(1.dp, XpControlBorder)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CachedResourceIcon(icon, label, modifier = Modifier.size(18.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                fontSize = 9.sp,
                color = if (enabled) Color(0xFF183363) else Color(0xFF9A9A9A),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ArchiveBadge(label: String, emphasized: Boolean = false) {
    Box(
        modifier = Modifier
            .background(if (emphasized) Color(0xFFDCEBFC) else Color.White, RoundedCornerShape(4.dp))
            .border(1.dp, if (emphasized) XpBlue else Color(0xFFC7D3E0), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            label,
            fontSize = 9.5.sp,
            color = if (emphasized) Color(0xFF174D88) else XpTextSecondary,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun ArchiveMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(5.dp))
            .border(1.dp, Color(0xFFD7E1EC), RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 6.dp),
    ) {
        Text(label, fontSize = 9.sp, color = XpTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, fontSize = 10.5.sp, color = Color(0xFF20344F), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ArchivePrimaryActionButton(
    label: String,
    icon: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(38.dp)
            .background(if (enabled) Color(0xFFDCEBFC) else Color(0xFFF0F0F0), RoundedCornerShape(5.dp))
            .border(1.dp, if (enabled) XpBlue else Color(0xFFD1D6DC), RoundedCornerShape(5.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 9.dp),
    ) {
        CachedResourceIcon(icon, label, modifier = Modifier.size(17.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = if (enabled) Color(0xFF173A67) else Color(0xFF999999),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ArchiveSecondaryActionButton(
    label: String,
    icon: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(33.dp)
            .background(if (enabled) Color.White else Color(0xFFF0F0F0), RoundedCornerShape(5.dp))
            .border(1.dp, XpControlBorder, RoundedCornerShape(5.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp),
    ) {
        CachedResourceIcon(icon, label, modifier = Modifier.size(14.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            fontSize = 10.5.sp,
            color = if (enabled) Color(0xFF303030) else Color(0xFF999999),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
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

private fun shareArchiveFile(context: Context, file: File) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar arquivo compactado"))
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

@Composable
private fun ArchiveBreadcrumb(currentPath: String, onNavigate: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "ZIP:/",
            fontSize = 11.sp,
            color = XpBlue,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onNavigate("") }.padding(vertical = 5.dp),
        )
        val parts = normalizeEntryPath(currentPath).split('/').filter { it.isNotBlank() }
        parts.forEachIndexed { index, part ->
            Text("/", fontSize = 11.sp, color = XpTextSecondary, modifier = Modifier.padding(horizontal = 2.dp))
            val target = parts.take(index + 1).joinToString("/")
            Text(
                part,
                fontSize = 11.sp,
                color = if (index == parts.lastIndex) Color(0xFF303030) else XpBlue,
                fontWeight = if (index == parts.lastIndex) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.clickable { onNavigate(target) }.padding(vertical = 5.dp),
                maxLines = 1,
            )
        }
    }
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
            Spacer(Modifier.height(12.dp))
            XpDialogButton(
                "Cancelar",
                iconRes = R.drawable.close,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
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
