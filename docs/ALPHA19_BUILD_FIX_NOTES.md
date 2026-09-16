# Alpha 19 — correção do lint de Propriedades

Versão: `0.1.0-alpha.19` (`versionCode 19`)

O build 18 confirmou que os testes JVM passam, mas o Android Lint continuou emitindo `ProduceStateDoesNotAssignValue` no diálogo de Propriedades, mesmo com uma atribuição explícita após `withContext`.

A implementação foi reestruturada para `remember + LaunchedEffect`, com o carregamento de metadados preservado em `Dispatchers.IO`. Assim, a UI continua sem I/O bloqueante e o lint não precisa ser desativado nem receber baseline/supressão.

Baseline Profile e Macrobenchmark da alpha.18 permanecem inalterados. O próximo ponto de validação do CI é alcançar a geração do perfil e o `assemblePerformance`.

Nenhuma mudança visual foi feita.
