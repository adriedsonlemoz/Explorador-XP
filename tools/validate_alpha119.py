"""Contratos básicos da integração de áudio alpha.119; executar antes do Gradle."""
import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent.parent
src = root / 'app/src/main/java/com/exploradorxp/app'
get = lambda name: (src/name).read_text()
manifest = ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
android = '{http://schemas.android.com/apk/res/android}'
services = [e for e in manifest.findall('.//service')]
media_services = [e for e in services if e.attrib.get(android+'name')=='.MusicPlaybackService']
assert len(media_services)==1, 'Expected one MediaSessionService'
service = media_services[0]
assert service.attrib.get(android+'foregroundServiceType') == 'mediaPlayback'
assert service.find('intent-filter/action').attrib[android+'name']=='androidx.media3.session.MediaSessionService'
permissions = {e.attrib.get(android+'name') for e in manifest.findall('uses-permission')}
assert {'android.permission.FOREGROUND_SERVICE','android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK'} <= permissions

gradle = (root/'app/build.gradle.kts').read_text()
assert 'implementation("androidx.media3:media3-session:1.9.4")' in gradle
assert 'versionCode = 119' in gradle and 'versionName = "0.1.0-alpha.119"' in gradle
service_src = get('MusicPlaybackService.kt')
viewer = get('AudioPlayerViewer.kt')
main = get('MainActivity.kt')
connector = get('MusicPlaybackConnection.kt')
internal = get('InternalViewer.kt')
assert 'MediaSessionService()' in service_src and 'MediaSession.Builder' in service_src
assert 'ExoPlayer.Builder' in service_src and 'session?.release()' in service_src and 'audioPlayer?.release()' in service_src
assert 'onGetSession' in service_src and 'setSessionActivity(pendingIntent)' in service_src
assert 'ExoPlayer.Builder' not in viewer and 'player.release()' not in viewer
assert 'player.setMediaItems(items, startIndex, 0L)' in viewer
assert 'if (currentUris != newUris)' in viewer and 'onTrackChanged(tracks[index])' in viewer
assert 'onIsPlayingChanged' in connector and 'onMediaItemTransition' in connector
assert 'MediaController.Builder' in connector and 'MediaController.releaseFuture(future)' in connector
assert 'MusicMiniPlayer(' in main and 'AudioPlayerViewer(' in internal
assert 'onAudioRequested()' in internal and 'onAudioRequested = { musicActivated = true }' in main
assert 'musicItemFile(musicController?.currentMediaItem)' in main
assert 'ACTION_OPEN_MUSIC_PLAYER' in main and 'ACTION_OPEN_MUSIC_PLAYER' in service_src
assert 'setOf("mp3", "wav", "m4a", "aac", "ogg", "flac", "opus")' in main
assert 'LaunchedEffect(musicOpenRequest, musicController)' in main
assert 'player.pause()' not in viewer.split('DisposableEffect(player, tracks) {')[1].split('LaunchedEffect(player)')[0], 'must not pause on ON_STOP'
notes = get('ReleaseNotes.kt')
assert all(x in notes for x in ('Mini-player','MediaSessionService','Mesmo player'))
for name in ('VERSION','README.md','RELEASE.md','github-manager.json','app_identity.json'):
    assert '0.1.0-alpha.119' in (root/name).read_text(), name
for name in ('github-manager.json','app_identity.json'):
    meta=json.loads((root/name).read_text())
    assert meta['versionCode']==119, name
validation=json.loads((root/'docs/VALIDATION.json').read_text())
assert validation['version_code']==119
workflow=(root/'.github/workflows/gerar-apk.yml').read_text()
assert 'python3 tools/validate_alpha119.py' in workflow
assert 'python3 tools/validate_alpha118.py' not in workflow
print('alpha119 contracts: 29 checks passed (static, not Android compilation)')
