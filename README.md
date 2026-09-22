# Explorador XP

Gerenciador de arquivos Android nativo em **Kotlin + Jetpack Compose**, inspirado no Windows XP e redesenhado para uso confortável em telas de celular.

**Versão atual:** `0.1.0-alpha.66` (`versionCode 66`)  

- A alpha.66 redesenha o **navegador de arquivos compactados** com cabeçalho informativo, métricas de tamanho/conteúdo/compressão, ações principais **Extrair** e **Abrir com**, barra inferior menos redundante e uma janela **Info** dividida em Informações gerais, Conteúdo e Origem. A tela **Dispositivo** passa a usar um resolvedor extensível de SoCs para exibir primeiro o nome comercial quando houver correspondência segura (por exemplo MT6765 → Helio P35, SM6225 → Snapdragon 680 e SDM660 → Snapdragon 660), preservando fabricante, identificador técnico, CPU, arquitetura, frequências e, quando catalogados, GPU e processo de fabricação.
- A alpha.65 adiciona **detecção de tipo pelo conteúdo** para arquivos sem extensão, com extensão desconhecida ou genérica. ZIP/APK, PDF, PNG/JPEG/GIF/BMP/WebP e texto podem ser reconhecidos pela assinatura/conteúdo e encaminhados ao visualizador interno correto. O arquivo `EditaAi-0.1.0-alpha.3`, que é um ZIP sem extensão contendo um APK, passa a abrir no navegador ZIP normalmente.
- A alpha.64 corrige o bloqueio de Lint `WrongConstant` no fluxo **Abrir com**: permissões persistentes de leitura/escrita agora são solicitadas somente com combinações explícitas aceitas pela API Android, mantendo todo o modo de arquivos grandes da alpha.63.

### Assinatura permanente dos APKs

A partir da `0.1.0-alpha.42`, o APK `performance` usa assinatura Android permanente via GitHub Actions Secrets. O workflow espera exatamente `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` e `ANDROID_KEY_PASSWORD`. O arquivo `.jks` não deve ser enviado ao repositório.

**Pacote:** `com.exploradorxp.app`  
**Min SDK:** 26  
**Target/Compile SDK:** 35

## O que já está implementado

