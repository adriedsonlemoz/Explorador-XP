# Alpha.115 • Correção da compilação Kotlin do workflow 108

## Evidência
- Workflow **Gerar APK 108**, etapa `Testar`, falhou antes dos testes, na tarefa `:app:compileDebugKotlin`.
- Erro do compilador: `ExplorerQuickAccess.kt:508:18 Unresolved reference 'horizontalScroll'`.
- Causa confirmada no código alpha.114: `.horizontalScroll(androidx.compose.foundation.rememberScrollState())` sem import da extensão `androidx.compose.foundation.horizontalScroll`.

## Correção
- Import Kotlin adicionado, preservando a rolagem horizontal da barra com menus da galeria.
- Guard estático `tools/validate_compose_imports.py` executado na pipeline antes de `:app:testDebugUnitTest` para detectar regressão equivalente.
- `ReleaseNotes` e teste de escopo de novidades sincronizados com a alpha.115. Histórico permanece no CHANGELOG.
- Nenhum teste ou regra R8 desativado, sem alteração de permissão ou de API.

## Limitações de validação
- Teste preventivo é análise estática; compilação Android precisa ser confirmada pelo GitHub Actions.
- Testar telas Fotos/Vídeos e interação dos menus em aparelho após build.
