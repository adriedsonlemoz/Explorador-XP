#!/usr/bin/env python3
"""Small structural regression check. Does not replace Android build or UI tests."""
import json
from pathlib import Path
root = Path(__file__).resolve().parents[1]
assert (root/'VERSION').read_text().strip() == '0.1.0-alpha.124'
gradle = (root/'app/build.gradle.kts').read_text()
assert 'versionCode = 124' in gradle and 'versionName = "0.1.0-alpha.124"' in gradle
for meta in ('github-manager.json', 'app_identity.json'):
    data = json.loads((root/meta).read_text())
    assert data.get('versionCode') == 124 and data.get('versionName', data.get('version')) == '0.1.0-alpha.124'
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
assert 'chips compactos com ícones' in news
print('OK: alpha.124 chips, navigation, version metadata, release notes')
