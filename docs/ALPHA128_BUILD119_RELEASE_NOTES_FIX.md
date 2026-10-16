# Alpha.128: correção do build 119 (2026-10-16)

**Entrada de diagnóstico:** `Explorador-XP-Gerar-APK-119-logs.zip`, etapa `build/8_Testar.txt`.

## Diagnóstico

- `:app:compileDebugKotlin` e a compilação dos testes Kotlin concluíram sem falhas de compilação.
- `:app:testDebugUnitTest`: 168 testes executados, 1 falha em `ReleaseNotesScopeTest.updateScreenShowsOnlyChangesFromTheCurrentBuild`, na linha 14, causando `BUILD FAILED`.
- O teste esperava `Salvar ZIP`, `duas linhas`, `streaming`, notas da alpha.126, enquanto o conteúdo de `ReleaseNotes.current` na alpha.127 já havia mudado para as alterações ZIP/editor dessa versão.

## Correção

1. Alterado `ReleaseNotesScopeTest.kt` para verificar versionName/versionCode do BuildConfig, título, limites, itens vazios, duplicados e referências a histórico, em vez de fixar textos de outra versão.
2. `ReleaseNotes.kt` anuncia apenas a correção desta versão (alpha.128).
3. Novo `tools/validate_source.py` reutiliza as verificações de interface anteriores e resolve o número de versão a partir do arquivo `VERSION`, confrontando Gradle, metadados JSON e documentação. Atualizado `.github/workflows/gerar-apk.yml`.
4. Sincronizados Gradle, VERSION, README, CHANGELOG, RELEASE, `docs/VALIDATION.json`, `app_identity.json`, `github-manager.json`.

## Limitações

Não há ambiente local completo Gradle/Android disponível. Reexecutar workflow para comprovar aprovação dos 168 testes, lint e geração do APK performance assinado. Testes no celular pendentes.