- A alpha.63 substitui o antigo bloqueio de arquivos acima de 750 KB por um **Modo arquivo grande** paginado: o arquivo completo permanece no disco e somente uma janela de aproximadamente 384 KB fica no editor. O usuário pode editar/salvar o trecho atual, navegar para trechos anterior/próximo, usar **Ir linha**, manter quebra de linha e numeração global, e pesquisar o arquivo inteiro por streaming com progresso e cancelamento. O realce de sintaxe passa a acompanhar somente a área visível nesse modo, o histórico usa orçamento de memória menor e a gravação reconstrói prefixo/trecho/sufixo em arquivo temporário sem carregar vários MB na RAM.
- A alpha.62 torna a **quebra automática de linha** visível na barra e no menu do editor/visualizador, mantendo a preferência salva. Arquivos de texto recebidos por **Abrir com** agora podem ser editados sobre uma cópia de trabalho: quando a origem concede escrita, **Salvar original** grava de volta no `content://`; sem escrita, **Salvar como** usa o seletor de documentos do Android. Antes de sobrescrever a origem, o editor compara SHA-256 para detectar alterações feitas por outro aplicativo e evita perda silenciosa de dados.
- A alpha.61 profissionaliza o **editor interno de texto/código** sem trocar seu componente leve: amplia o destaque de sintaxe, adiciona autoindentação e fechamento automático de pares, permite escolher TAB/espaços e quebra de linha, melhora números de linha e localizar/substituir com contagem de ocorrências, preserva desfazer/refazer e reduz varreduras/cópias de texto durante a digitação. O fluxo **Abrir com** continua seguro e somente leitura, mas agora mantém metadados da origem separados para preparar um futuro “Salvar de volta”.
- A alpha.60 renova o **instalador/inspector de APK**: compara versão instalada × APK, valida compatibilidade de Android e ABI, compara assinatura SHA-256 quando há app instalado, lista permissões declaradas, mostra arquiteturas e bloqueia tentativas claramente incompatíveis antes de abrir o instalador do sistema.
- A alpha.59 evolui o **navegador ZIP**: cria um índice hierárquico único com pastas implícitas, tamanho/contagem recursivos por pasta, breadcrumb clicável, pesquisa com caminho do resultado, seleção rápida com tamanho real selecionado, barra de status do nível atual e pré-visualização com progresso/cancelamento e cache temporário reutilizável.
- A alpha.58 adiciona **Pausar / Continuar** às operações longas de copiar, mover, excluir e Lixeira. A pausa é cooperativa e acontece entre arquivos e entre blocos de 256 KiB durante cópias, sem cancelar nem reiniciar a operação; velocidade e ETA desconsideram o tempo em que a tarefa ficou pausada, e **Cancelar** continua disponível mesmo durante a pausa.
- A alpha.57 melhora as operações de arquivos com progresso por bytes, velocidade e tempo restante em cópia/movimentação, além de tratamento explícito de conflitos com **Substituir / Ignorar / Manter ambos** e opção de aplicar a decisão aos próximos conflitos. O visualizador de imagens agora permite navegar por **Anterior/Próxima** ou gesto lateral sem sair da tela, usando somente as imagens da pasta que estava aberta no Explorer e respeitando a ordenação atual.
- A alpha.56 continua a otimização de desempenho: progresso de transferências, análise de armazenamento e estado da Lixeira foram separados do `ExplorerUiState` principal para não recomporem toda a tela; itens de arquivo reutilizam um cache LRU de metadados já formatados; lista/grade informam `contentType` ao Compose; seleção na barra inferior usa índice por caminho; e a análise de armazenamento reaproveita o tamanho persistido da Lixeira em vez de revarrer árvores novas.
- A alpha.55 corrige dois problemas visuais persistentes: as janelas modais deixam de reaplicar os insets do sistema dentro do `Dialog`, ficando centralizadas de forma simétrica na área útil do aplicativo; e os recursos de **Movies, Music e Pictures/DCIM** foram reconstruídos a partir dos próprios recursos XP existentes no projeto porque os PNGs especializados antigos já continham o desenho cortado na origem. Nenhuma arte generativa ou mockup foi criado.
- A alpha.54 continua a melhoria de desempenho nas operações pesadas: copiar, mover, excluir e Lixeira agora montam um plano iterativo da árvore uma única vez e reutilizam esse snapshot para executar a operação, eliminando enumerações recursivas duplicadas com `listFiles()`. O mesmo plano fornece contagem e tamanho total, usa buffer maior na cópia e grava o tamanho da árvore no metadado da Lixeira para acelerar reaberturas futuras.
- A alpha.53 inicia a otimização estrutural de desempenho: leituras de diretórios grandes passam a responder ao cancelamento em lotes, navegações que já têm snapshot em cache adiam a releitura do armazenamento para evitar I/O inútil, estatísticas recursivas da barra inferior ganham cache curto e scanner único, e miniaturas deixam de ser decodificadas durante a rolagem. A mesma versão corrige o enquadramento dos ícones XP de pastas especiais e separa o botão **Instalar APK** do fluxo `ACTION_VIEW`, impedindo que a instalação seja recapturada pelo próprio **Abrir com** do Explorador XP.
- A alpha.52 padroniza as janelas maiores do app: **Ajuda, Sobre, Lixeira, Armazenamento e Informações do dispositivo** passam a ficar centralizadas dentro da área segura, com altura limitada, conteúdo rolável e rodapé fixo, deixando visível onde cada janela termina.
- A alpha.51 integra o Explorador XP ao **Abrir com** do Android somente para formatos que o app realmente consegue tratar. Arquivos recebidos por `content://` são validados, copiados para uma área temporária privada e abertos diretamente no leitor interno; ZIP, PDF, APK, textos/código, imagens, áudio e vídeo compatíveis podem ser recebidos sem registrar MIME coringa. Arquivos externos de texto entram em leitura segura para não editar apenas uma cópia temporária.
- A alpha.50 corrige a inicialização do **leitor/editor interno de texto e código** em aparelhos Android onde o `EditText` dispara seleção ainda durante o construtor. O callback de cursor agora é protegido até o editor terminar de configurar, eliminando o erro minificado `Function2 ... null object reference` observado ao abrir Markdown e preservando edição, números de linha, localizar/substituir e salvamento.
- A alpha.49 redesenha o **visualizador de APK** com cabeçalho em card, comparação clara entre APK e versão instalada, detalhes mais limpos e ações **Instalar/Reinstalar/Atualizar** + **Abrir app** lado a lado. O fluxo de fonte desconhecida também foi reforçado: ao voltar da tela do Android após conceder a permissão, o instalador é aberto automaticamente sem exigir um segundo toque no botão.
- A alpha.48 reorganiza a tela principal e corrige o fluxo de APK: **Atualizar** sai da toolbar e vai para **Exibir**; a barra inferior ganha mais espaço para armazenamento e passa a somar recursivamente arquivos, subpastas e tamanho da pasta atual; o visualizador APK detecta pacotes instalados com visibilidade adequada e abre diretamente o instalador do Android, orientando a permissão de fonte desconhecida quando necessário.
- A alpha.47 corrige a compilação do visualizador ZIP após o polimento visual da alpha.45: foi restaurado o import de `horizontalScroll` usado na faixa de ações **Info / Verificar / Extrair / Ordenar**, sem remover as melhorias da alpha.46.
- A alpha.46 simplifica a **tela principal**: o cartão grande de armazenamento foi removido e espaço livre/uso passaram para a barra inferior clicável; a toolbar ganhou **Atualizar** e **Novo** (pasta/arquivo), enquanto a barra de seleção expõe Renomear, Compartilhar, ZIP e Excluir diretamente, reduzindo a dependência de **Mais**.
- Interface principal baseada no Explorer clássico do Windows XP: barra de título azul, menus Arquivo/Editar/Exibir/Favoritos/Ferramentas/Ajuda, barra de ferramentas compacta, barra de endereço, indicador de armazenamento, lista/grade e barra de status inferior.
- Pacote visual XP com ícones PNG otimizados por densidade Android; os 150 ícones comuns usados na navegação ficam em `drawable-xxxhdpi`, com variantes grandes apenas onde necessário.
- A experiência vetorial da alpha.27 foi revertida na alpha.28 por preferência visual e ausência de ganho perceptível no aparelho; o app voltou ao pipeline PNG otimizado com decodificação assíncrona/cache.
- A alpha.29 aplica o polimento visual observado no vídeo de uso real: menus e diálogos XP mais consistentes, busca focada, breadcrumb clicável, grade mais espaçosa, seleção contextual, miniaturas locais de foto/vídeo e visualizadores internos padronizados.
- A alpha.30 integra uma **Lixeira real** ao fluxo de exclusão: cada remoção oferece mover para a Lixeira ou apagar permanentemente; itens podem ser restaurados, apagados individualmente ou removidos de uma vez com **Esvaziar Lixeira**.
- A alpha.31 continua o refinamento visual: menus superiores mais legíveis e roláveis em telas estreitas, barra de seleção com ações sem compressão, listagem com tipo/tamanho e data separados, janelas com botão Fechar no padrão XP e armazenamento com leitura visual mais clara.
- A alpha.45 faz o primeiro polimento do **visualizador ZIP** após o uso real: os botões do topo agora têm ícones e hierarquia visual melhor, o estado da ordenação fica visível, arquivos TXT/HTML abertos a partir do ZIP entram em pré-visualização temporária somente leitura e o editor ganhou um fallback defensivo para evitar fechamento brusco caso a inicialização falhe.
- A alpha.44 adiciona **compactação ZIP de arquivos e pastas selecionados**: no modo de seleção, **Mais > Compactar em ZIP** abre uma configuração com nome e pasta de destino, preserva a hierarquia das pastas, mostra progresso real com porcentagem/bytes/velocidade, permite cancelar com remoção do arquivo parcial, evita sobrescrita silenciosa criando nome livre e oferece **Abrir ZIP** ou **Abrir pasta** ao terminar.
- A alpha.43 reconstrói o **visualizador e extrator ZIP**: navegação por pastas internas, pesquisa, ordenação, seleção e extração parcial, visualização de itens sem descompactar o pacote inteiro, escolha de destino com memória do último local, progresso real com porcentagem/bytes/velocidade, cancelamento, conflitos (renomear/substituir/ignorar), abrir pasta ao concluir, verificação de integridade, ZIP com senha via Zip4j, proteção contra Zip Slip e ações **Extrair aqui / Extrair para... / Abrir com...** diretamente no Explorer.
- A alpha.41 corrige o respeito à **área segura do Android** em telas e diálogos, evitando que conteúdo e botões entrem atrás da barra de navegação. A tela **Sobre** foi recentralizada e os cards/botões claros receberam bordas suaves e padronizadas; a revisão também alcança menus de contexto, visualizadores e o editor de texto/código.
- A alpha.40 reconstrói a **Lixeira** após o problema visual visto em aparelho real: nomes dos arquivos voltam a ocupar a área principal, cada item mostra tipo/tamanho/data/origem, **Restaurar** e **Apagar** ficam visíveis lado a lado, **Atualizar** e **Esvaziar Lixeira** permanecem no topo e os diálogos de confirmação voltam a exibir as duas ações corretamente. O nome original também passa a ser gravado no metadado da Lixeira, preservando compatibilidade com itens antigos.
- A alpha.39 corrige a compilação da alpha.38 no diálogo de exclusão: a largura responsiva do `BoxWithConstraints` agora é capturada antes do `Row`, evitando o erro de receiver implícito do Compose sem alterar o layout solicitado.
- A alpha.38 refina a experiência da **Lixeira** e do diálogo de exclusão: o item excluído volta a exibir nome e origem na lista, **Esvaziar Lixeira** fica sempre acessível em layout responsivo e a janela de exclusão mostra os nomes selecionados com os três comandos lado a lado sempre que houver largura suficiente.
- A alpha.37 corrige os dois erros Kotlin restantes apontados pelo CI após a correção do Media3: callback de fechamento do visualizador com retorno `Unit` explícito e tratamento nulo seguro de `WebResourceError` no preview do editor web.
- A alpha.36 corrige o build do GitHub após o Media3 1.11.1 exigir API 36: o player mantém as mesmas funções, mas passa a usar **Media3/ExoPlayer 1.9.4**, compatível com o `compileSdk 35` e a toolchain atual do projeto.
- A alpha.35 transforma o visualizador de texto/código em um **editor interno leve**: Salvar/Salvar como, desfazer/refazer, localizar/substituir, ir para linha, linha/coluna, números de linha, codificação detectada, aviso de alterações não salvas, gravação temporária validada e preview web para HTML/CSS/JS. Arquivos grandes entram em leitura parcial para evitar travamentos.
- A alpha.34 modernizou o player interno de vídeo com **Media3/ExoPlayer**; desde a alpha.36 a dependência está fixada em **1.9.4** para compatibilidade com `compileSdk 35`: play/pause, progresso/tempo, ±10 s, velocidades de 0.5x a 2x, reiniciar, tela cheia, rotação estável, Ajustar/Preencher, retomada de posição, informações do arquivo, controles auto-ocultáveis e fallback claro para abertura externa.
- A alpha.33 padroniza janelas e rolagens com áreas seguras do Android, reorganiza a exclusão com botões responsivos e ícones XP, refina percentuais de armazenamento, moderniza a tela Sobre sem abandonar a identidade XP e melhora densidade/alinhamento de Lixeira, lista, grade, seleção e Informações do dispositivo.
- A alpha.32 corrige vazamentos visuais da Lixeira, reforça o esvaziamento físico da pasta gerenciada, simplifica a seleção para **Copiar/Mover/Excluir/Mais**, compacta Lixeira/Favoritos/progresso, trata `Android/data`/`obb` como áreas restritas e melhora Propriedades, análise de armazenamento, grade e barra inferior.
- O card de armazenamento abre uma análise detalhada sob demanda, com total/usado/livre, categorias de arquivo, pastas que mais ocupam espaço e maiores arquivos; a varredura roda fora da thread principal, pode ser cancelada e não pesa na abertura do app.
- Arquivos e pastas ganharam identificação textual consistente de tipo em lista, grade, status, Lixeira e visualizadores (por exemplo, **Imagem JPEG**, **Documento PDF** e **Aplicativo Android (APK)**).
- A toolbar ganhou acesso direto à Lixeira no lugar da ação duplicada **Exibir**; Ajuda foi reorganizada em tópicos expansíveis e Sobre reúne versão, desenvolvedor, PIX copiável e novidades da versão.
- Imagens e vídeos agora podem mostrar miniaturas assíncronas em lista/grade com cache LRU limitado; os ícones XP originais continuam sendo o fallback e permanecem inalterados para os demais tipos.
- ZIP agora funciona como navegador de compactados, com pastas internas, pesquisa, ordenação, seleção, extração total/parcial, destino configurável, progresso, senha e verificação de integridade; TXT/código usa o editor interno da alpha.35 com histórico, busca/substituição, números de linha, codificação e salvamento seguro; APK mostra nome/ícone, versão instalada × arquivo, SDK, arquiteturas, assinatura SHA-256, permissões e compatibilidade antes de encaminhar ao instalador do Android.
- Reconhecimento visual de dezenas de tipos de arquivo: PDF, Word, Excel, PowerPoint, HTML, CSS, JS, JSON, XML, APK, ZIP, RAR, 7Z, imagens, áudio, vídeo, código e outros.
- Navegação real pelo armazenamento compartilhado primário.
- Histórico de navegação com Voltar e Avançar, além da ação Subir.
- Busca no diretório atual.
- Modos Lista e Grade.
- Ordenação por nome, data, tamanho e tipo.
- Toque longo entra diretamente no modo de seleção; a barra de ferramentas troca temporariamente para ações de seleção e volta ao normal ao concluir/cancelar.
- Copiar, recortar/mover e colar, incluindo pastas recursivas; quando há conteúdo na área de transferência, **Downloads** vira temporariamente **Colar** na barra de ferramentas. Cópia, mover e exclusão exibem uma janela de progresso com o item atual, contagem e barra animada, com opção de **Cancelar** a qualquer momento.
- Criar pasta, renomear e excluir.
- Visualizador interno para imagens, textos/código editáveis, HTML, PDF, ZIP, áudio, vídeo e informações de APK; o vídeo usa Media3/ExoPlayer com controles próprios e formatos/codec não suportados continuam disponíveis via `Abrir com...`.
- Abertura externa por aplicativo compatível via `FileProvider` quando necessário.
- Compartilhar arquivos.
- Favoritos persistentes.
- Atalho fixo para a pasta Downloads do armazenamento.
- Data e horário do item são mantidos em cache durante a listagem para reduzir acesso repetitivo ao armazenamento; propriedades continuam exibindo criação e modificação.
- Opção persistente para mostrar/ocultar arquivos ocultos.
- Propriedades básicas de arquivos/pastas.
- Na primeira abertura sem permissão, um pop-up central obrigatório explica o acesso aos arquivos e leva diretamente à tela do Android para conceder `MANAGE_EXTERNAL_STORAGE`.
- **Informações do dispositivo** ganhou painel visual moderno feito integralmente em Compose: resumo do aparelho, indicadores de RAM/armazenamento/bateria, sistema, conectividade, recursos, sensores e exportação para IA, sem foto fake nem banco fixo de especificações.
- O painel também detecta sensores reais via `SensorManager` e oferece **Copiar resumo**, **Salvar PNG** e **Compartilhar imagem**, mantendo identificadores sensíveis fora das saídas rápidas.
- Launcher legado/adaptativo atualizado com nova arte da pasta dourada e órbita azul, agora com margem de segurança maior, sem borda aparente e com transparência correta para evitar cortes na máscara adaptativa do Android.
- Ícones PNG usados na navegação foram otimizados para densidade Android: o conjunto comum saiu de 256×256 `nodpi` para 192×192 em `drawable-xxxhdpi`, permitindo que o sistema decodifique tamanhos menores em telas de densidade inferior.
- Ícones dos itens da lista/grade são decodificados em background e reutilizados por um cache LRU de 6 MiB, evitando a primeira decodificação pesada no frame da rolagem; os primeiros tipos visíveis são aquecidos de forma assíncrona.
- Barras de status e navegação do Android permanecem visíveis no app; somente o modo **Tela cheia** do player de vídeo pode ocultá-las temporariamente, com restauração ao sair/fechar.

