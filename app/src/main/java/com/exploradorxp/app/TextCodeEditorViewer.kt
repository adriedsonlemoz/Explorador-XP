package com.exploradorxp.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import android.view.Gravity
import android.view.KeyEvent
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlin.math.max

private const val EDITOR_EDIT_MAX_BYTES = 750_000L
private const val EDITOR_PREVIEW_MAX_BYTES = 400_000
private const val SYNTAX_HIGHLIGHT_MAX_CHARS = 140_000
private const val HISTORY_LIMIT = 250
private const val HISTORY_CHAR_BUDGET = 4_000_000
private const val EDITOR_PREFERENCES = "text_code_editor"
private const val PREF_USE_TABS = "use_tabs"
private const val PREF_INDENT_SIZE = "indent_size"
private const val PREF_WORD_WRAP = "word_wrap"

private val webExtensions = setOf("html", "htm", "css", "js", "mjs", "cjs")
private val syntaxExtensions = setOf(
    "html", "htm", "xml", "css", "js", "mjs", "cjs", "ts", "tsx", "jsx", "json",
    "md", "markdown", "mds", "yml", "yaml", "toml", "ini", "cfg", "conf", "properties",
    "kt", "kts", "java", "gradle", "py", "sh", "bash", "php", "rb", "go", "rs", "swift",
    "dart", "c", "h", "hpp", "cpp", "cc", "sql", "vue", "svelte", "bat", "cmd", "ps1",
    "env", "tex", "csv"
)

private sealed interface EditorLoadState {
    data object Loading : EditorLoadState
    data class Ready(val document: EditorDocument) : EditorLoadState
    data class Error(val message: String, val likelyBinary: Boolean = false) : EditorLoadState
}

private data class EditorDocument(
    val text: String,
    val encoding: EditorEncoding,
    val truncated: Boolean,
    val lineEnding: String,
)

private data class EditorEncoding(
    val charset: Charset,
    val label: String,
    val bom: ByteArray = byteArrayOf(),
)

private data class EditorHistoryState(
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val dirty: Boolean = false,
)

private data class EditorMetrics(
    val line: Int = 1,
    val column: Int = 1,
    val lineCount: Int = 1,
)

private data class EditorSearchState(
    val total: Int = 0,
    val current: Int = 0,
)

private data class EditorSearchResult(
    val message: String,
    val state: EditorSearchState,
)

private data class EditorPreferences(
    val useTabs: Boolean = false,
    val indentSize: Int = 4,
    val wordWrap: Boolean = false,
) {
    val indentUnit: String
        get() = if (useTabs) "\t" else " ".repeat(indentSize.coerceIn(2, 8))
}

private enum class EditorViewMode { CODE, PREVIEW }

