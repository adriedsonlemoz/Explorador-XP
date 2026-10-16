# Alpha.131: correção de compilação do workflow Gerar APK 122

## Falha comprovada nos logs

- Etapa: `:app:compileDebugKotlin`.
- Arquivo: `app/src/main/java/com/exploradorxp/app/ArchiveViewer.kt`.
- Erros no runner: linha 656 (`Unresolved reference rememberLauncherForActivityResult`) e 657 (`Unresolved reference ActivityResultContracts`).
- A falha aconteceu antes de rodar testes unitários, Lint e assemblePerformance; o APK não foi gerado.

## Correção

- Adicionados imports `androidx.activity.compose.rememberLauncherForActivityResult` e `androidx.activity.result.contract.ActivityResultContracts`.
- O projeto já inclui `androidx.activity:activity-compose:1.10.0`; sem dependências novas.
- `tools/validate_compose_imports.py` passou a verificar ambos os símbolos diretamente quando usados, no mesmo padrão dos verificadores anteriores de extensões Compose.
- Não houve alterações funcionais em extração ZIP, notificação, salvamento automático em `Armazenamento interno/XP/`, APK ou editor.

## Confirmação e pendências

- Verificar os dois imports presentes, o resultado de `tools/validate_compose_imports.py`, `tools/validate_source.py`, metadados da alpha.131, arquivos JSON/XML e ZIP íntegro.
- Pendente no ambiente local: executar `:app:testDebugUnitTest`, `:app:lintDebug` e `:app:assemblePerformance` em um SDK Android/Gradle funcional, além de teste físico do fluxo de notificação e salvamento.
