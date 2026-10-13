# Alpha.121: correção Android Lint build 112

- O log do workflow 112 confirmou sucesso em `:app:testDebugUnitTest` e falha em `:app:lintDebug`: **2 errors, 40 warnings**.
- Falhas localizadas em `AudioPlayerViewer.kt:155` e `:232` por `ProduceStateDoesNotAssignValue`.
- Solução: estado de fila e tags com `remember(key) { mutableStateOf(null) }` e atualização em `LaunchedEffect(key)` via `withContext(Dispatchers.IO)`. Os resultados assíncronos são cancelados quando a key muda e não bloqueiam o thread da UI.
- Nenhuma regra Lint foi desabilitada. A verificação preventiva não substitui o Lint do Android nem o build real.
- Sem alterações no serviço Media3, notificações, permissões, player em segundo plano ou mini-player.
- Pendente: executar novamente CI e testes físicos, com atenção a trocas rápidas de faixas e capas.