## Estrutura

```text
app/src/main/java/com/exploradorxp/app/
  MainActivity.kt
  ExplorerScreen.kt
  ExplorerViewModel.kt
  ExplorerModels.kt
  FileRepository.kt
  ExplorerItemTransforms.kt
  FileDisplayFormatter.kt
  FileTypeClassifier.kt
  FileIconMapper.kt
  FileThumbnail.kt
  InternalViewer.kt
  VideoPlayerViewer.kt
  TextCodeEditorViewer.kt
  ArchiveManager.kt
  ArchiveBrowserIndex.kt
  ArchiveViewer.kt
  ArchiveProgressNotifier.kt
  PreferencesStore.kt
  DeviceInfo.kt
  DeviceInfoScreen.kt

app/src/test/java/com/exploradorxp/app/
  ExplorerItemTransformsTest.kt
  ArchiveBrowserIndexTest.kt
  DeviceInfoReportTest.kt

baselineprofile/src/main/java/com/exploradorxp/benchmark/
  BaselineProfileGenerator.kt
  ExplorerMacrobenchmark.kt
  BenchmarkFixtures.kt

app/src/main/res/drawable-xxxhdpi/
  150 PNGs do pacote visual XP

app/src/main/res/drawable-nodpi/
  5 variantes grandes/launcher

docs/
  mockup_explorador_android_xp.png
  catalogo_icones.png
  ICON_FILES.txt
```

