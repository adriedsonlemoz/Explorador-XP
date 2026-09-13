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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = XpBlue,
                    secondary = XpBlueDark,
                    background = XpBackground,
                    surface = XpSurface,
                )
            ) {
                ExplorerApp(modifier = Modifier)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
private fun ExplorerApp(
    modifier: Modifier = Modifier,
    viewModel: ExplorerViewModel = viewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var accessGranted by remember { mutableStateOf(hasFileAccess(context)) }

    val allFilesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        accessGranted = hasFileAccess(context)
        viewModel.refresh()
    }
    val legacyPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        accessGranted = hasFileAccess(context)
        viewModel.refresh()
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
                is ExplorerEvent.OpenFile -> openFile(context, event.file)
                is ExplorerEvent.ShareFiles -> shareFiles(context, event.files)
                is ExplorerEvent.ShowMessage -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
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
        onItemLongClick = viewModel::onItemLongClick,
        onNavigateTo = viewModel::navigateTo,
        onToggleSearch = { viewModel.setSearchVisible(!state.searchVisible) },
        onQueryChange = viewModel::setQuery,
        onToggleView = viewModel::toggleViewMode,
        onToggleHidden = viewModel::setShowHidden,
        onSortMode = viewModel::setSortMode,
        onTabChange = viewModel::setTab,
        onCopy = viewModel::copySelected,
        onCut = viewModel::cutSelected,
        onPaste = viewModel::pasteClipboard,
        onDelete = viewModel::deleteSelected,
        onShare = viewModel::shareSelected,
        onClearSelection = viewModel::clearSelection,
        onSelectOnly = viewModel::selectOnly,
        onToggleFavorite = viewModel::toggleFavorite,
        onCreateFolder = viewModel::createFolder,
        onRename = viewModel::rename,
        modifier = modifier,
    )
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
