#!/usr/bin/env python3
"""Small structural regression check. Does not replace Android build or UI tests."""
import json
from pathlib import Path
root = Path(__file__).resolve().parents[1]
version = (root/'VERSION').read_text(encoding='utf-8').strip()
m = __import__('re').fullmatch(r'0\.1\.0-alpha\.(\d+)', version)
assert m, f'VERSION com formato inválido: {version}'
version_code = int(m.group(1))
gradle = (root/'app/build.gradle.kts').read_text(encoding='utf-8')
assert f'versionCode = {version_code}' in gradle and f'versionName = "{version}"' in gradle
for meta in ('github-manager.json', 'app_identity.json'):
    data = json.loads((root/meta).read_text(encoding='utf-8'))
    assert data.get('versionCode') == version_code
    assert data.get('versionName', data.get('version')) == version
    if meta == 'app_identity.json':
        assert data['android']['versionName'] == version
        assert data['android']['versionCode'] == version_code
validation = json.loads((root/'docs/VALIDATION.json').read_text(encoding='utf-8'))
assert validation['version_build'] == version and validation['version_code'] == version_code
assert validation['manager_version'] == version
assert (root/'README.md').read_text(encoding='utf-8').startswith(f'> **Versão atual:** {version} (`versionCode {version_code}`)')
assert (root/'CHANGELOG.md').read_text(encoding='utf-8').startswith(f'## {version} ')
assert (root/'RELEASE.md').read_text(encoding='utf-8').startswith(f'# Explorador XP {version}\n')
src = (root/'app/src/main/java/com/exploradorxp/app/InstalledAppsManager.kt').read_text()
assert src.count('AppManagerChip("') == 8
for name in ('Todos','Usuário','Sistema','Nome','Maiores','Menores','Instalados','Atualizados'):
    assert f'AppManagerChip("{name}"' in src
assert src.count('horizontalScroll(rememberScrollState())') == 1
assert 'horizontalArrangement = Arrangement.spacedBy(4.dp)' in src
assert 'imageVector = icon' in src and 'stateDescription = if (selected)' in src
assert 'Modifier.size(14.dp)' in src
nav=(root/'app/src/main/java/com/exploradorxp/app/ExplorerQuickAccess.kt').read_text()
assert 'ExplorerQuickDestination.HOME, "Início"' in nav
screen=(root/'app/src/main/java/com/exploradorxp/app/ExplorerScreen.kt').read_text()
assert 'ExplorerQuickDestination.HOME -> {' in screen
assert 'onTabChange(ExplorerTab.FILES)' in screen and 'onHome()' in screen
news=(root/'app/src/main/java/com/exploradorxp/app/ReleaseNotes.kt').read_text()
assert 'changes = listOf(' in news and 'BuildConfig.VERSION_NAME' in news and 'BuildConfig.VERSION_CODE' in news
zip_ui=(root/'app/src/main/java/com/exploradorxp/app/ArchiveViewer.kt').read_text()
assert 'ArchiveCompactActionButton(' in zip_ui
assert 'else "Salvar"' in zip_ui
assert 'ActivityResultContracts.CreateDocument("application/zip")' in zip_ui
assert 'ArchiveZipCopy.copy(input, output, expected)' in zip_ui
zip_logic=(root/'app/src/main/java/com/exploradorxp/app/ArchiveZipCopy.kt').read_text()
assert 'copied != expectedBytes' in zip_logic and 'output.flush()' in zip_logic
zip_tests=(root/'app/src/test/java/com/exploradorxp/app/ArchiveZipCopyTest.kt').read_text()
assert 'preservesBytesWithoutRecompressing' in zip_tests and 'rejectsTruncatedInput' in zip_tests
print(f'OK: {version} chips, navigation, ZIP UI, editor, version metadata, release notes')

quicktest=(root/'app/src/test/java/com/exploradorxp/app/ExplorerQuickAccessTest.kt').read_text()
assert 'bottomBarKeepsSevenShortcutsWithHomeCentered' in quicktest
expected=['PHOTOS','VIDEOS','DOWNLOADS','HOME','NETWORK','TRASH','TOOLS']
start=quicktest.index('assertEquals(listOf(')
end=quicktest.index('), ExplorerQuickDestination.values()',start)
import re
assert re.findall(r'ExplorerQuickDestination\.([A-Z_]+),', quicktest[start:end]) == expected
assert quicktest[start:end].count('ExplorerQuickDestination.') == 7

# Regressões de interface: ZIP e editor
assert zip_ui.count('ArchiveCompactActionButton(') == 4  # três usos + declaração
assert 'ArchivePrimaryActionButton(' not in zip_ui and 'ArchiveSecondaryActionButton(' not in zip_ui
assert 'height(49.dp)' in zip_ui
assert 'ArchiveMenuText("Informações")' in zip_ui  # Acesso pelo menu preservado.
assert 'label = "Info"' not in zip_ui
assert all(f'label = "{label}"' in zip_ui for label in ('Verificar',))
apk_ui = (root/'app/src/main/java/com/exploradorxp/app/InternalViewer.kt').read_text()
apk_support = (root/'app/src/main/java/com/exploradorxp/app/ApkInspectorSupport.kt').read_text()
assert '"Instalada: ${apkVersionLabel(info.installedVersion, info.installedVersionCode)}"' in apk_ui
assert '"No APK: ${apkVersionLabel(info.versionName, info.versionCode)}"' in apk_ui
assert apk_ui.count('ApkCompactActionButton(') == 5  # Quatro ações + declaração.
assert 'label = "Desinstalar"' in apk_ui and 'label = "Gerenciar"' in apk_ui
assert 'showDowngradeConfirm = true' in apk_ui and 'showUninstallConfirm = true' in apk_ui
assert 'apkVersionLabel(versionName: String?, versionCode: Long?)' in apk_support
assert 'visibleVersionLabelHandlesMissingVersionName' in (root/'app/src/test/java/com/exploradorxp/app/ApkInspectorSupportTest.kt').read_text()
editor = (root/'app/src/main/java/com/exploradorxp/app/TextCodeEditorViewer.kt').read_text()
assert 'EditorClipboardBar(' in editor and 'EditorClipboardAction(' in editor
assert editor.count('EditorClipboardAction(') == 6
for label in ('"Selec."', '"Copiar"', '"Colar"', '"Cortar"', '"Tudo"'):
    assert label in editor
assert 'enabled = hasSelection' in editor
assert 'enabled = editable && hasSelection' in editor
assert 'EditorSelectionRules.wordRange(source, selectionStart)' in editor
assert (root/'app/src/test/java/com/exploradorxp/app/EditorSelectionRulesTest.kt').exists()
print('OK: ZIP compacto e edição rápida no rodapé')