## Acesso aos arquivos

O app usa acesso amplo ao armazenamento compartilhado porque sua função principal é gerenciamento de arquivos. Em Android 11+, o usuário precisa conceder manualmente **Acesso a todos os arquivos**. Em versões anteriores, o app solicita as permissões legadas necessárias.

A primeira alpha prioriza o armazenamento compartilhado primário. O suporte dedicado a SD/USB por SAF (`ACTION_OPEN_DOCUMENT_TREE`) está planejado para a próxima etapa, para cobrir volumes que não podem ser tratados diretamente por `java.io.File`.



## Dispositivo e diagnóstico alpha.21

- **Ferramentas → Informações do dispositivo** abre um painel local com dados reais expostos pelo Android, incluindo CPU/arquitetura/clock disponível, rede atual, Wi‑Fi, rede móvel, SIM/eSIM, Bluetooth, VPN, sensores detectados pelo `SensorManager`, resumo copiável, ficha PNG compartilhável e relatório técnico para IA.
- A tela prioriza informações fáceis de entender: nome/modelo, fabricante, Android/API, patch de segurança, processador, núcleos/arquitetura, RAM, armazenamento interno, resolução/taxa de atualização, bateria e recursos disponíveis.
- RAM, armazenamento e bateria têm indicadores compactos para leitura rápida; os valores variáveis podem ser atualizados sem fechar a tela.
- **Exportar relatório para IA** usa o seletor de arquivos do Android e gera um `.txt` estruturado com valores humanos e valores brutos úteis para diagnóstico.
- A exportação é propositalmente mais detalhada que a tela e inclui ABI, kernel/build, bytes de RAM/armazenamento, densidade, bateria e recursos. Não coleta IMEI, serial, Android ID, MAC, localização, contas nem lista/conteúdo dos arquivos do usuário.
- O workflow normal **Gerar APK** deixou de regenerar Baseline Profile a cada build; ele valida o perfil já embarcado e segue para `assemblePerformance`. A geração pesada por Managed Virtual Device permanece no workflow separado **Desempenho e Baseline Profile**.

