#!/usr/bin/env python3
"""Contrato de integração alpha.117 (não substitui o Android Gradle build)."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
src=r/'app/src/main/java/com/exploradorxp/app'
texts={p.name:p.read_text(encoding='utf-8') for p in src.glob('*.kt')}
checks={
    'common_menu':all('XpExplorerMenuBar {' in texts[p] for p in ['ExplorerScreen.kt','ExplorerQuickAccess.kt']),
    'common_navigation': 'XpNavigationButtons(XpNavigationCommands' in texts['ExplorerScreen.kt'] and 'XpExplorerNavigationBar(XpNavigationCommands' in texts['ExplorerQuickAccess.kt'],
    'common_tools_help': all(all(f in texts[p] for f in ['XpExplorerToolsMenu(', 'XpExplorerHelpMenu(']) for p in ['ExplorerScreen.kt','ExplorerQuickAccess.kt']),
    'gallery_operations': all(f in texts['ExplorerQuickAccess.kt'] for f in ['onCopyFiles(', 'onCutFiles(', 'onMoveToTrash(files)', 'onCompressFiles(files)', 'onPasteInto(File(it))']),
    'gallery_confirms_trash': 'AlertDialog(onDismissRequest = { confirmTrash = null }' in texts['ExplorerQuickAccess.kt'],
    'transfer_dialogs_visible': 'TransferProgressHost(transferState' in texts['ExplorerScreen.kt'] and 'TransferConflictHost(transferConflict' in texts['ExplorerScreen.kt'],
    'explicit_paste_destination':'fun pasteClipboardTo(destination: File)' in texts['ExplorerViewModel.kt'],
    'rename_shared': 'RenameFileDialog(file = target' in texts['ExplorerScreen.kt'] and 'RenameFileDialog(file, onDismiss' in texts['ExplorerQuickAccess.kt'],
    'rename_callback':'onRenameFile(file, newName) { reloadGallery() }' in texts['ExplorerQuickAccess.kt'],
    'rename_validation':'FileRenameRules.validationError(newName)' in texts['FileRepository.kt'],
    'root_create_disabled':'canNew = folderPath != null' in texts['ExplorerQuickAccess.kt'],
}
for name, passed in checks.items():
    print(f'{"OK" if passed else "ERRO"}: {name}')
if not all(checks.values()): raise SystemExit(1)
print(f'{len(checks)} testes estáticos aprovados')