@Composable
fun TextCodeEditorViewer(
    file: File,
    forcedReadOnly: Boolean = false,
    externalOrigin: ExternalOpenOrigin? = null,
    fullScreen: Boolean,
    onFullScreenChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onCloseHandlerChanged: ((() -> Unit)?) -> Unit,
    onOpenExternal: () -> Unit,
    onFileChanged: (File) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val extension = file.extension.lowercase()
    val syntaxExtension = remember(file.name) { editorSyntaxExtension(file) }
    val supportsPreview = extension in webExtensions
    val key = "${file.absolutePath}:${file.lastModified()}:${file.length()}"
    val isArchiveCachePreview = remember(file.absolutePath) {
        file.absolutePath.startsWith(File(context.cacheDir, "archive-preview").absolutePath)
    }
    val isArchivePreview = forcedReadOnly || isArchiveCachePreview

    var loadState by remember(key) { mutableStateOf<EditorLoadState>(EditorLoadState.Loading) }
    var editorView by remember(file.absolutePath) { mutableStateOf<CodeEditText?>(null) }
    var workingText by remember(file.absolutePath) { mutableStateOf("") }
    var dirty by remember(file.absolutePath) { mutableStateOf(false) }
    var metrics by remember(file.absolutePath) { mutableStateOf(EditorMetrics()) }
    var historyState by remember(file.absolutePath) { mutableStateOf(EditorHistoryState()) }
    var statusMessage by remember(file.absolutePath) { mutableStateOf("") }
    var viewMode by remember(file.absolutePath) { mutableStateOf(EditorViewMode.CODE) }
    var showFindPanel by remember(file.absolutePath) { mutableStateOf(false) }
    var showReplaceField by remember(file.absolutePath) { mutableStateOf(false) }
    var findQuery by remember(file.absolutePath) { mutableStateOf("") }
    var replaceText by remember(file.absolutePath) { mutableStateOf("") }
    var showGoToLine by remember(file.absolutePath) { mutableStateOf(false) }
    var showSaveAs by remember(file.absolutePath) { mutableStateOf(false) }
    var pendingSaveAsTarget by remember(file.absolutePath) { mutableStateOf<File?>(null) }
    var showOverwriteConfirm by remember(file.absolutePath) { mutableStateOf(false) }
    var showCloseConfirm by remember(file.absolutePath) { mutableStateOf(false) }
    var showMore by remember(file.absolutePath) { mutableStateOf(false) }
    var showEditorSettings by remember(file.absolutePath) { mutableStateOf(false) }
    var searchState by remember(file.absolutePath) { mutableStateOf(EditorSearchState()) }
    var editorPreferences by remember { mutableStateOf(loadEditorPreferences(context)) }
    var previewRevision by remember(file.absolutePath) { mutableIntStateOf(0) }
    var previewSource by remember(file.absolutePath) { mutableStateOf("") }
    var previewError by remember(file.absolutePath) { mutableStateOf("") }

    LaunchedEffect(key) {
        loadState = EditorLoadState.Loading
        loadState = withContext(Dispatchers.IO) { loadEditorDocument(file) }
        val ready = loadState as? EditorLoadState.Ready
        workingText = ready?.document?.text.orEmpty()
        previewSource = workingText
        dirty = false
        metrics = metricsForText(workingText, 0)
        historyState = EditorHistoryState()
        searchState = EditorSearchState()
        statusMessage = ""
        editorView = null
        viewMode = EditorViewMode.CODE
        onFullScreenChange(false)
    }

    val requestCloseState = rememberUpdatedState<() -> Unit> {
        if (dirty) showCloseConfirm = true else onClose()
    }
    DisposableEffect(file.absolutePath) {
        val handler = { requestCloseState.value.invoke() }
        onCloseHandlerChanged(handler)
        onDispose { onCloseHandlerChanged(null) }
    }

    fun refreshPreview() {
        val currentSource = editorView?.text?.toString() ?: workingText
        workingText = currentSource
        previewSource = currentSource
        previewRevision++
        previewError = ""
    }

    fun saveTo(target: File, switchToTarget: Boolean) {
        val ready = loadState as? EditorLoadState.Ready ?: return
        if (ready.document.truncated) {
            statusMessage = "Este arquivo está em modo somente leitura porque é muito grande."
            return
        }
        val textToSave = editorView?.text?.toString() ?: workingText
        scope.launch {
            statusMessage = "Salvando com segurança..."
            val result = withContext(Dispatchers.IO) {
                safeWriteTextFile(
                    target = target,
                    text = textToSave,
                    encoding = ready.document.encoding,
                    preferredLineEnding = ready.document.lineEnding,
                )
            }
            result.fold(
                onSuccess = {
                    workingText = textToSave
                    dirty = false
                    editorView?.markSaved()
                    historyState = EditorHistoryState()
                    statusMessage = if (switchToTarget) "Salvo como ${target.name}" else "Arquivo salvo"
                    previewSource = textToSave
                    previewRevision++
                    if (switchToTarget && target.absolutePath != file.absolutePath) onFileChanged(target)
                },
                onFailure = { error ->
                    statusMessage = "Não foi possível salvar: ${error.message ?: "erro desconhecido"}"
                },
            )
        }
    }

    if (showCloseConfirm) {
        EditorChoiceDialog(
            title = "Alterações não salvas",
            message = "Há alterações que ainda não foram salvas em ${file.name}. O que deseja fazer?",
            primaryLabel = "Salvar e sair",
            secondaryLabel = "Sair sem salvar",
            cancelLabel = "Cancelar",
            destructiveSecondary = true,
            onPrimary = {
                showCloseConfirm = false
                val ready = loadState as? EditorLoadState.Ready
                if (ready != null && !ready.document.truncated) {
                    val textToSave = editorView?.text?.toString() ?: workingText
                    scope.launch {
                        statusMessage = "Salvando com segurança..."
                        val result = withContext(Dispatchers.IO) {
                            safeWriteTextFile(file, textToSave, ready.document.encoding, ready.document.lineEnding)
                        }
                        result.fold(
                            onSuccess = {
                                dirty = false
                                editorView?.markSaved()
                                onClose()
                            },
                            onFailure = { statusMessage = "Não foi possível salvar: ${it.message ?: "erro desconhecido"}" },
                        )
                    }
                }
            },
            onSecondary = {
                showCloseConfirm = false
                dirty = false
                onClose()
            },
            onCancel = { showCloseConfirm = false },
        )
    }

    if (showSaveAs) {
        SaveAsDialog(
            initialName = file.name,
            parentPath = file.parentFile?.absolutePath.orEmpty(),
            onDismiss = { showSaveAs = false },
            onConfirm = { name ->
                val target = File(file.parentFile ?: return@SaveAsDialog, name)
                if (target.exists() && target.absolutePath != file.absolutePath) {
                    pendingSaveAsTarget = target
                    showSaveAs = false
                    showOverwriteConfirm = true
                } else {
                    showSaveAs = false
                    saveTo(target, switchToTarget = true)
                }
            },
        )
    }

    if (showOverwriteConfirm) {
        val target = pendingSaveAsTarget
        EditorChoiceDialog(
            title = "Substituir arquivo?",
            message = target?.let { "${it.name} já existe nesta pasta. Deseja substituí-lo?" }.orEmpty(),
            primaryLabel = "Substituir",
            secondaryLabel = null,
            cancelLabel = "Cancelar",
            destructivePrimary = true,
            onPrimary = {
                showOverwriteConfirm = false
                if (target != null) saveTo(target, switchToTarget = true)
                pendingSaveAsTarget = null
            },
            onSecondary = {},
            onCancel = {
                showOverwriteConfirm = false
                pendingSaveAsTarget = null
            },
        )
    }

    if (showGoToLine) {
        GoToLineDialog(
            onDismiss = { showGoToLine = false },
            onGo = { line ->
                val view = editorView
                if (view != null) {
                    val actual = goToLine(view, line)
                    metrics = view.currentMetrics()
                    statusMessage = if (actual) "Posicionado na linha $line" else "Linha $line não existe neste arquivo"
                }
                showGoToLine = false
            },
        )
    }

    if (showMore) {
        EditorMoreDialog(
            editable = ((loadState as? EditorLoadState.Ready)?.document?.truncated == false) && !isArchivePreview,
            supportsPreview = supportsPreview,
            onDismiss = { showMore = false },
            onSelectAll = { editorView?.selectAll(); showMore = false },
            onCopy = { editorView?.onTextContextMenuItem(android.R.id.copy); showMore = false },
            onCut = { editorView?.onTextContextMenuItem(android.R.id.cut); showMore = false },
            onPaste = { editorView?.onTextContextMenuItem(android.R.id.paste); showMore = false },
            onGoToLine = { showMore = false; showGoToLine = true },
            onFindReplace = {
                showMore = false
                showFindPanel = true
                showReplaceField = true
                searchState = editorView?.searchState(findQuery) ?: EditorSearchState()
            },
            onSettings = { showMore = false; showEditorSettings = true },
            onPreview = {
                showMore = false
                if (supportsPreview) {
                    refreshPreview()
                    viewMode = EditorViewMode.PREVIEW
                }
            },
        )
    }


    if (showEditorSettings) {
        EditorSettingsDialog(
            preferences = editorPreferences,
            onDismiss = { showEditorSettings = false },
            onChange = { updated ->
                editorPreferences = updated
                saveEditorPreferences(context, updated)
                editorView?.updatePreferences(updated)
            },
        )
    }

    val showFullScreenPreview = fullScreen && viewMode == EditorViewMode.PREVIEW

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
        EditorToolbar(
            dirty = dirty,
            editable = ((loadState as? EditorLoadState.Ready)?.document?.truncated == false) && !isArchivePreview,
            canUndo = historyState.canUndo,
            canRedo = historyState.canRedo,
            supportsPreview = supportsPreview,
            viewMode = viewMode,
            onSave = { saveTo(file, switchToTarget = false) },
            onSaveAs = { showSaveAs = true },
            onUndo = { editorView?.undoEdit() },
            onRedo = { editorView?.redoEdit() },
            onFind = { showFindPanel = !showFindPanel },
            onTogglePreview = {
                if (viewMode == EditorViewMode.CODE) {
                    refreshPreview()
                    viewMode = EditorViewMode.PREVIEW
                } else {
                    viewMode = EditorViewMode.CODE
                    onFullScreenChange(false)
                }
            },
            onMore = { showMore = true },
        )

        val ready = loadState as? EditorLoadState.Ready
        val warning = when {
            statusMessage.isNotBlank() -> statusMessage
            isArchiveCachePreview -> "Pré-visualização temporária extraída do ZIP: aberto em modo somente leitura para evitar travamentos e alterações acidentais."
            forcedReadOnly && externalOrigin != null -> "Arquivo recebido por Abrir com: a origem foi preservada separadamente e esta cópia temporária continua somente leitura."
            forcedReadOnly -> "Arquivo aberto por outro aplicativo: modo somente leitura para preservar o documento original."
            ready?.document?.truncated == true -> "Arquivo grande: aberto parcialmente e somente para leitura para evitar travamentos."
            ready != null && file.length() > SYNTAX_HIGHLIGHT_MAX_CHARS && syntaxExtension in syntaxExtensions ->
                "Realce de sintaxe reduzido neste arquivo para manter o editor responsivo."
            else -> ""
        }
        if (warning.isNotBlank()) {
            Text(
                warning,
                fontSize = 11.sp,
                color = Color(0xFF5A4A16),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF6CF))
                    .border(1.dp, Color(0xFFE5D28A))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }

        if (showFindPanel) {
            FindReplacePanel(
                query = findQuery,
                replacement = replaceText,
                showReplace = showReplaceField,
                resultLabel = when {
                    findQuery.isBlank() -> ""
                    searchState.total == 0 -> "0 ocorrências"
                    searchState.current > 0 -> "${searchState.current}/${searchState.total}"
                    else -> "${searchState.total} ocorrências"
                },
                onQueryChange = { query ->
                    findQuery = query
                    searchState = editorView?.searchState(query) ?: EditorSearchState()
                },
                onReplacementChange = { replaceText = it },
                onToggleReplace = { showReplaceField = !showReplaceField },
                onPrevious = {
                    val result = findInEditor(editorView, findQuery, forward = false)
                    statusMessage = result.message
                    searchState = result.state
                },
                onNext = {
                    val result = findInEditor(editorView, findQuery, forward = true)
                    statusMessage = result.message
                    searchState = result.state
                },
                onReplace = {
                    val result = replaceCurrent(editorView, findQuery, replaceText)
                    statusMessage = result.message
                    searchState = result.state
                },
                onReplaceAll = {
                    val result = replaceAll(editorView, findQuery, replaceText)
                    statusMessage = result.message
                    searchState = result.state
                },
                onClose = { showFindPanel = false },
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (val state = loadState) {
                EditorLoadState.Loading -> EditorCenteredMessage("Carregando arquivo de texto...")
                is EditorLoadState.Error -> EditorErrorPanel(state.message, onOpenExternal)
                is EditorLoadState.Ready -> {
                    key(file.absolutePath, state.document.encoding.label) {
                        CodeEditorView(
                            initialText = editorView?.text?.toString() ?: workingText,
                            readOnly = state.document.truncated || isArchivePreview,
                            extension = syntaxExtension,
                            syntaxHighlight = !state.document.truncated && state.document.text.length <= SYNTAX_HIGHLIGHT_MAX_CHARS,
                            preferences = editorPreferences,
                            onViewReady = { editorView = it },
                            onMetricsState = { updatedMetrics -> metrics = updatedMetrics },
                            onContentChanged = {
                                if (showFindPanel && findQuery.isNotBlank()) {
                                    searchState = editorView?.searchState(findQuery) ?: EditorSearchState()
                                }
                            },
                            onHistoryState = { history ->
                                historyState = history
                                dirty = history.dirty
                            },
                        )
                    }
                    if (viewMode == EditorViewMode.PREVIEW && supportsPreview && !showFullScreenPreview) {
                        WebPreview(
                            file = file,
                            extension = extension,
                            source = previewSource,
                            revision = previewRevision,
                            onError = { previewError = it },
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (previewError.isNotBlank()) {
                            Text(
                                previewError,
                                color = Color(0xFF8A251D),
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth().background(Color(0xFFFFE8E5)).padding(8.dp),
                            )
                        }
                    }
                }
            }
        }

        if (viewMode == EditorViewMode.PREVIEW && supportsPreview) {
            WebPreviewToolbar(
                onRefresh = ::refreshPreview,
                onFullScreen = { onFullScreenChange(true) },
                onCode = { viewMode = EditorViewMode.CODE },
            )
        }

        EditorStatusBar(
            state = loadState,
            metrics = metrics,
            dirty = dirty,
            preferences = editorPreferences,
        )
        }

        if (showFullScreenPreview) {
        FullScreenWebPreview(
            file = file,
            extension = extension,
            source = previewSource,
            revision = previewRevision,
            error = previewError,
            onError = { previewError = it },
            onRefresh = ::refreshPreview,
            onBackToCode = {
                onFullScreenChange(false)
                viewMode = EditorViewMode.CODE
            },
            onExitFullScreen = { onFullScreenChange(false) },
        )
        }
    }
}

@Composable
private fun EditorToolbar(
    dirty: Boolean,
    editable: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    supportsPreview: Boolean,
    viewMode: EditorViewMode,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFind: () -> Unit,
    onTogglePreview: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(XpPanel)
            .border(1.dp, XpChromeBorder)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp),
    ) {
        EditorButton(if (dirty) "Salvar *" else "Salvar", enabled = editable && dirty, onClick = onSave)
        Spacer(Modifier.width(5.dp))
        EditorButton("Salvar como", enabled = editable, onClick = onSaveAs)
        Spacer(Modifier.width(8.dp))
        EditorButton("Desfazer", enabled = editable && canUndo, onClick = onUndo)
        Spacer(Modifier.width(5.dp))
        EditorButton("Refazer", enabled = editable && canRedo, onClick = onRedo)
        Spacer(Modifier.width(8.dp))
        EditorButton("Localizar", onClick = onFind)
        if (supportsPreview) {
            Spacer(Modifier.width(5.dp))
            EditorButton(if (viewMode == EditorViewMode.CODE) "Visualizar" else "Código", onClick = onTogglePreview)
        }
        Spacer(Modifier.width(5.dp))
        EditorButton("Mais", onClick = onMore)
    }
}