## Correção de build alpha.20

- O log **Gerar APK 19** confirmou que `testDebugUnitTest` e `lintDebug` passam integralmente. A integração de desempenho avançou até `:baselineprofile:compileNonMinifiedReleaseKotlin`.
- Corrigida a compilação do módulo `:baselineprofile`: `@LargeTest` (`androidx.test.filters.LargeTest`) era usado pelos geradores/benchmarks sem uma dependência explícita do AndroidX Test runner/rules.
- Adicionados `androidx.test:runner:1.7.0` e `androidx.test:rules:1.7.0` ao módulo produtor, mantendo `androidx.test.ext:junit:1.3.0`, Macrobenchmark 1.5.0 e UI Automator 2.4.0.
- A correção não altera o app nem o visual; atua somente na infraestrutura de Baseline Profile/Macrobenchmark.
- O próximo gate do CI passa a ser a execução real do Managed Virtual Device para gerar o perfil e, em seguida, `assemblePerformance`.

## Correção de build alpha.19

- O log **Gerar APK 18** confirmou novamente que `testDebugUnitTest` passa; o job parou somente em `lintDebug`.
- O detector `ProduceStateDoesNotAssignValue` continuou acusando `PropertiesDialog` apesar da atribuição explícita dentro de `produceState`. Para remover a ambiguidade sem suprimir o lint, o carregamento assíncrono foi migrado para `remember + LaunchedEffect`.
- As consultas de propriedades continuam executando em `Dispatchers.IO`, portanto a correção não reintroduz I/O do sistema de arquivos na thread principal.
- Baseline Profile, Macrobenchmark, R8, `shrinkResources`, testes e lint continuam habilitados exatamente como na alpha.18.

