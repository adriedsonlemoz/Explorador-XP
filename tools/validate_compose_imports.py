#!/usr/bin/env python3
"""Check Compose extension imports that caused alpha.114 build 108 to fail.

Source guard, not a substitute for compiling with the Android Gradle plugin.
"""
from pathlib import Path
import re

source = Path(__file__).resolve().parents[1] / "app/src/main/java/com/exploradorxp/app/ExplorerQuickAccess.kt"
content = source.read_text(encoding="utf-8")
needed = {
    "horizontalScroll": "androidx.compose.foundation.horizontalScroll",
}
for modifier, expected_import in needed.items():
    if re.search(rf"\.\s*{modifier}\s*\(", content):
        if not re.search(rf"^import\s+{re.escape(expected_import)}\s*$", content, re.M):
            raise SystemExit(f"FALHA: {source.name} utiliza {modifier} sem importar {expected_import}")
        print(f"OK: {source.name} importa {modifier}")
print("Compose imports: verificação estática aprovada")