@Composable
private fun FindReplacePanel(
    query: String,
    replacement: String,
    showReplace: Boolean,
    resultLabel: String,
    onQueryChange: (String) -> Unit,
    onReplacementChange: (String) -> Unit,
    onToggleReplace: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F5FA))
            .border(1.dp, Color(0xFFB8C7DA))
            .padding(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            EditorTextInput(query, "Localizar", onQueryChange, Modifier.widthIn(min = 130.dp, max = 260.dp))
            if (resultLabel.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                Text(resultLabel, fontSize = 10.sp, color = XpTextSecondary, maxLines = 1)
            }
            Spacer(Modifier.width(5.dp))
            EditorButton("◀", enabled = query.isNotEmpty(), onClick = onPrevious)
            Spacer(Modifier.width(3.dp))
            EditorButton("▶", enabled = query.isNotEmpty(), onClick = onNext)
            Spacer(Modifier.width(5.dp))
            EditorButton(if (showReplace) "Ocultar substituir" else "Substituir", onClick = onToggleReplace)
            Spacer(Modifier.width(5.dp))
            EditorButton("×", onClick = onClose)
        }
        if (showReplace) {
            Spacer(Modifier.height(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            ) {
                EditorTextInput(replacement, "Substituir por", onReplacementChange, Modifier.widthIn(min = 130.dp, max = 260.dp))
                Spacer(Modifier.width(5.dp))
                EditorButton("Substituir", enabled = query.isNotEmpty(), onClick = onReplace)
                Spacer(Modifier.width(4.dp))
                EditorButton("Todos", enabled = query.isNotEmpty(), onClick = onReplaceAll)
            }
        }
    }
}

@Composable
private fun EditorTextInput(
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(28.dp)
            .background(Color.White)
            .border(1.dp, XpControlBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) Text(hint, fontSize = 11.sp, color = Color(0xFF8995A3))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFF202020)),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CodeEditorView(
    initialText: String,
    readOnly: Boolean,
    extension: String,
    syntaxHighlight: Boolean,
    preferences: EditorPreferences,
    onViewReady: (CodeEditText) -> Unit,
    onMetricsState: (EditorMetrics) -> Unit,
    onContentChanged: () -> Unit,
    onHistoryState: (EditorHistoryState) -> Unit,
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            runCatching {
                CodeEditText(context).apply {
                    configure(
                        initialText = initialText,
                        readOnly = readOnly,
                        extension = extension,
                        syntaxHighlight = syntaxHighlight,
                        preferences = preferences,
                        onMetricsState = onMetricsState,
                        onContentChanged = onContentChanged,
                        onHistoryState = onHistoryState,
                    )
                    onViewReady(this)
                }
            }.getOrElse { error ->
                EditText(context).apply {
                    setText(
                        "Não foi possível inicializar o editor interno.\n\n" +
                            (error.message ?: "Erro inesperado.")
                    )
                    isFocusable = false
                    isFocusableInTouchMode = false
                    isClickable = false
                    setTextColor(android.graphics.Color.rgb(122, 33, 26))
                    setBackgroundColor(android.graphics.Color.WHITE)
                    setPadding(28, 28, 28, 28)
                }
            }
        },
        update = { view ->
            if (view is CodeEditText) {
                view.updatePreferences(preferences)
                onViewReady(view)
            }
        },
    )
}

private class CodeEditText(context: Context) : EditText(context) {
    private data class EditOperation(
        val start: Int,
        val before: String,
        val after: String,
        val beforeStateId: Long,
        val afterStateId: Long,
    )

    private data class AutomaticEdit(
        val start: Int,
        val before: String,
        val after: String,
        val cursor: Int,
    )

