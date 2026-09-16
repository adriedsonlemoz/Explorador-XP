# Explorador XP — Alpha 16

## Motivo da release

O workflow **Gerar APK 15** parou em `:app:lintDebug` antes de `assemblePerformance`. O único erro fatal foi `ProduceStateDoesNotAssignValue` no `PropertiesDialog`.

## Correção aplicada

A consulta de propriedades continua em `Dispatchers.IO`. Em vez de atribuir diretamente `value = withContext(...)`, o carregamento é concluído em `loadedInfo` e, em seguida, `value = loadedInfo` é executado explicitamente dentro do produtor. Isso preserva a otimização de I/O da alpha.15 e torna a atualização do estado inequívoca para o Android Lint.

Nenhum `lint-baseline.xml`, `@SuppressLint` ou desativação da regra foi adicionado.

## CI

O fluxo continua na ordem:

1. `testDebugUnitTest`;
2. `lintDebug`;
3. `assemblePerformance`;
4. publicação do APK somente quando as etapas anteriores passam.

As actions de checkout, Java e Gradle também foram atualizadas para a geração v6 para evitar o runtime Node 20 sinalizado pelo runner.

## Próxima validação

Executar o workflow da alpha.16. Se testes e lint passarem, validar no aparelho o APK `performance`, especialmente scroll em pastas grandes, troca de ordenação, busca e Voltar/Avançar.
