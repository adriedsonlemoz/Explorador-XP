#!/usr/bin/env python3
"""Contratos principais do player alpha.118; não substitui compilação Android."""
from pathlib import Path
import json
root = Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/com/exploradorxp/app"
p = (src / "AudioPlayerViewer.kt").read_text()
i = (src / "InternalViewer.kt").read_text()
rules = (src / "AudioQueueRules.kt").read_text()
checks = {
    'Media3 player de audio': 'ExoPlayer.Builder' in p and 'setMediaItems(' in p,
    'foco de audio e fones': 'setAudioAttributes(AudioAttributes.DEFAULT, true)' in p and 'setHandleAudioBecomingNoisy(true)' in p,
    'liberacao': 'player.release()' in p and 'Lifecycle.Event.ON_STOP' in p,
    'metadados': 'MediaMetadataRetriever' in p and 'METADATA_KEY_ARTIST' in p and 'embeddedPicture' in p,
    'fila por pasta': 'selected.parentFile?.listFiles()' in rules and 'isAudio(it.name)' in rules,
    'limite de fila': 'MAX_TRACKS = 1_000' in rules,
    'controles': all(x in p for x in ('seekToNextMediaItem()', 'seekToPreviousMediaItem()', 'shuffleModeEnabled', 'repeatMode', 'Slider(')),
    'tratamento de falha': 'onPlayerError' in p and 'Tentar novamente' in p,
    'integracao visualizador': 'AudioPlayerViewer(' in i and 'MediaViewer(activeFile)' not in i,
    'VideoView removido do audio': 'VideoView(context)' not in i and 'MediaController' not in i,
    'OPUS identificado': '"opus"' in (src / 'FileIconMapper.kt').read_text() and '"opus" to "Áudio Opus"' in (src / 'FileTypeClassifier.kt').read_text(),
    'metadados de versao': all('0.1.0-alpha.118' in (root / f).read_text() for f in ('VERSION','RELEASE.md','README.md','app_identity.json','github-manager.json')),
    'versionCode': 'versionCode = 118' in (root / 'app/build.gradle.kts').read_text(),
}
assert json.loads((root/'docs/VALIDATION.json').read_text())['version_code']==118
for name, good in checks.items():
    print(('OK' if good else 'ERRO') + ': ' + name)
if not all(checks.values()): raise SystemExit(1)
print(f'{len(checks)} contratos estaticos aprovados')
