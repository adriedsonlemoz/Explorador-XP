package com.exploradorxp.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import java.io.File

private const val EXTRA_PERFORMANCE_START_PATH = "com.exploradorxp.app.extra.PERFORMANCE_START_PATH"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = XpBlue,
                    secondary = XpBlueDark,
                    background = XpBackground,
                    surface = XpSurface,
                )
            ) {
                // PaddingValues preserva os insets para janelas Dialog filhas; safeDrawingPadding
                // aqui consumia os insets e fazia modais edge-to-edge perderem a margem inferior.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(XpBlueDark)
                        .padding(WindowInsets.safeDrawing.asPaddingValues())
                ) {
                    ExplorerApp(
                        modifier = Modifier.fillMaxSize(),
                        initialDirectoryPath = intent.getStringExtra(EXTRA_PERFORMANCE_START_PATH),
                    )
                }
            }
        }
    }

}

@Composable
private fun ExplorerApp(
    modifier: Modifier = Modifier,
    initialDirectoryPath: String? = null,
    viewModel: ExplorerViewModel = viewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var accessGranted by remember { mutableStateOf(hasFileAccess(context)) }
    var viewerFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var initialDirectoryHandled by remember(initialDirectoryPath) { mutableStateOf(false) }

    val allFilesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        accessGranted = hasFileAccess(context)
        if (accessGranted) {
            viewModel.refresh()
            viewModel.loadTrash()
        }
    }
    val legacyPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        accessGranted = hasFileAccess(context)
        if (accessGranted) {
            viewModel.refresh()
            viewModel.loadTrash()
        }
    }

    fun requestFileAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appIntent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            runCatching { allFilesLauncher.launch(appIntent) }
                .onFailure { allFilesLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
        } else {
            legacyPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ExplorerEvent.OpenFile -> {
                    if (supportsInternalViewer(event.file)) viewerFilePath = event.file.absolutePath
                    else openFile(context, event.file)
                }
                is ExplorerEvent.ShareFiles -> shareFiles(context, event.files)
                is ExplorerEvent.ShowMessage -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Entrada opcional usada somente pelas rotinas de desempenho. Não muda o fluxo normal.
    LaunchedEffect(accessGranted, initialDirectoryPath) {
        if (accessGranted && !initialDirectoryHandled && !initialDirectoryPath.isNullOrBlank()) {
            initialDirectoryHandled = true
            viewModel.navigateTo(File(initialDirectoryPath))
        }
    }

    // Permite que Macrobenchmark diferencie primeiro frame de conteúdo realmente pronto.
    // Quando a automação fornece uma pasta-alvo, só sinaliza Fully Drawn depois que
    // essa pasta específica terminou de carregar — nunca durante o refresh inicial da raiz.
    val performanceTargetPath = remember(initialDirectoryPath) {
        initialDirectoryPath?.takeIf { it.isNotBlank() }?.let { File(it).absolutePath }
    }
    ReportDrawnWhen {
        !accessGranted || if (performanceTargetPath == null) {
            !state.loading
        } else {
            initialDirectoryHandled &&
                state.currentDir.absolutePath == performanceTargetPath &&
                !state.loading
        }
    }

    if (!accessGranted) {
        PermissionAccessDialog(onRequestAccess = ::requestFileAccess)
        return
    }

    val activeViewer = viewerFilePath?.let(::File)
    if (activeViewer != null) {
        InternalViewerScreen(
            file = activeViewer,
            onClose = {
                viewerFilePath = null
                viewModel.refresh()
            },
            onOpenExternal = { target -> openFile(context, target) },
            onOpenFolder = { target ->
                viewerFilePath = null
                viewModel.navigateTo(target)
            },
        )
        return
    }

    ExplorerScreen(
        state = state,
        accessGranted = accessGranted,
        onRequestAccess = ::requestFileAccess,
        onBack = viewModel::goBack,
        onForward = viewModel::goForward,
        onHome = viewModel::goHome,
        onUp = viewModel::goUp,
        onRefresh = viewModel::refresh,
        onItemClick = viewModel::onItemClick,
        onNavigateTo = viewModel::navigateTo,
        onToggleSearch = { viewModel.setSearchVisible(!state.searchVisible) },
        onQueryChange = viewModel::setQuery,
        onToggleView = viewModel::toggleViewMode,
        onToggleHidden = viewModel::setShowHidden,
        onSortMode = viewModel::setSortMode,
        onFoldersFirstChange = viewModel::setFoldersFirst,
        onTabChange = viewModel::setTab,
        onCopy = viewModel::copySelected,
        onCut = viewModel::cutSelected,
        onCopyTarget = viewModel::copyFile,
        onCutTarget = viewModel::cutFile,
        onPaste = viewModel::pasteClipboard,
        onClearClipboard = viewModel::clearClipboard,
        onDelete = viewModel::deleteSelected,
        onDeleteTarget = viewModel::deleteFile,
        onMoveToTrash = viewModel::moveSelectedToTrash,
        onMoveTargetToTrash = viewModel::moveFileToTrash,
        onShare = viewModel::shareSelected,
        onShareTarget = viewModel::shareFile,
        onOpenTarget = viewModel::openFileOrFolder,
        onOpenExternalTarget = { target -> openFile(context, target) },
        onSelectAll = viewModel::selectAllVisible,
        onClearSelection = viewModel::clearSelection,
        onSelectOnly = viewModel::selectOnly,
        onToggleSelection = viewModel::toggleSelection,
        onToggleFavorite = viewModel::toggleFavorite,
        onCreateFolder = viewModel::createFolder,
        onCreateFile = viewModel::createFile,
        onRename = viewModel::rename,
        onLoadTrash = viewModel::loadTrash,
        onRestoreTrashItem = viewModel::restoreTrashItem,
        onDeleteTrashItem = viewModel::permanentlyDeleteTrashItem,
        onEmptyTrash = viewModel::emptyTrash,
        onAnalyzeStorage = viewModel::analyzeStorage,
        onCancelStorageAnalysis = viewModel::cancelStorageAnalysis,
        onCancelTransfer = viewModel::cancelTransfer,
        modifier = modifier,
    )
}

@Composable
private fun PermissionAccessDialog(onRequestAccess: () -> Unit) {
    XpDialogFrame(
        title = "Acesso aos arquivos",
        onDismiss = { /* Acesso é necessário para usar o gerenciador. */ },
    ) {
        Text(
            "O Explorador XP precisa de acesso aos arquivos para mostrar, abrir, copiar, mover e organizar o armazenamento. " +
                "Toque em Liberar acesso e ative a permissão na próxima tela.",
            color = androidx.compose.ui.graphics.Color(0xFF303030),
        )
        Spacer(Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth(),
        ) {
            XpDialogButton("Liberar acesso", onClick = onRequestAccess)
        }
    }
}

fun hasFileAccess(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

private fun openFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val mime = mimeTypeFor(file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Abrir com"))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Nenhum aplicativo compatível encontrado.", Toast.LENGTH_SHORT).show()
    }
}

private fun shareFiles(context: Context, files: List<File>) {
    val uris = ArrayList(files.map { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it) })
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = mimeTypeFor(files.first())
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
    }.apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar"))
}

private fun mimeTypeFor(file: File): String {
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "application/octet-stream"
}