    private val undoStack = ArrayDeque<EditOperation>()
    private val redoStack = ArrayDeque<EditOperation>()
    private var undoChars = 0
    private var redoChars = 0
    private val lineStarts = ArrayList<Int>()
    private var nextStateId = 0L
    private var currentStateId = 0L
    private var savedStateId = 0L
    private var suppressHistory = false
    private var beforeStart = 0
    private var beforeText = ""
    private var pendingInsertedLength = 0
    private var editorReadOnly = false
    private var preferences = EditorPreferences()
    // Android/TextView pode chamar onSelectionChanged() ainda durante o construtor da superclasse.
    // Os callbacks permanecem anuláveis até configure() terminar.
    private var callbackMetricsState: ((EditorMetrics) -> Unit)? = null
    private var callbackContentChanged: (() -> Unit)? = null
    private var callbackHistoryState: ((EditorHistoryState) -> Unit)? = null
    private var sourceExtension: String = ""
    private var syntaxHighlightEnabled = false
    private val highlightHandler = Handler(Looper.getMainLooper())
    private val highlightRunnable = Runnable { applySyntaxHighlighting() }
    private val gutterBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(239, 244, 249) }
    private val gutterDivider = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(190, 204, 220) }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(104, 121, 140)
        textSize = 10f * resources.displayMetrics.scaledDensity
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.RIGHT
    }
    private var gutterWidthPx = 44f * resources.displayMetrics.density
    private val innerPaddingPx = (8f * resources.displayMetrics.density).toInt()

    fun configure(
        initialText: String,
        readOnly: Boolean,
        extension: String,
        syntaxHighlight: Boolean,
        preferences: EditorPreferences,
        onMetricsState: (EditorMetrics) -> Unit,
        onContentChanged: () -> Unit,
        onHistoryState: (EditorHistoryState) -> Unit,
    ) {
        callbackMetricsState = onMetricsState
        callbackContentChanged = onContentChanged
        callbackHistoryState = onHistoryState
        sourceExtension = extension
        syntaxHighlightEnabled = syntaxHighlight && extension in syntaxExtensions
        this.preferences = preferences
        setBackgroundColor(android.graphics.Color.WHITE)
        setTextColor(android.graphics.Color.rgb(32, 32, 32))
        typeface = Typeface.MONOSPACE
        textSize = 13f
        gravity = Gravity.TOP or Gravity.START
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        isVerticalScrollBarEnabled = true
        setTextIsSelectable(true)
        editorReadOnly = readOnly
        isFocusable = true
        isFocusableInTouchMode = true
        isCursorVisible = !readOnly
        isLongClickable = true
        if (readOnly) keyListener = null
        applyWordWrap(preferences.wordWrap)
        suppressHistory = true
        setText(initialText)
        suppressHistory = false
        rebuildLineIndex(initialText)
        updateGutterPadding()
        if (!readOnly) setSelection(0)
        addTextChangedListener(historyWatcher)
        notifyMetrics()
        notifyHistory()
        scheduleHighlight()
    }

    fun updatePreferences(updated: EditorPreferences) {
        if (preferences == updated) return
        val wordWrapChanged = preferences.wordWrap != updated.wordWrap
        preferences = updated
        if (wordWrapChanged) {
            applyWordWrap(updated.wordWrap)
            requestLayout()
            invalidate()
        }
    }

    private fun applyWordWrap(enabled: Boolean) {
        setHorizontallyScrolling(!enabled)
        isHorizontalScrollBarEnabled = !enabled
    }

    private val historyWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            if (suppressHistory) return
            beforeStart = start
            beforeText = s?.subSequence(start, (start + count).coerceAtMost(s.length))?.toString().orEmpty()
            pendingInsertedLength = after
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(s: Editable?) {
            if (suppressHistory) return
            val editable = s ?: return
            val safeStart = beforeStart.coerceIn(0, editable.length)
            val insertedEnd = (safeStart + pendingInsertedLength).coerceIn(safeStart, editable.length)
            val inserted = editable.subSequence(safeStart, insertedEnd).toString()

            var operationStart = safeStart
            var operationBefore = beforeText
            var operationAfter = inserted
            var desiredCursor: Int? = null
            if (!editorReadOnly) {
                val automatic = applyEditorAssists(editable, safeStart, beforeText, inserted)
                if (automatic != null) {
                    operationStart = automatic.start
                    operationBefore = automatic.before
                    operationAfter = automatic.after
                    desiredCursor = automatic.cursor
                }
            }

            if (operationBefore != operationAfter) {
                val beforeState = currentStateId
                val afterState = ++nextStateId
                val operation = EditOperation(
                    start = operationStart,
                    before = operationBefore,
                    after = operationAfter,
                    beforeStateId = beforeState,
                    afterStateId = afterState,
                )
                currentStateId = afterState
                undoStack.addLast(operation)
                undoChars += operation.historyCharCost()
                trimUndoHistory()
                redoStack.clear()
                redoChars = 0
                updateLineIndex(operation.start, operation.before, operation.after)
                callbackContentChanged?.invoke()
            }

            desiredCursor?.let { cursor ->
                val target = cursor.coerceIn(0, editable.length)
                if (selectionStart != target || selectionEnd != target) setSelection(target)
            }
            updateGutterPadding()
            notifyMetrics()
            notifyHistory()
            scheduleHighlight()
            invalidate()
        }
    }

    private fun applyEditorAssists(
        editable: Editable,
        start: Int,
        replacedText: String,
        insertedText: String,
    ): AutomaticEdit? {
        if (insertedText.length != 1) return null
        val typed = insertedText[0]
        val initialEnd = (start + 1).coerceAtMost(editable.length)
        val next = editable.getOrNull(initialEnd)

        // Se o par de fechamento já está à frente do cursor, apenas avança sobre ele.
        if (replacedText.isEmpty() && typed in ")]}'\"" && next == typed) {
            suppressHistory = true
            editable.delete(start, initialEnd)
            suppressHistory = false
            return AutomaticEdit(start, "", "", start + 1)
        }

        if (typed == '\n') {
            val previousBreak = if (start <= 0) -1 else editable.lastIndexOf('\n', start - 1)
            val previousLineStart = if (previousBreak < 0) 0 else previousBreak + 1
            val previousLine = editable.subSequence(previousLineStart, start).toString()
            val baseIndent = previousLine.takeWhile { it == ' ' || it == '\t' }
            val trimmed = previousLine.trimEnd()
            val lastMeaningful = trimmed.lastOrNull()
            val increaseIndent = when {
                lastMeaningful in setOf('{', '[', '(') -> true
                sourceExtension == "py" && trimmed.endsWith(':') -> true
                sourceExtension in setOf("yml", "yaml") && trimmed.endsWith(':') -> true
                else -> false
            }
            val indent = baseIndent + if (increaseIndent) preferences.indentUnit else ""
            val matchingClose = when (lastMeaningful) {
                '{' -> '}'
                '[' -> ']'
                '(' -> ')'
                else -> null
            }
            val after = if (matchingClose != null && next == matchingClose) {
                "\n$indent\n$baseIndent"
            } else {
                "\n$indent"
            }
            if (after != insertedText) {
                suppressHistory = true
                editable.replace(start, initialEnd, after)
                suppressHistory = false
                val cursor = start + 1 + indent.length
                return AutomaticEdit(start, replacedText, after, cursor)
            }
            return null
        }

        val close = when (typed) {
            '(' -> ')'
            '[' -> ']'
            '{' -> '}'
            '\'' -> '\''
            '"' -> '"'
            else -> null
        } ?: return null

        if ((typed == '\'' || typed == '"') && isEscapedAt(editable, start)) return null
        if (typed == '\'' && replacedText.isEmpty()) {
            val previous = editable.getOrNull(start - 1)
            val following = editable.getOrNull(start + 1)
            if (previous?.isLetterOrDigit() == true && following?.isLetterOrDigit() == true) return null
        }

        val paired = if (replacedText.isNotEmpty()) "$typed$replacedText$close" else "$typed$close"
        suppressHistory = true
        editable.replace(start, initialEnd, paired)
        suppressHistory = false
        val cursor = if (replacedText.isNotEmpty()) start + paired.length else start + 1
        return AutomaticEdit(start, replacedText, paired, cursor)
    }

    private fun isEscapedAt(text: CharSequence, position: Int): Boolean {
        var slashes = 0
        var index = position - 1
        while (index >= 0 && text[index] == '\\') {
            slashes++
            index--
        }
        return slashes % 2 == 1
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (!suppressHistory) notifyMetrics()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_TAB && !editorReadOnly) {
            val editable = text ?: return super.onKeyDown(keyCode, event)
            val start = selectionStart.coerceAtLeast(0)
            val end = selectionEnd.coerceAtLeast(start)
            editable.replace(start, end, preferences.indentUnit)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    fun undoEdit() {
        val op = undoStack.removeLastOrNull() ?: return
        undoChars = (undoChars - op.historyCharCost()).coerceAtLeast(0)
        applyOperation(op, undo = true)
        currentStateId = op.beforeStateId
        redoStack.addLast(op)
        redoChars += op.historyCharCost()
        trimRedoHistory()
        notifyHistory()
    }

    fun redoEdit() {
        val op = redoStack.removeLastOrNull() ?: return
        redoChars = (redoChars - op.historyCharCost()).coerceAtLeast(0)
        applyOperation(op, undo = false)
        currentStateId = op.afterStateId
        undoStack.addLast(op)
        undoChars += op.historyCharCost()
        trimUndoHistory()
        notifyHistory()
    }

    private fun applyOperation(op: EditOperation, undo: Boolean) {
        val editable = text ?: return
        val before = if (undo) op.after else op.before
        val after = if (undo) op.before else op.after
        suppressHistory = true
        val end = (op.start + before.length).coerceAtMost(editable.length)
        editable.replace(op.start.coerceAtMost(editable.length), end, after)
        updateLineIndex(op.start, before, after)
        val cursor = (op.start + after.length).coerceIn(0, editable.length)
        setSelection(cursor)
        suppressHistory = false
        callbackContentChanged?.invoke()
        updateGutterPadding()
        notifyMetrics()
        scheduleHighlight()
        invalidate()
    }

    fun markSaved() {
        savedStateId = currentStateId
        undoStack.clear()
        redoStack.clear()
        undoChars = 0
        redoChars = 0
        notifyHistory()
    }

    fun replaceWholeText(newText: String) {
        if (editorReadOnly) return
        val editable = text ?: return
        editable.replace(0, editable.length, newText)
    }

    fun isReadOnlyMode(): Boolean = editorReadOnly

    fun currentMetrics(): EditorMetrics = metricsAt(selectionStart.coerceAtLeast(0))

    fun goToLine(requestedLine: Int): Boolean {
        if (requestedLine !in 1..lineStarts.size) return false
        val target = lineStarts[requestedLine - 1].coerceIn(0, text?.length ?: 0)
        setSelection(target)
        requestFocus()
        return true
    }

    fun searchState(query: String): EditorSearchState {
        if (query.isBlank()) return EditorSearchState()
        val source = text?.toString().orEmpty()
        if (source.isEmpty()) return EditorSearchState()
        var total = 0
        var current = 0
        var index = 0
        val selectedStart = selectionStart.coerceAtLeast(0)
        val selectedEnd = selectionEnd.coerceAtLeast(selectedStart)
        while (index <= source.length - query.length) {
            val found = source.indexOf(query, startIndex = index, ignoreCase = true)
            if (found < 0) break
            total++
            if (found == selectedStart && found + query.length == selectedEnd) current = total
            index = found + query.length.coerceAtLeast(1)
        }
        return EditorSearchState(total = total, current = current)
    }

    private fun EditOperation.historyCharCost(): Int = before.length + after.length

    private fun trimUndoHistory() {
        while (undoStack.size > 1 && (undoStack.size > HISTORY_LIMIT || undoChars > HISTORY_CHAR_BUDGET)) {
            undoChars = (undoChars - undoStack.removeFirst().historyCharCost()).coerceAtLeast(0)
        }
    }

    private fun trimRedoHistory() {
        while (redoStack.size > 1 && (redoStack.size > HISTORY_LIMIT || redoChars > HISTORY_CHAR_BUDGET)) {
            redoChars = (redoChars - redoStack.removeFirst().historyCharCost()).coerceAtLeast(0)
        }
    }

    private fun notifyHistory() {
        callbackHistoryState?.invoke(
            EditorHistoryState(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                dirty = currentStateId != savedStateId,
            )
        )
    }

    private fun notifyMetrics() {
        callbackMetricsState?.invoke(metricsAt(selectionStart.coerceAtLeast(0)))
    }

    private fun metricsAt(cursor: Int): EditorMetrics {
        if (lineStarts.isEmpty()) return EditorMetrics()
        val safeCursor = cursor.coerceIn(0, text?.length ?: 0)
        val lineIndex = lineIndexForOffset(safeCursor)
        val lineStart = lineStarts.getOrElse(lineIndex) { 0 }
        return EditorMetrics(
            line = lineIndex + 1,
            column = safeCursor - lineStart + 1,
            lineCount = lineStarts.size.coerceAtLeast(1),
        )
    }

    private fun rebuildLineIndex(source: CharSequence) {
        lineStarts.clear()
        lineStarts.add(0)
        source.forEachIndexed { index, c -> if (c == '\n') lineStarts.add(index + 1) }
    }

    private fun updateLineIndex(start: Int, before: String, after: String) {
        if (lineStarts.isEmpty()) lineStarts.add(0)
        val oldEnd = start + before.length
        val delta = after.length - before.length

        var index = lineStarts.size - 1
        while (index > 0) {
            val value = lineStarts[index]
            when {
                value > oldEnd -> lineStarts[index] = value + delta
                value > start && value <= oldEnd -> lineStarts.removeAt(index)
            }
            index--
        }

        val additions = ArrayList<Int>()
        after.forEachIndexed { relative, c -> if (c == '\n') additions.add(start + relative + 1) }
        if (additions.isNotEmpty()) {
            var insertAt = lineStarts.binarySearch(start + 1).let { if (it >= 0) it else -it - 1 }
            additions.forEach { value ->
                while (insertAt < lineStarts.size && lineStarts[insertAt] < value) insertAt++
                lineStarts.add(insertAt, value)
                insertAt++
            }
        }
    }

    private fun lineIndexForOffset(offset: Int): Int {
        if (lineStarts.isEmpty()) return 0
        var low = 0
        var high = lineStarts.lastIndex
        var answer = 0
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lineStarts[mid] <= offset) {
                answer = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return answer
    }

    private fun isLogicalLineStart(offset: Int): Int? {
        val index = lineStarts.binarySearch(offset)
        return index.takeIf { it >= 0 }
    }

    private fun scheduleHighlight() {
        highlightHandler.removeCallbacks(highlightRunnable)
        if (syntaxHighlightEnabled && (text?.length ?: 0) <= SYNTAX_HIGHLIGHT_MAX_CHARS) {
            highlightHandler.postDelayed(highlightRunnable, 180L)
        }
    }

    private fun applySyntaxHighlighting() {
        val editable = text ?: return
        editable.getSpans(0, editable.length, EditorSyntaxSpan::class.java).forEach(editable::removeSpan)
        if (!syntaxHighlightEnabled || editable.length > SYNTAX_HIGHLIGHT_MAX_CHARS) return
        val source = editable.toString()
        syntaxPatterns(sourceExtension).forEach { spec ->
            spec.regex.findAll(source).forEach { match ->
                val start = match.range.first
                val end = match.range.last + 1
                if (start in 0 until end && end <= editable.length) {
                    editable.setSpan(EditorSyntaxSpan(spec.color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }

    private fun updateGutterPadding() {
        val digits = max(2, lineStarts.size.coerceAtLeast(1).toString().length)
        val wanted = ((digits * 8 + 18) * resources.displayMetrics.density)
        if (kotlin.math.abs(wanted - gutterWidthPx) >= 1f || paddingLeft == 0) {
            gutterWidthPx = wanted
            setPadding(gutterWidthPx.toInt() + innerPaddingPx, innerPaddingPx, innerPaddingPx, innerPaddingPx)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val layout = layout
        if (layout != null) {
            val left = scrollX.toFloat()
            val top = scrollY.toFloat()
            canvas.drawRect(left, top, left + gutterWidthPx, top + height, gutterBackground)
            canvas.drawLine(left + gutterWidthPx - 1f, top, left + gutterWidthPx - 1f, top + height, gutterDivider)
            val firstVisualLine = layout.getLineForVertical(scrollY)
            val lastVisualLine = layout.getLineForVertical(scrollY + height)
            for (visualLine in firstVisualLine..lastVisualLine.coerceAtMost(layout.lineCount - 1)) {
                val offset = layout.getLineStart(visualLine)
                val logicalLine = isLogicalLineStart(offset) ?: continue
                val baseline = layout.getLineBaseline(visualLine) + totalPaddingTop
                canvas.drawText(
                    (logicalLine + 1).toString(),
                    left + gutterWidthPx - 7f * resources.displayMetrics.density,
                    baseline.toFloat(),
                    linePaint,
                )
            }
        }
        super.onDraw(canvas)
    }

    override fun onDetachedFromWindow() {
        highlightHandler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }
}

private data class SyntaxPattern(val regex: Regex, val color: Int)

private val syntaxPatternCache = HashMap<String, List<SyntaxPattern>>()

private fun syntaxPatterns(extension: String): List<SyntaxPattern> =
    syntaxPatternCache.getOrPut(extension) { buildSyntaxPatterns(extension) }

private fun buildSyntaxPatterns(extension: String): List<SyntaxPattern> {
    val comment = android.graphics.Color.rgb(75, 132, 73)
    val keyword = android.graphics.Color.rgb(38, 72, 150)
    val string = android.graphics.Color.rgb(155, 57, 42)
    val number = android.graphics.Color.rgb(112, 59, 150)
    val tag = android.graphics.Color.rgb(28, 101, 129)
    val accent = android.graphics.Color.rgb(132, 75, 24)

    val quotedStrings = SyntaxPattern(
        Regex("\\\"(?:\\\\.|[^\\\"\\\\])*\\\"|'(?:\\\\.|[^'\\\\])*'"),
        string,
    )
    val cComments = SyntaxPattern(
        Regex("//.*?$|/\\*.*?\\*/", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)),
        comment,
    )
    val hashComments = SyntaxPattern(Regex("#.*?$", RegexOption.MULTILINE), comment)
    val commonNumbers = SyntaxPattern(
        Regex("(?<![A-Za-z_])[-+]?\\b(?:0x[0-9A-Fa-f]+|\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)\\b"),
        number,
    )

    return when (extension) {
        "html", "htm", "xml", "vue", "svelte" -> listOf(
            SyntaxPattern(Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE), keyword),
            SyntaxPattern(Regex("</?[A-Za-z][^>]*>"), tag),
            SyntaxPattern(Regex("&[A-Za-z0-9#]+;"), accent),
            quotedStrings,
            SyntaxPattern(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), comment),
        )

        "css" -> listOf(
            SyntaxPattern(Regex("@[A-Za-z-]+|[A-Za-z-]+(?=\\s*:)", RegexOption.IGNORE_CASE), keyword),
            SyntaxPattern(Regex("#[0-9A-Fa-f]{3,8}|\\b\\d+(?:\\.\\d+)?(?:px|em|rem|%|vh|vw|vmin|vmax|s|ms|deg)?\\b"), number),
            SyntaxPattern(Regex("[.#]?[A-Za-z_][A-Za-z0-9_-]*(?=\\s*[,{])"), tag),
            quotedStrings,
            SyntaxPattern(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), comment),
        )

        "js", "mjs", "cjs", "ts", "tsx", "jsx" -> listOf(
            SyntaxPattern(
                Regex("\\b(const|let|var|function|return|if|else|for|while|do|switch|case|break|continue|class|extends|new|this|super|async|await|try|catch|finally|throw|import|export|from|default|typeof|instanceof|in|of|interface|type|enum|implements|public|private|protected|readonly|static|yield|null|undefined|true|false)\\b"),
                keyword,
            ),
            SyntaxPattern(Regex("@[A-Za-z_][A-Za-z0-9_]*"), accent),
            SyntaxPattern(Regex("`(?:\\\\.|[^`\\\\])*`", RegexOption.DOT_MATCHES_ALL), string),
            quotedStrings,
            commonNumbers,
            cComments,
        )

        "json" -> listOf(
            SyntaxPattern(Regex("\\\"(?:\\\\.|[^\\\"\\\\])*\\\"(?=\\s*:)", RegexOption.DOT_MATCHES_ALL), keyword),
            SyntaxPattern(Regex("\\\"(?:\\\\.|[^\\\"\\\\])*\\\"", RegexOption.DOT_MATCHES_ALL), string),
            SyntaxPattern(Regex("\\b(true|false|null)\\b"), keyword),
            commonNumbers,
        )

        "md", "markdown", "mds" -> listOf(
            SyntaxPattern(Regex("^#{1,6}\\s+.*$", RegexOption.MULTILINE), keyword),
            SyntaxPattern(Regex("^>\\s?.*$", RegexOption.MULTILINE), comment),
            SyntaxPattern(Regex("\\[[^]]+]\\([^)]+\\)"), tag),
            SyntaxPattern(Regex("`[^`]+`"), string),
            SyntaxPattern(Regex("(?:\\*\\*|__)[^\\n]+?(?:\\*\\*|__)"), accent),
            SyntaxPattern(Regex("^\\s*[-*+]\\s+|^\\s*\\d+\\.\\s+", RegexOption.MULTILINE), number),
            SyntaxPattern(Regex("^```.*$", RegexOption.MULTILINE), comment),
        )

        "yml", "yaml" -> listOf(
            SyntaxPattern(Regex("^[ \\t-]*[A-Za-z0-9_.\\\"']+(?=\\s*:)", RegexOption.MULTILINE), keyword),
            SyntaxPattern(Regex("\\b(true|false|null|yes|no|on|off)\\b", RegexOption.IGNORE_CASE), keyword),
            quotedStrings,
            commonNumbers,
            hashComments,
        )

        "toml", "ini", "cfg", "conf", "properties", "env" -> listOf(
            SyntaxPattern(Regex("^\\s*\\[[^]]+]\\s*$", RegexOption.MULTILINE), tag),
            SyntaxPattern(Regex("^[ \\t]*[A-Za-z0-9_.-]+(?=\\s*[=:])", RegexOption.MULTILINE), keyword),
            quotedStrings,
            commonNumbers,
            SyntaxPattern(Regex("^[ \\t]*[#!;].*$", RegexOption.MULTILINE), comment),
        )

        "kt", "kts", "java", "gradle" -> listOf(
            SyntaxPattern(
                Regex("\\b(package|import|class|interface|object|enum|data|sealed|open|abstract|final|public|private|protected|internal|static|override|fun|val|var|const|lateinit|companion|constructor|init|this|super|new|return|if|else|when|switch|case|for|while|do|break|continue|try|catch|finally|throw|throws|in|is|as|typeof|void|boolean|byte|short|int|long|float|double|char|String|true|false|null)\\b"),
                keyword,
            ),
            SyntaxPattern(Regex("@[A-Za-z_][A-Za-z0-9_.]*"), accent),
            quotedStrings,
            commonNumbers,
            cComments,
        )

        "py" -> listOf(
            SyntaxPattern(
                Regex("\\b(and|as|assert|async|await|break|class|continue|def|del|elif|else|except|False|finally|for|from|global|if|import|in|is|lambda|None|nonlocal|not|or|pass|raise|return|True|try|while|with|yield)\\b"),
                keyword,
            ),
            SyntaxPattern(Regex("@[A-Za-z_][A-Za-z0-9_.]*"), accent),
            quotedStrings,
            commonNumbers,
            hashComments,
        )

        "sh", "bash" -> listOf(
            SyntaxPattern(Regex("\\b(if|then|else|elif|fi|for|while|until|do|done|case|esac|function|in|select|time)\\b"), keyword),
            SyntaxPattern(Regex("\\$\\{?[A-Za-z_][A-Za-z0-9_]*}?"), accent),
            quotedStrings,
            commonNumbers,
            hashComments,
        )

        "bat", "cmd" -> listOf(
            SyntaxPattern(Regex("\\b(if|else|for|in|do|goto|call|set|setlocal|endlocal|shift|exit|echo|pause|start|title|color|choice|errorlevel|exist|defined|not)\\b", RegexOption.IGNORE_CASE), keyword),
            SyntaxPattern(Regex("%[A-Za-z0-9_]+%|%[0-9*~][A-Za-z0-9_:~,.=-]*"), accent),
            quotedStrings,
            commonNumbers,
            SyntaxPattern(Regex("^[ \t]*(?:rem\\b|::).*$", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)), comment),
        )

        "ps1" -> listOf(
            SyntaxPattern(Regex("\\b(function|filter|param|begin|process|end|if|elseif|else|switch|foreach|for|while|do|until|break|continue|return|throw|try|catch|finally|class|enum|using|in)\\b", RegexOption.IGNORE_CASE), keyword),
            SyntaxPattern(Regex("\\$[A-Za-z_][A-Za-z0-9_:]*"), accent),
            quotedStrings,
            commonNumbers,
            hashComments,
        )

        "php" -> listOf(
            SyntaxPattern(Regex("<\\?php|\\?>"), tag),
            SyntaxPattern(Regex("\\b(function|class|interface|trait|namespace|use|public|private|protected|static|final|abstract|if|else|elseif|foreach|for|while|do|switch|case|return|new|throw|try|catch|finally|true|false|null)\\b", RegexOption.IGNORE_CASE), keyword),
            SyntaxPattern(Regex("\\$[A-Za-z_][A-Za-z0-9_]*"), accent),
            quotedStrings,
            commonNumbers,
            cComments,
            hashComments,
        )

        "rb" -> listOf(
            SyntaxPattern(Regex("\\b(class|module|def|end|if|elsif|else|unless|case|when|while|until|for|do|begin|rescue|ensure|return|yield|require|include|extend|attr_reader|attr_writer|attr_accessor|true|false|nil)\\b"), keyword),
            SyntaxPattern(Regex("@[A-Za-z_][A-Za-z0-9_]*|:[A-Za-z_][A-Za-z0-9_]*"), accent),
            quotedStrings,
            commonNumbers,
            hashComments,
        )

        "c", "h", "hpp", "cpp", "cc", "go", "rs", "swift", "dart" -> listOf(
            SyntaxPattern(
                Regex("\\b(auto|break|case|char|class|const|continue|default|defer|do|double|else|enum|extern|false|final|float|for|func|function|if|impl|import|in|int|interface|let|long|match|mut|namespace|new|null|nullptr|override|package|private|protected|public|return|short|sizeof|static|string|struct|super|switch|this|throw|trait|true|try|type|typedef|union|unsigned|use|var|void|while|with|yield)\\b"),
                keyword,
            ),
            SyntaxPattern(Regex("#[A-Za-z_][A-Za-z0-9_]*|@[A-Za-z_][A-Za-z0-9_]*"), accent),
            quotedStrings,
            commonNumbers,
            cComments,
        )

        "sql" -> listOf(
            SyntaxPattern(Regex("\\b(select|from|where|join|left|right|inner|outer|on|group|by|order|having|limit|offset|insert|into|values|update|set|delete|create|alter|drop|table|view|index|primary|key|foreign|references|constraint|distinct|as|and|or|not|null|is|in|exists|case|when|then|else|end|union|all)\\b", RegexOption.IGNORE_CASE), keyword),
            quotedStrings,
            commonNumbers,
            SyntaxPattern(Regex("--.*?$|/\\*.*?\\*/", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)), comment),
        )

        "tex" -> listOf(
            SyntaxPattern(Regex("\\\\[A-Za-z@]+\\*?"), keyword),
            SyntaxPattern(Regex("\\\\(?:begin|end)\\{[^}]+}"), tag),
            SyntaxPattern(Regex("\\$\\$?.*?\\$\\$?"), accent),
            commonNumbers,
            SyntaxPattern(Regex("(?<!\\\\)%.*?$", RegexOption.MULTILINE), comment),
        )

        "csv" -> listOf(
            SyntaxPattern(Regex("\"(?:\"\"|[^\"])*\""), string),
            commonNumbers,
        )

        else -> emptyList()
    }
}

