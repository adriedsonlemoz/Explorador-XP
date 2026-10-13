"""Evita o erro de Android Lint ProduceStateDoesNotAssignValue do workflow 112.

Este teste estático não substitui a execução de :app:lintDebug, mas sinaliza a
reintrodução da construção problemática antes do Gradle.
"""
from pathlib import Path
import re

root = Path(__file__).resolve().parent.parent
viewer = (root / "app/src/main/java/com/exploradorxp/app/AudioPlayerViewer.kt").read_text()
assert "produceState" not in re.sub(r"//[^\n]*", "", viewer), "Player voltou a usar produceState"
for label, pattern in {
    "queue_state": r"var queue by remember\(file\.absolutePath\) \{ mutableStateOf<List<File>\?>\(null\) \}",
    "queue_async": r"LaunchedEffect\(file\.absolutePath\) \{\s*queue = withContext\(Dispatchers\.IO\)",
    "tags_state": r"var tags by remember\(currentFile\.absolutePath\) \{ mutableStateOf<AudioTags\?>\(null\) \}",
    "tags_async": r"LaunchedEffect\(currentFile\.absolutePath\) \{\s*tags = withContext\(Dispatchers\.IO\)",
}.items():
    assert re.search(pattern, viewer), f"Contrato ausente: {label}"
assert "AudioQueueRules.buildQueue(file)" in viewer
assert "extractAudioTags(currentFile)" in viewer
print("Audio Compose state: checked (not a substitute for Android Lint)")
