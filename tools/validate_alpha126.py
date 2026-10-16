#!/usr/bin/env python3
"""Small structural regression check. Does not replace Android build or UI tests."""
import json
from pathlib import Path
root = Path(__file__).resolve().parents[1]
assert (root/'VERSION').read_text().strip() == '0.1.0-alpha.126'
gradle = (root/'app/build.gradle.kts').read_text()
assert 'versionCode = 126' in gradle and 'versionName = "0.1.0-alpha.126"' in gradle
for meta in ('github-manager.json', 'app_identity.json'):
    data = json.loads((root/meta).read_text())
    assert data.get('versionCode') == 126 and data.get('versionName', data.get('version')) == '0.1.0-alpha.126'
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
assert 'Salvar ZIP' in news
zip_ui=(root/'app/src/main/java/com/exploradorxp/app/ArchiveViewer.kt').read_text()
assert 'ArchivePrimaryActionButton(' in zip_ui
assert '"Salvar ZIP"' in zip_ui
assert 'ActivityResultContracts.CreateDocument("application/zip")' in zip_ui
assert 'ArchiveZipCopy.copy(input, output, expected)' in zip_ui
zip_logic=(root/'app/src/main/java/com/exploradorxp/app/ArchiveZipCopy.kt').read_text()
assert 'copied != expectedBytes' in zip_logic and 'output.flush()' in zip_logic
zip_tests=(root/'app/src/test/java/com/exploradorxp/app/ArchiveZipCopyTest.kt').read_text()
assert 'preservesBytesWithoutRecompressing' in zip_tests and 'rejectsTruncatedInput' in zip_tests
print('OK: alpha.126 chips, navigation, ZIP copy, version metadata, release notes')

quicktest=(root/'app/src/test/java/com/exploradorxp/app/ExplorerQuickAccessTest.kt').read_text()
assert 'bottomBarKeepsSevenShortcutsWithHomeCentered' in quicktest
expected=['PHOTOS','VIDEOS','DOWNLOADS','HOME','NETWORK','TRASH','TOOLS']
start=quicktest.index('assertEquals(listOf(')
end=quicktest.index('), ExplorerQuickDestination.values()',start)
import re
assert re.findall(r'ExplorerQuickDestination\.([A-Z_]+),', quicktest[start:end]) == expected
assert quicktest[start:end].count('ExplorerQuickDestination.') == 7