private class EditorSyntaxSpan(private val color: Int) : CharacterStyle(), UpdateAppearance {
    override fun updateDrawState(tp: TextPaint) {
        tp.color = color
    }
}

@Composable
private fun WebPreview(
    file: File,
    extension: String,
    source: String,
    revision: Int,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val html = remember(source, extension, revision) { previewHtml(extension, source) }
    val baseUrl = remember(file.parentFile?.absolutePath) {
        file.parentFile?.toURI()?.toString() ?: "file:///"
    }
    var webView by remember(file.absolutePath) { mutableStateOf<WebView?>(null) }

    AndroidView(
        modifier = modifier.background(Color.White),
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        if (request?.isForMainFrame == true) {
                            onError("Falha ao carregar o preview: ${error?.description ?: "recurso indisponível"}")
                        }
                    }
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = false
                settings.loadsImagesAutomatically = true
                webView = this
                loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
            }
        },
        update = { view ->
            val marker = "$revision:${source.hashCode()}:$extension"
            if (view.tag != marker) {
                view.tag = marker
                view.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
            }
        },
    )

    DisposableEffect(webView) {
        onDispose {
            webView?.stopLoading()
            webView?.loadUrl("about:blank")
            webView?.destroy()
        }
    }
}

private fun previewHtml(extension: String, source: String): String = when (extension) {
    "html", "htm" -> source
    "css" -> """
        <!doctype html><html><head><meta charset="utf-8"><style>$source</style></head>
        <body>
          <h1>Preview CSS</h1><p>Texto de exemplo para visualizar estilos.</p>
          <button>Botão</button><input placeholder="Campo de texto">
          <ul><li>Item um</li><li>Item dois</li></ul>
        </body></html>
    """.trimIndent()
    "js", "mjs", "cjs" -> {
        val safeScript = source.replace("</script", "<\\/script", ignoreCase = true)
        """
            <!doctype html><html><head><meta charset="utf-8"></head>
            <body><h1>Preview JavaScript</h1><p id="explorador-preview">Área de preview.</p>
            <script>$safeScript</script></body></html>
        """.trimIndent()
    }
    else -> source
}

