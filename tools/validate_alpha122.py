#!/usr/bin/env python3
"""Pré-verificação estática da alpha.122. Não substitui lint/Gradle."""
from pathlib import Path
import json
r=Path(__file__).resolve().parents[1]
a=r/'app/src/main/java/com/exploradorxp/app'
read=lambda n:(a/n).read_text()
assert (r/'VERSION').read_text().strip() == '0.1.0-alpha.122'
assert 'versionCode = 122' in (r/'app/build.gradle.kts').read_text()
assert 'versionName = "0.1.0-alpha.122"' in (r/'app/build.gradle.kts').read_text()
for name in ('app_identity.json','github-manager.json'):
    assert json.loads((r/name).read_text())['versionCode']==122
assert json.loads((r/'docs/VALIDATION.json').read_text())['version_code']==122
assert 'StorageDisplaySection.CATEGORIES' in read('ExplorerScreen.kt')
assert 'StorageDisplaySection.FOLDERS' in read('ExplorerScreen.kt')
assert 'StorageDisplaySection.FILES' in read('ExplorerScreen.kt')
assert 'AppUsageSummary.from(apps, storage)' in read('ExplorerScreen.kt')
assert 'AppManagerSort.SIZE_ASC' in read('InstalledAppsManager.kt')
assert 'ext == "obb"' in read('FileTypeClassifier.kt')
assert 'FileContentDetector.isZipSignature(magic)' in read('MainActivity.kt')
assert 'mayBeUnlabeledArchive(displayName, resolvedMime)' in read('MainActivity.kt')
assert 'application/octet-stream' in (r/'app/src/main/AndroidManifest.xml').read_text()
assert 'python3 tools/validate_alpha122.py' in (r/'.github/workflows/gerar-apk.yml').read_text()
assert 'python3 tools/validate_audio_compose_state.py' in (r/'.github/workflows/gerar-apk.yml').read_text()
assert 'Abertura de arquivos ZIP sem extensão' in read('ReleaseNotes.kt')
print('alpha122: verificação estática aprovada')