## Desempenho alpha.18

- Adicionado módulo `:baselineprofile` com **BaselineProfileRule** e **Macrobenchmark**.
- O perfil separa a inicialização (Startup Profile) da jornada de pasta grande + rolagem (Baseline Profile), evitando colocar todo o scroll no caminho de startup.
- `ProfileInstaller 1.4.1` foi integrado para permitir que o perfil embarcado também seja aplicado em instalações por APK/sideload quando suportado.
- Foi adicionado um perfil inicial em `app/src/main/baseline-prof.txt` para os caminhos centrais do Explorer; a geração automatizada produz um perfil baseado em execução real e é mesclada em `src/main`.
- Macrobenchmarks comparam inicialização e fluidez de rolagem **com e sem Baseline Profile** usando `StartupTimingMetric` e `FrameTimingMetric`.
- O benchmark cria dados artificiais em `/sdcard/Download/ExploradorXP-Benchmark`, sem usar ou modificar os arquivos pessoais do usuário.
- `ReportDrawnWhen` informa quando a listagem terminou o carregamento, permitindo medir o estado realmente pronto para interação.
- Novo workflow **Desempenho e Baseline Profile** gera o perfil e publica relatórios. As métricas em emulador servem como sinal de regressão; medições finais devem ser confirmadas em aparelho físico.
- O workflow **Gerar APK** reutiliza o Baseline Profile já embarcado e não inicia mais um emulador em toda compilação. A regeneração do perfil fica concentrada no workflow **Desempenho e Baseline Profile**.

