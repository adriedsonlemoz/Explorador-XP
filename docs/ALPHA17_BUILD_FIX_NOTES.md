# Alpha 17 — correção do build 16

## Resultado do log Gerar APK 16

O pipeline avançou pelos testes JVM e chegou ao Android Lint. A correção de `produceState` feita na alpha.16 não voltou a falhar. O bloqueio ocorreu em `lintDebug`, que contabilizou 3 erros e 51 avisos, mas o log padrão exibiu apenas o primeiro erro completo.

O primeiro erro era `NewApi` em `ExplorerViewModel.kt`: a chamada `history.removeLast()` foi resolvida como `java.util.List.removeLast()`, API 35, enquanto o Explorador XP mantém Min SDK 26.

## Correções

- `ExplorerViewModel`: `removeLast()` substituído por `removeAt(history.lastIndex)`.
- `PreferencesStore`: a ocorrência equivalente na lista de recentes também foi substituída por `removeAt(items.lastIndex)`.
- O workflow separa testes de lint.
- Se `lintDebug` falhar, o CI procura e imprime `lint-results-debug.txt` por completo antes de retornar o código de erro.
- Nenhum baseline ou supressão foi adicionado.

## Próxima validação

Executar o workflow da alpha.17. Se houver outro erro de lint, o próprio log deverá trazer todas as ocorrências; se passar, o pipeline seguirá para `assemblePerformance` com R8 e `shrinkResources`.
