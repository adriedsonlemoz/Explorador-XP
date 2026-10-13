"""Contratos básicos da correção do workflow 112/alpha.121."""
import json
from pathlib import Path

root = Path(__file__).resolve().parent.parent
sources = root/'app/src/main/java/com/exploradorxp/app'
viewer = (sources/'AudioPlayerViewer.kt').read_text()
notes = (sources/'ReleaseNotes.kt').read_text()
gradle = (root/'app/build.gradle.kts').read_text()
workflow = (root/'.github/workflows/gerar-apk.yml').read_text()
assert 'versionCode = 121' in gradle and 'versionName = "0.1.0-alpha.121"' in gradle
for name in ['VERSION','README.md','RELEASE.md','github-manager.json','app_identity.json']:
    assert '0.1.0-alpha.121' in (root/name).read_text(), name
for name in ['github-manager.json','app_identity.json']:
    assert json.loads((root/name).read_text())['versionCode'] == 121
assert json.loads((root/'docs/VALIDATION.json').read_text())['version_code'] == 121
assert 'Android Lint' in notes and 'fila' in notes and 'verificação preventiva' in notes
assert 'MediaController' in viewer and 'player.setMediaItems(items, startIndex, 0L)' in viewer
assert 'onMediaItemTransition' in viewer and 'extractAudioTags(currentFile)' in viewer
assert 'python3 tools/validate_audio_compose_state.py' in workflow
assert 'python3 tools/validate_alpha121.py' in workflow
assert 'python3 tools/validate_alpha120.py' not in workflow
assert 'gradle :app:lintDebug' in workflow
print('alpha121: version, audio and workflow contracts valid (not Android compilation)')