### Comandos de desempenho

```bash
# Gera/atualiza o Baseline Profile via dispositivo gerenciado
gradle :app:generateBaselineProfile

# Lista as tarefas de benchmark disponíveis
gradle :baselineprofile:tasks --all
```

Os resultados detalhados do Macrobenchmark ficam em `baselineprofile/build/outputs/` e incluem JSON e traces Perfetto quando disponíveis.

## Correção de build alpha.17

- Corrigidas chamadas `removeLast()` que o Android Lint resolvia como `java.util.List.removeLast()`, disponível apenas a partir da API 35. O histórico de navegação e a lista de recentes agora usam `removeAt(lastIndex)`, mantendo compatibilidade com o Min SDK 26.
- O log do build 16 confirmou que os testes JVM continuam passando e que a correção do `produceState` da alpha.16 foi aceita; a nova falha ocorreu somente no `lintDebug`.
- O workflow agora separa testes e lint. Se o lint falhar, o relatório textual completo é impresso no próprio log antes de o job encerrar, evitando que erros adicionais fiquem escondidos atrás do primeiro `First failure`.
- Mantidos testes + lint como portas obrigatórias antes do APK `performance`; nenhuma regra foi suprimida e nenhum baseline de lint foi criado.

## Correção de build alpha.16

- Corrigido o bloqueio do Android Lint em `PropertiesDialog`: o resultado carregado em `Dispatchers.IO` agora é atribuído explicitamente ao `produceState`, preservando a consulta assíncrona e satisfazendo a regra `ProduceStateDoesNotAssignValue`.
- O workflow continua exigindo testes JVM e lint antes do APK; nenhum baseline de lint foi criado para esconder o erro.
- GitHub Actions atualizadas para gerações com runtime Node 24 (`checkout@v6`, `setup-java@v6` e `setup-gradle@v6`), removendo os avisos de ações antigas vistos no build 15.
- APK de desempenho continua sendo gerado pelo build type `performance`, com R8 e `shrinkResources`.

## Desempenho alpha.15

Esta versão concentra a primeira otimização estrutural da navegação. A leitura do armazenamento agora gera um **snapshot bruto da pasta**; busca, ordenação, `Pastas primeiro` e mostrar/ocultar arquivos ocultos são projetados sobre esse snapshot em memória, em `Dispatchers.Default`, sem chamar `listFiles()` novamente a cada mudança. As últimas 12 pastas/abas ficam em um pequeno cache LRU para que Voltar/Avançar possam apresentar o conteúdo imediatamente enquanto a atualização real ocorre em segundo plano.

Os textos de data, hora e tamanho mostrados em lista/grade são preparados junto com os metadados em `Dispatchers.IO`, eliminando criação de formatadores durante o scroll. A UI também deixou de usar `canonicalPath` durante composição/navegação visual, a camada duplicada de gesto da área externa foi removida e a animação `animateItem` foi retirada dos itens da lista/grade para priorizar fluidez.

A descoberta de volumes passou a ser cacheada por sessão e `StatFs` só é consultado quando o cartão de armazenamento realmente pode aparecer na página inicial. A validação de pasta ao navegar também sai da thread principal.

O pipeline agora executa testes JVM e lint antes do APK e publica um build **`performance`** instalável, baseado em Release, com **R8 + `shrinkResources`**, assinado com a chave debug apenas enquanto o projeto está em alpha. O bloco `release` também passou a usar minificação e redução de recursos. Foram adicionados os primeiros testes automatizados para busca, arquivos ocultos, ordenação e prioridade de pastas.

## Interface alpha.14

Cópia, mover e exclusão deixam de ser operações "silenciosas": agora abrem uma janela de progresso no estilo do app (título, ícone da ação, nome do item atual, barra de progresso animada, contagem "X de Y itens" e percentual), com um botão **Cancelar** que interrompe a operação a qualquer momento. O total de itens é calculado antes de começar, então a barra reflete o andamento real mesmo em pastas com muitos arquivos. Mover dentro do mesmo volume continua usando o caminho rápido (renomear em vez de copiar byte a byte), mas agora também é refletido na barra de progresso. Internamente, as atualizações de progresso são agrupadas a cada ~80 ms para não sobrecarregar a interface durante transferências com milhares de arquivos.

