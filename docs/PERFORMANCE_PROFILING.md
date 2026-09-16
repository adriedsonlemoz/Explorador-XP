# Desempenho: Macrobenchmark e Baseline Profile

## Objetivo

A alpha.18 adiciona duas camadas complementares:

1. **Baseline Profile**: pré-compila os caminhos mais importantes do Explorador XP.
2. **Macrobenchmark**: mede inicialização e fluidez de rolagem e gera dados comparáveis.

## Jornada coberta

A automação cria `/sdcard/Download/ExploradorXP-Benchmark` com 600 arquivos e 40 pastas artificiais. Isso evita depender do conteúdo pessoal do aparelho. A automação separa duas jornadas de perfil: inicialização até a primeira pasta ficar pronta (também usada no Startup Profile) e carregamento + rolagem da pasta grande (Baseline Profile normal).

## Métricas

- `StartupTimingMetric`: tempos de inicialização (incluindo TTID e, quando reportado, TTFD).
- `FrameTimingMetric`: duração dos frames e jank durante rolagem.
- Perfetto trace/JSON: emitidos pelo Macrobenchmark em `baselineprofile/build/outputs/`.

## Comparação

Os testes têm duas condições:

- `CompilationMode.None()`: sem compilação AOT do app.
- `CompilationMode.Partial(BaselineProfileMode.Require)`: instalação semelhante à experiência com Baseline Profile aplicado.

## Execução

### Gerar o perfil

```bash
gradle :app:generateBaselineProfile
```

### Macrobenchmark

Consulte primeiro a tarefa criada para o dispositivo gerenciado:

```bash
gradle :baselineprofile:tasks --all
```

No GitHub, execute manualmente **Desempenho e Baseline Profile**. O artifact `explorador-xp-performance-reports` contém o perfil, saídas e traces encontrados.

## Importante sobre emulador

O emulador é adequado para gerar o Baseline Profile e detectar regressões grosseiras/validar a automação. Para números de desempenho representativos do usuário final, execute Macrobenchmark em aparelho físico com carga/bateria estáveis.

## APK performance

O workflow `Gerar APK` executa a geração do Baseline Profile antes do `assemblePerformance`. O build final continua com R8 e `shrinkResources`.


## Dependências de instrumentação do módulo produtor

Desde a alpha.20, `:baselineprofile` declara explicitamente `androidx.test:runner:1.7.0` e `androidx.test:rules:1.7.0`. Isso é necessário porque os testes de geração/benchmark usam `androidx.test.filters.LargeTest`; não devemos depender de esse pacote chegar de forma transitiva por outra biblioteca.
