# Explorador XP 0.1.0-alpha.121

**Versão:** `0.1.0-alpha.121`  
**Version code:** `121`

## Correção do build 112

- Em `AudioPlayerViewer.kt`, substituídas duas construções `produceState` sinalizadas pelo Android Lint por `remember` + `mutableStateOf` e `LaunchedEffect`, mantendo a fila, metadados e capas carregados em `Dispatchers.IO`.
- Verificação preventiva de padrão de estado do player adicionada ao workflow.
- Teste de Novidades atualizado para mostrar somente as correções da alpha.121.
- Media3, mini-player, notificação, segundo plano e demais funcionalidades anteriores preservadas.

## Pendências

- Confirmar `:app:testDebugUnitTest`, `:app:lintDebug` e `:app:assemblePerformance` no GitHub Actions.
- Validar o player real no aparelho com diferentes formatos e ao trocar rapidamente as faixas.
