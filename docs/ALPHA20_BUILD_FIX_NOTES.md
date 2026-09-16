# Explorador XP — alpha.20

## Motivo

O `Gerar APK 19` passou por testes JVM e Android Lint e chegou pela primeira vez à etapa de geração do Baseline Profile. A compilação de `:baselineprofile` falhou porque `BaselineProfileGenerator.kt` e `ExplorerMacrobenchmark.kt` usam `androidx.test.filters.LargeTest`, mas o módulo não declarava explicitamente os artefatos AndroidX Test que fornecem runner/filtros.

## Correção

Foram adicionados ao módulo `:baselineprofile`:

- `androidx.test:runner:1.7.0`
- `androidx.test:rules:1.7.0`

Foram mantidos:

- `androidx.test.ext:junit:1.3.0`
- `androidx.benchmark:benchmark-macro-junit4:1.5.0`
- `androidx.test.uiautomator:uiautomator:2.4.0`

A correção é restrita à infraestrutura de testes de desempenho e não muda interface, navegação ou operações de arquivos.

## Evidência do CI anterior

- `:app:testDebugUnitTest`: **PASSOU**
- `:app:lintDebug`: **PASSOU**
- `:baselineprofile:compileNonMinifiedReleaseKotlin`: falhou antes da correção com `Unresolved reference 'filters'` / `LargeTest`
- `assemblePerformance`: não alcançado

## Próxima validação

A alpha.20 deve avançar para o Managed Virtual Device, coletar o Baseline/Startup Profile e então executar o APK `performance`.