@Composable
private fun WebPreviewToolbar(onRefresh: () -> Unit, onFullScreen: () -> Unit, onCode: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth().height(36.dp).background(XpPanel).border(1.dp, XpChromeBorder).padding(horizontal = 6.dp),
    ) {
        EditorButton("Código", onClick = onCode)
        Spacer(Modifier.width(5.dp))
        EditorButton("Atualizar", onClick = onRefresh)
        Spacer(Modifier.width(5.dp))
        EditorButton("Tela cheia", onClick = onFullScreen)
    }
}

@Composable
private fun FullScreenWebPreview(
    file: File,
    extension: String,
    source: String,
    revision: Int,
    error: String,
    onError: (String) -> Unit,
    onRefresh: () -> Unit,
    onBackToCode: () -> Unit,
    onExitFullScreen: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(38.dp).background(Color(0xFFE8EFF7)).padding(horizontal = 6.dp),
        ) {
            Text("Preview • ${file.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            EditorButton("Código", onClick = onBackToCode)
            Spacer(Modifier.width(4.dp))
            EditorButton("Atualizar", onClick = onRefresh)
            Spacer(Modifier.width(4.dp))
            EditorButton("Sair da tela cheia", onClick = onExitFullScreen)
        }
        if (error.isNotBlank()) {
            Text(error, color = Color(0xFF8A251D), fontSize = 11.sp, modifier = Modifier.fillMaxWidth().background(Color(0xFFFFE8E5)).padding(6.dp))
        }
        WebPreview(file, extension, source, revision, onError, Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
private fun EditorStatusBar(
    state: EditorLoadState,
    metrics: EditorMetrics,
    dirty: Boolean,
    preferences: EditorPreferences,
) {
    val doc = (state as? EditorLoadState.Ready)?.document
    val line = metrics.line
    val column = metrics.column
    val lineCount = metrics.lineCount
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(28.dp).background(XpChrome).border(1.dp, XpChromeBorder).padding(horizontal = 8.dp),
    ) {
        Text(
            "Linha $line, Coluna $column  •  ${if (doc?.truncated == true) "$lineCount+" else lineCount} linhas${if (dirty) "  •  não salvo" else ""}",
            fontSize = 10.sp,
            color = Color(0xFF303030),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            buildString {
                append(doc?.encoding?.label ?: "—")
                append("  •  ")
                append(if (preferences.useTabs) "TAB" else "${preferences.indentSize} esp.")
                if (preferences.wordWrap) append("  •  quebra")
            },
            fontSize = 10.sp,
            color = XpTextSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun EditorButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(28.dp)
            .widthIn(min = 42.dp)
            .background(
                color = when {
                    !enabled -> Color(0xFFF0F2F4)
                    pressed -> XpControlPressed
                    else -> XpControlBackground
                },
                shape = RoundedCornerShape(4.dp),
            )
            .border(1.dp, if (enabled) XpControlBorder else Color(0xFFD3D9E0), RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp),
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) Color(0xFF202020) else Color(0xFF969696), maxLines = 1)
    }
}

