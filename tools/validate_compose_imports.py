#!/usr/bin/env python3
"""Guard known Compose extension imports before Android compilation.

Catches the build-108 horizontalScroll and build-111 fillMaxHeight regressions.
This is a *static* preflight, not an Android compiler substitute.
"""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1] / 'app/src/main/java'
required_imports = {
    'horizontalScroll': 'androidx.compose.foundation.horizontalScroll',
    'fillMaxHeight': 'androidx.compose.foundation.layout.fillMaxHeight',
}
checked = 0
for file in root.rglob('*.kt'):
    source = file.read_text(encoding='utf-8')
    for extension, fqname in required_imports.items():
        if not re.search(rf'\.\s*{extension}\s*\(', source):
            continue
        checked += 1
        namespace = fqname.rsplit('.', 1)[0]
        direct = re.search(rf'^import\s+{re.escape(fqname)}(?:\s+as\s+\w+)?\s*$', source, re.M)
        wildcard = re.search(rf'^import\s+{re.escape(namespace)}\.\*\s*$', source, re.M)
        if not (direct or wildcard):
            raise SystemExit(f'FALHA: {file.relative_to(root)} usa {extension} sem importar {fqname}')
        print(f'OK: {file.name} importa {extension}')
print(f'Compose imports: {checked} usos verificados')