## Interface alpha.13

Foco em desempenho e polimento visual, sem mudanças de layout. As Propriedades de arquivo/pasta (tamanho, datas de criação/modificação, permissões de leitura/escrita) deixaram de ser lidas na thread de composição e passaram a ser carregadas em `Dispatchers.IO`, com um indicador de carregamento breve enquanto os dados chegam. A lista e a grade de arquivos agora reiniciam a rolagem no topo ao trocar de pasta ou aba, em vez de manter a posição da pasta visitada anteriormente, e os itens são reordenados com uma animação curta de posição ao mudar a ordenação. O indicador de espaço usado no cartão de armazenamento faz uma transição suave ao alternar entre armazenamento interno e cartão SD. A barra de status inferior deixou de recalcular contagem e tamanho total a cada recomposição não relacionada à lista.

## Interface alpha.12

O ícone do launcher foi corrigido estruturalmente para o padrão Adaptive Icon do Android. O fundo grafite agora é uma camada separada que ocupa toda a máscara escolhida pelo sistema, enquanto a pasta dourada fica isolada em uma camada foreground transparente, maior e centralizada. Isso elimina o efeito de “ícone dentro de outro ícone” que deixava a arte pequena nas telas de permissões, configurações e instalador. Android 13+ também recebeu uma camada monocromática própria para ícones temáticos.

## Interface alpha.11

A interface segue o Explorer do Windows XP adaptado a telas Android. O cabeçalho possui menu clássico, barra de ferramentas compacta e campo Endereço. O Endereço também permite alternar entre armazenamento interno e cartão SD quando detectado. O cartão de capacidade fica restrito à página inicial, deixando as pastas com mais área útil.

Os ícones principais de pastas, navegação e dispositivos foram atualizados para uma aparência mais próxima do Windows XP, mantendo os recursos já existentes para tipos de arquivo.

Na alpha.7, a barra de ferramentas foi compactada para caber inteira sem rolagem horizontal. O toque longo em arquivo/pasta abre as ações no centro da tela; o toque longo em área vazia oferece Colar, Nova pasta, Selecionar tudo, Atualizar e Propriedades.

Na alpha.9, os menus Arquivo/Editar/Exibir/Favoritos/Ferramentas/Ajuda seguem um fluxo mais próximo do Explorer clássico, com menus suspensos compactos. Ferramentas ganhou `Organizar ›` com nome, data, tamanho, tipo e `Pastas primeiro`. Ajuda ganhou Manual de Ajuda, Sobre e Doação.

O modo de seleção passa a usar a própria barra de ferramentas com sete ações: Copiar, Mover, Excluir, Renomear, Compartilhar, Propriedades e Selecionar tudo. O menu contextual do botão de opções foi remodelado para uma lista compacta no estilo clássico.

Na alpha.10, o pacote de ícones v2 foi aplicado às ações principais da barra e aos botões de opções. Os ícones superiores ficaram maiores e mais encorpados, e o botão lateral de opções deixou de usar a bolinha azul. A tipografia compacta foi normalizada para melhorar a legibilidade sem perder o layout de Explorer clássico. Os controles decorativos de minimizar, maximizar e fechar foram removidos do cabeçalho principal. O aplicativo também deixou o modo imersivo: as barras de sistema do Android voltam a permanecer visíveis.

Na alpha.11, o ícone do aplicativo foi substituído pelo novo desenho grafite/dourado e preparado como launcher adaptativo, round e legado, sem borda branca. O visualizador interno deixou de fazer decodificação pesada de imagem, leitura de texto, renderização de PDF, leitura de ZIP e inspeção de APK diretamente na thread da interface. Essas operações agora rodam em I/O com indicadores de carregamento. Arquivos de texto grandes usam prévia limitada e o modo de leitura usa o componente nativo do Android para reduzir travamentos. A listagem também passou a armazenar os metadados básicos de cada item uma única vez, e buscas cancelam/agrupam atualizações rápidas para evitar varreduras repetidas.

## Build

Abra o projeto no Android Studio e sincronize o Gradle. O workflow `.github/workflows/gerar-apk.yml` executa testes JVM e lint e, em seguida, gera um APK `performance` instalável (Release otimizado com R8/`shrinkResources`, assinado com chave debug enquanto o projeto está em alpha) e o publica diretamente como asset da prerelease `explorador-xp-dev` — sem empacotar o APK em ZIP de artifact.

## Observação

O cabeçalho reproduz de forma mais fiel a estrutura do Explorer do Windows XP, mas mantém áreas de toque e comportamento adaptados a telas verticais de Android.
