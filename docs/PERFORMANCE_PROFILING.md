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

Desde a alpha.21, o workflow `Gerar APK` **não** inicia mais o dispositivo gerenciado nem regenera o perfil em toda compilação. Ele valida que `app/src/main/baseline-prof.txt` existe e segue diretamente para `assemblePerformance`, mantendo R8 e `shrinkResources`.

A regeneração do perfil e os Macrobenchmarks ficam no workflow separado **Desempenho e Baseline Profile**. Isso preserva a otimização embarcada sem adicionar cerca de 8–9 minutos a cada APK comum. O build 20 mediu 8m38s apenas na geração do perfil.


## Dependências de instrumentação do módulo produtor

Desde a alpha.20, `:baselineprofile` declara explicitamente `androidx.test:runner:1.7.0` e `androidx.test:rules:1.7.0`. Isso é necessário porque os testes de geração/benchmark usam `androidx.test.filters.LargeTest`; não devemos depender de esse pacote chegar de forma transitiva por outra biblioteca.

## Otimização de ícones — alpha.23

Os ícones de arquivos/pastas usados no caminho de rolagem deixaram de depender da primeira decodificação síncrona de `painterResource()` no item. A lista e a grade usam `CachedResourceIcon`, que decodifica PNGs em `Dispatchers.IO`, limita a duas decodificações simultâneas e guarda bitmaps em um LRU de 6 MiB.

Os 150 PNGs comuns também foram convertidos de 256×256 `nodpi` para 192×192 em `drawable-xxxhdpi`. Assim, o Android pode aplicar density scaling durante a decodificação em aparelhos mdpi/hdpi/xhdpi/xxhdpi, em vez de manter sempre o bitmap bruto de 256×256. Variantes grandes separadas são usadas nas poucas telas que precisam de 72–86 dp.

A validação recomendada é comparar o `FrameTimingMetric` da alpha.23 com a linha anterior usando a mesma pasta de benchmark e confirmar em aparelho físico se desaparecem os engasgos quando novos tipos de arquivo entram na viewport.