@Composable
private fun EditorCenteredMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = XpTextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun EditorErrorPanel(message: String, onOpenExternal: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(20.dp),
    ) {
        Text("Não foi possível abrir como texto", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7A211A))
        Spacer(Modifier.height(8.dp))
        Text(message, fontSize = 12.sp, color = Color(0xFF4B4B4B))
        Spacer(Modifier.height(14.dp))
        EditorButton("Abrir com outro aplicativo", onClick = onOpenExternal)
    }
}

@Composable
private fun SafeEditorDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SaveAsDialog(initialName: String, parentPath: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember(initialName) { mutableStateOf(initialName) }
    SafeEditorDialog(onDismiss) {
        EditorDialogSurface("Salvar como") {
            Text("Pasta", fontSize = 10.sp, color = XpTextSecondary, fontWeight = FontWeight.Bold)
            Text(parentPath, fontSize = 11.sp, color = Color(0xFF303030), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Text("Nome do arquivo", fontSize = 11.sp, color = XpTextSecondary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            EditorTextInput(value, "Nome", { value = it }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                EditorButton("Cancelar", onClick = onDismiss)
                Spacer(Modifier.width(6.dp))
                EditorButton("Salvar", enabled = isSafeFileName(value), onClick = { onConfirm(value.trim()) })
            }
        }
    }
}

@Composable
private fun GoToLineDialog(onDismiss: () -> Unit, onGo: (Int) -> Unit) {
    var value by remember { mutableStateOf("") }
    SafeEditorDialog(onDismiss) {
        EditorDialogSurface("Ir para linha") {
            Text("Digite o número da linha:", fontSize = 12.sp, color = Color(0xFF303030))
            Spacer(Modifier.height(7.dp))
            EditorTextInput(value, "Linha", { new -> value = new.filter(Char::isDigit).take(9) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                EditorButton("Cancelar", onClick = onDismiss)
                Spacer(Modifier.width(6.dp))
                EditorButton("Ir", enabled = value.toIntOrNull()?.let { it > 0 } == true, onClick = { onGo(value.toInt()) })
            }
        }
    }
}

@Composable
private fun EditorSettingsDialog(
    preferences: EditorPreferences,
    onDismiss: () -> Unit,
    onChange: (EditorPreferences) -> Unit,
) {
    SafeEditorDialog(onDismiss) {
        EditorDialogSurface("Configurações do editor") {
            Text("Indentação", fontSize = 11.sp, color = XpTextSecondary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EditorButton(if (!preferences.useTabs) "✓ Espaços" else "Espaços") {
                    onChange(preferences.copy(useTabs = false))
                }
                Spacer(Modifier.width(5.dp))
                EditorButton(if (preferences.useTabs) "✓ TAB" else "TAB") {
                    onChange(preferences.copy(useTabs = true))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Espaços por nível", fontSize = 11.sp, color = XpTextSecondary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(2, 4, 8).forEachIndexed { index, size ->
                    if (index > 0) Spacer(Modifier.width(5.dp))
                    EditorButton(if (!preferences.useTabs && preferences.indentSize == size) "✓ $size" else "$size") {
                        onChange(preferences.copy(useTabs = false, indentSize = size))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            EditorMenuRow(
                label = "Quebra automática de linha: ${if (preferences.wordWrap) "ativada" else "desativada"}",
                enabled = true,
                onClick = { onChange(preferences.copy(wordWrap = !preferences.wordWrap)) },
            )
            Text(
                "As opções são salvas para os próximos arquivos. Autoindentação e fechamento de pares permanecem ativos.",
                fontSize = 10.sp,
                color = XpTextSecondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                EditorButton("Fechar", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun EditorMoreDialog(
    editable: Boolean,
    supportsPreview: Boolean,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: () -> Unit,
    onGoToLine: () -> Unit,
    onFindReplace: () -> Unit,
    onSettings: () -> Unit,
    onPreview: () -> Unit,
) {
    SafeEditorDialog(onDismiss) {
        EditorDialogSurface("Editar") {
            EditorMenuRow("Selecionar tudo", true, onSelectAll)
            EditorMenuRow("Copiar", true, onCopy)
            EditorMenuRow("Recortar", editable, onCut)
            EditorMenuRow("Colar", editable, onPaste)
            HorizontalDivider(color = Color(0xFFD3DCE6), modifier = Modifier.padding(vertical = 4.dp))
            EditorMenuRow("Localizar e substituir", true, onFindReplace)
            EditorMenuRow("Ir para linha", true, onGoToLine)
            EditorMenuRow("Configurações do editor", true, onSettings)
            if (supportsPreview) EditorMenuRow("Visualizar preview", true, onPreview)
        }
    }
}

@Composable
private fun EditorMenuRow(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 12.sp,
        color = if (enabled) Color(0xFF202020) else Color(0xFF9A9A9A),
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 6.dp, vertical = 8.dp),
    )
}

@Composable
private fun EditorChoiceDialog(
    title: String,
    message: String,
    primaryLabel: String,
    secondaryLabel: String?,
    cancelLabel: String,
    destructivePrimary: Boolean = false,
    destructiveSecondary: Boolean = false,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onCancel: () -> Unit,
) {
    SafeEditorDialog(onCancel) {
        EditorDialogSurface(title) {
            Text(message, fontSize = 12.sp, color = Color(0xFF303030))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                EditorButton(cancelLabel, onClick = onCancel)
                if (secondaryLabel != null) {
                    Spacer(Modifier.width(6.dp))
                    EditorDialogActionButton(secondaryLabel, destructiveSecondary, onSecondary)
                }
                Spacer(Modifier.width(6.dp))
                EditorDialogActionButton(primaryLabel, destructivePrimary, onPrimary)
            }
        }
    }
}

@Composable
private fun EditorDialogActionButton(label: String, destructive: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(29.dp)
            .background(if (destructive) Color(0xFFFFE9E6) else XpControlBackground, RoundedCornerShape(4.dp))
            .border(1.dp, if (destructive) Color(0xFFE0A39D) else XpControlBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp),
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (destructive) Color(0xFF8C2018) else Color(0xFF202020), maxLines = 1)
    }
}

@Composable
private fun EditorDialogSurface(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 480.dp)
            .background(Color(0xFFF9FBFD), RoundedCornerShape(6.dp))
            .border(1.dp, XpBorder, RoundedCornerShape(6.dp))
    ) {
        Text(
            title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().background(XpBlue).padding(horizontal = 8.dp, vertical = 6.dp),
        )
        Column(Modifier.fillMaxWidth().padding(12.dp)) { content() }
    }
}

private fun findInEditor(view: CodeEditText?, query: String, forward: Boolean): EditorSearchResult {
    if (view == null || query.isBlank()) return EditorSearchResult("Digite um texto para localizar", EditorSearchState())
    val source = view.text?.toString().orEmpty()
    if (source.isEmpty()) return EditorSearchResult("Arquivo vazio", EditorSearchState())
    val selectionStart = view.selectionStart.coerceAtLeast(0)
    val selectionEnd = view.selectionEnd.coerceAtLeast(selectionStart)
    val found = if (forward) {
        val start = if (selectionEnd < source.length) selectionEnd else 0
        source.indexOf(query, startIndex = start, ignoreCase = true).takeIf { it >= 0 }
            ?: source.indexOf(query, startIndex = 0, ignoreCase = true).takeIf { it >= 0 }
    } else {
        val start = if (selectionStart > 0) selectionStart - 1 else source.length
        source.lastIndexOf(query, startIndex = start, ignoreCase = true).takeIf { it >= 0 }
            ?: source.lastIndexOf(query, startIndex = source.length, ignoreCase = true).takeIf { it >= 0 }
    }
    if (found == null) {
        return EditorSearchResult("Texto não encontrado", view.searchState(query))
    }
    view.requestFocus()
    view.setSelection(found, found + query.length)
    val state = view.searchState(query)
    val label = if (state.total == 1) "1 ocorrência" else "Ocorrência ${state.current} de ${state.total}"
    return EditorSearchResult(label, state)
}

private fun replaceCurrent(view: CodeEditText?, query: String, replacement: String): EditorSearchResult {
    if (view == null || query.isBlank()) return EditorSearchResult("Digite um texto para localizar", EditorSearchState())
    if (view.isReadOnlyMode()) return EditorSearchResult("Arquivo em modo somente leitura", view.searchState(query))
    val start = view.selectionStart.coerceAtLeast(0)
    val end = view.selectionEnd.coerceAtLeast(start)
    val selected = view.text?.subSequence(start, end)?.toString().orEmpty()
    if (!selected.equals(query, ignoreCase = true)) return findInEditor(view, query, forward = true)

    view.text?.replace(start, end, replacement)
    view.setSelection((start + replacement.length).coerceAtMost(view.text?.length ?: 0))
    val next = findInEditor(view, query, forward = true)
    return if (next.state.total == 0) {
        EditorSearchResult("Ocorrência substituída; não restam resultados", next.state)
    } else {
        EditorSearchResult("Ocorrência substituída • ${next.state.current}/${next.state.total}", next.state)
    }
}

private fun replaceAll(view: CodeEditText?, query: String, replacement: String): EditorSearchResult {
    if (view == null || query.isBlank()) return EditorSearchResult("Digite um texto para localizar", EditorSearchState())
    if (view.isReadOnlyMode()) return EditorSearchResult("Arquivo em modo somente leitura", view.searchState(query))
    val source = view.text?.toString().orEmpty()
    val regex = Regex(Regex.escape(query), RegexOption.IGNORE_CASE)
    val count = regex.findAll(source).count()
    if (count == 0) return EditorSearchResult("Nenhuma ocorrência encontrada", EditorSearchState())
    view.replaceWholeText(regex.replace(source) { replacement })
    return EditorSearchResult(
        if (count == 1) "1 ocorrência substituída" else "$count ocorrências substituídas",
        view.searchState(query),
    )
}

private fun goToLine(view: CodeEditText, requestedLine: Int): Boolean = view.goToLine(requestedLine)

private fun editorSyntaxExtension(file: File): String = when (file.name.lowercase()) {
    "readme" -> "md"
    "makefile", "dockerfile" -> "sh"
    ".gitignore", ".gitattributes" -> "conf"
    ".editorconfig" -> "ini"
    else -> file.extension.lowercase()
}

private fun loadEditorPreferences(context: Context): EditorPreferences {
    val prefs = context.getSharedPreferences(EDITOR_PREFERENCES, Context.MODE_PRIVATE)
    val indentSize = prefs.getInt(PREF_INDENT_SIZE, 4).takeIf { it in setOf(2, 4, 8) } ?: 4
    return EditorPreferences(
        useTabs = prefs.getBoolean(PREF_USE_TABS, false),
        indentSize = indentSize,
        wordWrap = prefs.getBoolean(PREF_WORD_WRAP, false),
    )
}

private fun saveEditorPreferences(context: Context, preferences: EditorPreferences) {
    context.getSharedPreferences(EDITOR_PREFERENCES, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(PREF_USE_TABS, preferences.useTabs)
        .putInt(PREF_INDENT_SIZE, preferences.indentSize)
        .putBoolean(PREF_WORD_WRAP, preferences.wordWrap)
        .apply()
}

private fun metricsForText(text: String, cursor: Int): EditorMetrics {
    val safeCursor = cursor.coerceIn(0, text.length)
    var line = 1
    var lineStart = 0
    var lineCount = 1
    text.forEachIndexed { index, c ->
        if (c == '\n') {
            lineCount++
            if (index < safeCursor) {
                line++
                lineStart = index + 1
            }
        }
    }
    return EditorMetrics(
        line = line,
        column = safeCursor - lineStart + 1,
        lineCount = lineCount,
    )
}

private fun loadEditorDocument(file: File): EditorLoadState {
    if (!file.exists() || !file.isFile) return EditorLoadState.Error("O arquivo não existe mais ou não pode ser acessado.")
    if (!file.canRead()) return EditorLoadState.Error("O Android não permitiu a leitura deste arquivo.")

    return runCatching {
        val fileSize = file.length()
        val truncated = fileSize > EDITOR_EDIT_MAX_BYTES
        val maxBytes = if (truncated) EDITOR_PREVIEW_MAX_BYTES else fileSize.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val bytes = FileInputStream(file).use { input ->
            val buffer = ByteArray(maxBytes)
            var total = 0
            while (total < buffer.size) {
                val read = input.read(buffer, total, buffer.size - total)
                if (read <= 0) break
                total += read
            }
            buffer.copyOf(total)
        }
        val encoding = detectEncoding(bytes)
            ?: return EditorLoadState.Error("O conteúdo parece binário ou usa uma codificação que o editor não reconheceu com segurança.", likelyBinary = true)
        val text = decodeText(bytes, encoding, tolerateTruncated = truncated)
        EditorLoadState.Ready(
            EditorDocument(
                text = text,
                encoding = encoding,
                truncated = truncated,
                lineEnding = detectLineEnding(text),
            )
        )
    }.getOrElse { error ->
        EditorLoadState.Error("Não foi possível ler o arquivo. ${error.message.orEmpty()}".trim())
    }
}

private fun detectEncoding(bytes: ByteArray): EditorEncoding? {
    val utf8 = Charsets.UTF_8
    val utf16le = Charsets.UTF_16LE
    val utf16be = Charsets.UTF_16BE
    if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
        return EditorEncoding(utf8, "UTF-8 BOM", byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
    }
    if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
        return EditorEncoding(utf16le, "UTF-16 LE", byteArrayOf(0xFF.toByte(), 0xFE.toByte()))
    }
    if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
        return EditorEncoding(utf16be, "UTF-16 BE", byteArrayOf(0xFE.toByte(), 0xFF.toByte()))
    }

    val sample = bytes.take(32_768).toByteArray()
    if (looksBinary(sample)) return null
    if (canDecodeStrict(sample, utf8)) return EditorEncoding(utf8, "UTF-8")
    val win1252 = Charset.forName("windows-1252")
    return if (!looksBinary(sample)) EditorEncoding(win1252, "Windows-1252") else null
}

private fun looksBinary(bytes: ByteArray): Boolean {
    if (bytes.isEmpty()) return false
    var suspicious = 0
    var zeroes = 0
    for (b in bytes) {
        val v = b.toInt() and 0xFF
        if (v == 0) zeroes++
        if (v < 0x09 || (v in 0x0E..0x1F)) suspicious++
    }
    return zeroes > bytes.size / 20 || suspicious > bytes.size / 10
}

private fun canDecodeStrict(bytes: ByteArray, charset: Charset): Boolean = try {
    charset.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes))
    true
} catch (_: CharacterCodingException) {
    false
}

private fun decodeText(bytes: ByteArray, encoding: EditorEncoding, tolerateTruncated: Boolean): String {
    val offset = encoding.bom.size.coerceAtMost(bytes.size)
    val body = ByteBuffer.wrap(bytes, offset, bytes.size - offset)
    val decoder = encoding.charset.newDecoder()
        .onMalformedInput(if (tolerateTruncated) CodingErrorAction.REPLACE else CodingErrorAction.REPORT)
        .onUnmappableCharacter(if (tolerateTruncated) CodingErrorAction.REPLACE else CodingErrorAction.REPORT)
    return decoder.decode(body).toString()
}

private fun detectLineEnding(text: String): String {
    val crlf = Regex("\\r\\n").findAll(text).count()
    val lf = text.count { it == '\n' } - crlf
    return if (crlf > lf) "\r\n" else "\n"
}

private fun normalizeLineEndings(text: String, preferred: String): String {
    val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
    return if (preferred == "\r\n") normalized.replace("\n", "\r\n") else normalized
}

private fun safeWriteTextFile(
    target: File,
    text: String,
    encoding: EditorEncoding,
    preferredLineEnding: String,
): Result<Unit> = runCatching {
    val parent = target.parentFile ?: error("Pasta de destino indisponível.")
    require(parent.exists() && parent.isDirectory) { "A pasta de destino não existe." }
    require(!target.isDirectory) { "O destino é uma pasta, não um arquivo." }

    val normalized = normalizeLineEndings(text, preferredLineEnding)
    val temp = File(parent, ".${target.name}.exploradorxp-${UUID.randomUUID()}.tmp")
    try {
        FileOutputStream(temp).use { output ->
            if (encoding.bom.isNotEmpty()) output.write(encoding.bom)
            output.write(normalized.toByteArray(encoding.charset))
            output.flush()
            output.fd.sync()
        }

        val verifyBytes = Files.readAllBytes(temp.toPath())
        val decoded = decodeText(verifyBytes, encoding, tolerateTruncated = false)
        check(decoded == normalized) { "A validação do arquivo temporário falhou; o original foi preservado." }

        val originalReadable = target.exists() && target.canRead()
        val originalWritable = target.exists() && target.canWrite()
        val originalExecutable = target.exists() && target.canExecute()
        try {
            Files.move(
                temp.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            val backup = if (target.exists()) File(parent, ".${target.name}.exploradorxp-backup-${UUID.randomUUID()}") else null
            try {
                if (backup != null) Files.move(target.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING)
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                backup?.delete()
            } catch (moveError: Throwable) {
                if (backup != null && backup.exists()) {
                    runCatching {
                        if (target.exists()) Files.delete(target.toPath())
                        Files.move(backup.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
                throw moveError
            } finally {
                if (backup != null && backup.exists() && target.exists()) backup.delete()
            }
        }
        if (target.exists()) {
            if (originalReadable) target.setReadable(true, true)
            if (originalWritable) target.setWritable(true, true)
            if (originalExecutable) target.setExecutable(true, true)
        }
    } finally {
        if (temp.exists()) temp.delete()
    }
}

private fun isSafeFileName(value: String): Boolean {
    val name = value.trim()
    return name.isNotEmpty() && name != "." && name != ".." && '/' !in name && '\u0000' !in name
}
