from pathlib import Path
r=Path(__file__).resolve().parents[1]
assert (r/"VERSION").read_text().strip()=="0.1.0-alpha.123"
text=(r/"app/build.gradle.kts").read_text()
assert 'versionCode = 123' in text
assert 'versionName = "0.1.0-alpha.123"' in text
