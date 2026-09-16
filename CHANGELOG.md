# CHANGELOG

## 0.1.0-alpha.21

- Adicionado **Ferramentas → Informações do dispositivo**, com leitura real dos dados expostos pelo Android, sem banco fixo de especificações por modelo.
- O painel mostra apenas informações úteis para uso comum: nome/modelo, fabricante, Android/API, patch de segurança, processador, CPU, RAM, armazenamento, tela, bateria e recursos disponíveis.
- RAM, armazenamento e bateria ganharam indicadores visuais compactos; a tela pode ser atualizada sem fechar o painel.
- Adicionada a opção **Exportar relatório para IA**, usando o seletor de arquivos do Android para salvar um TXT estruturado com valores humanos e valores brutos para diagnóstico.
- O relatório inclui detalhes técnicos extras como ABI, kernel/build, bytes de RAM/armazenamento, densidade/taxa da tela, estado da bateria e recursos, mas exclui IMEI, serial, Android ID, MAC, localização, contas e lista/conteúdo de arquivos do usuário.
- A coleta e a escrita do relatório executam I/O fora da thread principal.
- Adicionado teste JVM para validar os campos técnicos e o contrato de privacidade do relatório para IA.
- O workflow **Gerar APK** deixou de iniciar Managed Virtual Device e regenerar Baseline Profile em todo build; agora valida e reutiliza `app/src/main/baseline-prof.txt` antes do `assemblePerformance`.
- A geração pesada do Baseline Profile e os Macrobenchmarks permanecem no workflow separado **Desempenho e Baseline Profile**.
- O log do build 20 confirmou testes JVM, lint, geração real de Baseline Profile e `assemblePerformance` com sucesso; a geração do perfil consumiu 8m38s e motivou a separação dos workflows.
- Versão sincronizada para `0.1.0-alpha.21` / `versionCode 21`.

## 0.1.0-alpha.20

- Analisado o log `Gerar APK 19`: `testDebugUnitTest` e `lintDebug` passaram; o job avançou até a compilação do módulo `:baselineprofile`.
- Corrigidos os erros `Unresolved reference 'filters'` e `Unresolved reference 'LargeTest'` em `BaselineProfileGenerator.kt` e `ExplorerMacrobenchmark.kt`.
- Adicionadas dependências explícitas `androidx.test:runner:1.7.0` e `androidx.test:rules:1.7.0` ao módulo de performance, sem depender de dependências transitivas.
- Mantidos Macrobenchmark 1.5.0, ProfileInstaller 1.4.1, R8, `shrinkResources`, testes JVM e lint obrigatório.
- Versão sincronizada para `0.1.0-alpha.20` / `versionCode 20`.

## 0.1.0-alpha.19

- Analisado o log `Gerar APK 18`: `testDebugUnitTest` passou e o build parou em um único erro de `lintDebug` (`ProduceStateDoesNotAssignValue`) em `PropertiesDialog`.
- Substituído `produceState + withContext` por `remember + LaunchedEffect + withContext(Dispatchers.IO)`, mantendo a leitura de propriedades fora da thread principal e eliminando o falso positivo sem baseline/supressão de lint.
- Mantidos Baseline Profile, Startup Profile, Macrobenchmark, R8, `shrinkResources`, testes JVM e lint obrigatório.
- Versão sincronizada para `0.1.0-alpha.19` / `versionCode 19`.

## 0.1.0-alpha.18

- Implementado Baseline Profile no app com plugin `androidx.baselineprofile` 1.5.0 e `ProfileInstaller` 1.4.1.
- Adicionado perfil inicial direcionado a `MainActivity`, Compose do Explorer, ViewModel, repositório, transformações, formatação, ícones e preferências.
- Criado módulo `:baselineprofile` com geração automática usando `BaselineProfileRule`, separando startup do percurso de rolagem para manter o Startup Profile enxuto.
- Adicionados Macrobenchmarks para inicialização e rolagem, comparando `CompilationMode.None` com `CompilationMode.Partial(BaselineProfileMode.Require)`.
- Adicionado dataset sintético isolado com 600 arquivos e 40 pastas para medições reproduzíveis.
- `MainActivity` ganhou entrada opcional de automação de desempenho e `ReportDrawnWhen`, sem alteração visual no uso normal.
- Manifest passou a declarar o app como `profileable` para medições não-debuggable.
- Novo workflow `Desempenho e Baseline Profile` gera perfil e relatórios; `Gerar APK` também atualiza o perfil antes de montar o APK `performance`.
- Versão atualizada para `0.1.0-alpha.18` / `versionCode 18`.

## 0.1.0-alpha.17

- Corrigido erro `NewApi` apontado pelo build 16 em `ExplorerViewModel.kt`: `removeLast()` foi substituído por `removeAt(lastIndex)` para manter compatibilidade com Android API 26–34.
- Aplicada a mesma correção preventiva em `PreferencesStore.kt`, onde a lista de arquivos recentes também usava `removeLast()`.
- Confirmado pelo build 16 que `testDebugUnitTest` passa e que o erro `ProduceStateDoesNotAssignValue` corrigido na alpha.16 não voltou a aparecer.
- Workflow dividido em etapas explícitas de testes e lint; em caso de falha, o relatório completo `lint-results-debug.txt` passa a ser impresso no log, permitindo identificar todos os erros de uma única execução.
- Mantidos R8, `shrinkResources`, APK `performance` e bloqueio do APK por testes/lint.
- Versão sincronizada para `0.1.0-alpha.17` / `versionCode 17`.

## 0.1.0-alpha.16

- Corrigido erro de `lintDebug` em `ExplorerScreen.kt` (`ProduceStateDoesNotAssignValue`) no diálogo de Propriedades.
- Mantida a leitura de metadados em `Dispatchers.IO`; o resultado agora é atribuído explicitamente ao estado produzido, sem voltar a bloquear a thread principal.
- Confirmado pelo log do build 15 que `compileDebugUnitTestKotlin` e `testDebugUnitTest` foram executados antes da falha de lint.
- Workflow atualizado de `actions/checkout@v4`, `actions/setup-java@v4` e `gradle/actions/setup-gradle@v4` para as gerações `v6`, removendo dependência das actions com runtime Node 20 avisadas pelo runner.
- Mantidos `testDebugUnitTest` + `lintDebug` como portas obrigatórias antes de `assemblePerformance`; nenhum baseline/supressão foi usado para mascarar o erro.
- Versão sincronizada para `0.1.0-alpha.16` / `versionCode 16`.

## 0.1.0-alpha.15

- Navegação passou a usar snapshot de diretório em memória: busca, ordenação, `Pastas primeiro` e filtro de ocultos não relêem mais o armazenamento a cada alteração.
- Adicionado cache LRU das últimas 12 pastas/abas para acelerar Voltar/Avançar e reapresentar pastas recentes imediatamente.
- Filtragem e ordenação do snapshot passam a rodar em `Dispatchers.Default`, fora da thread principal.
- Data, hora e tamanho exibidos em lista/grade passam a ser pré-formatados durante a captura dos metadados em `Dispatchers.IO`, removendo trabalho repetitivo do scroll.
- Validação de diretório ao navegar passa para `Dispatchers.IO`; caminhos usados pela UI deixam de resolver `canonicalPath` durante composição.
- Checagens de existência/tipo usadas por abrir e compartilhar também saem da thread principal; seleção/cópia deixa de varrer `File.exists()` item a item antes de iniciar a operação.
- Descoberta de volumes é cacheada durante a sessão e `StatFs` só é atualizado quando o cartão de armazenamento da página inicial realmente é necessário.
- Removida a animação `animateItem` dos arquivos/pastas e removida uma camada duplicada de `combinedClickable`, reduzindo custo de gesto e reposicionamento durante rolagem.
- `FileItem` ganhou metadados imutáveis de oculto e textos prontos de apresentação, além de chave de caminho sem nova consulta ao filesystem.
- Primeira suíte de testes JVM adicionada para busca, ocultos, `Pastas primeiro` e ordenação por tamanho.
- Workflow passa a executar `testDebugUnitTest` e `lintDebug` antes da compilação.
- APK publicado passa de `assembleDebug` para um build instalável `performance`, baseado em Release, com R8 e `shrinkResources`; `release` também habilita minificação/redução.
- Versão sincronizada para `0.1.0-alpha.15` / `versionCode 15`.

## 0.1.0-alpha.14

- Copiar, mover e excluir agora exibem uma janela de progresso dedicada (ícone da ação, nome do item atual, barra animada e contagem "X de Y itens • Z%"), em vez de deixar a interface sem retorno durante a operação.
- Adicionado botão **Cancelar** na janela de progresso: interrompe cópia, mover ou exclusão em qualquer ponto, com aviso "Operação cancelada." e atualização imediata da listagem.
- `FileRepository.paste()` e `FileRepository.delete()` passam a contar o total de itens antes de iniciar e relatam progresso por item processado, incluindo o caminho rápido de mover no mesmo volume (renomear em vez de copiar).
- Atualizações de progresso são agrupadas em intervalos de ~80 ms para evitar sobrecarga de recomposição em transferências com muitos arquivos, sempre garantindo a emissão final em 100%.
- Nova transferência cancela automaticamente qualquer cópia/mover/exclusão ainda em andamento, evitando duas operações concorrentes sobre o armazenamento.
- Versão sincronizada para `0.1.0-alpha.14` / `versionCode 14`.

## 0.1.0-alpha.13

- Corrigida leitura bloqueante de arquivo na tela de Propriedades: tamanho, datas de criação/modificação e permissões de leitura/escrita agora são apuradas em `Dispatchers.IO` em vez de na thread de composição, com indicador de carregamento enquanto os dados chegam.
- Lista e grade de arquivos passam a ter estado de rolagem próprio por pasta/aba, reiniciando no topo ao navegar em vez de herdar a posição da pasta anterior.
- Itens da lista e da grade recebem animação curta de posição (`animateItem`) ao reordenar por nome, data, tamanho ou tipo.
- Barra de progresso do cartão de armazenamento passa a animar a transição de proporção usada/livre ao alternar entre armazenamento interno e cartão SD, em vez de saltar direto ao novo valor.
- Diálogo de Propriedades ganhou `animateContentSize` para acomodar a troca entre o estado de carregamento e os detalhes sem um salto abrupto de altura.
- Barra de status inferior deixou de refiltrar e resomar a lista de itens a cada recomposição não relacionada, recalculando apenas quando a listagem ou a seleção mudam de fato.
- Versão sincronizada para `0.1.0-alpha.13` / `versionCode 13`.

## 0.1.0-alpha.12

- Corrigida a estrutura do ícone adaptativo: o fundo grafite agora é uma camada separada e a pasta dourada é um foreground transparente.
- Removido o efeito de dupla máscara que fazia o ícone aparecer pequeno dentro do próprio recorte do Android.
- Pasta do launcher ampliada e recentralizada para ocupar melhor a área útil sem depender de bordas desenhadas na própria imagem.
- Adicionada camada monocromática dedicada para Android 13+ e suporte a ícones temáticos.
- Mipmaps normal e round regenerados a partir da mesma composição para manter consistência em telas do sistema.
- Versão sincronizada para `0.1.0-alpha.12` / `versionCode 12`.

## 0.1.0-alpha.11

- Novo ícone oficial do aplicativo: pasta dourada sobre fundo grafite, sem borda branca.
- Launcher preparado em versões adaptativa, round e mipmaps legados para manter recorte correto em launchers e telas do Android.
- Permissão de arquivos virou o primeiro fluxo do app: sem acesso, abre imediatamente um pop-up central com botão **Liberar acesso**.
- O app não inicia mais a varredura do armazenamento antes de a permissão ser concedida.
- Imagens passam a ser decodificadas fora da thread principal, com amostragem de resolução e indicador de carregamento.
- Textos/código passam a ser lidos em I/O; arquivos grandes usam prévia limitada para evitar congelamentos.
- Visualização somente leitura de texto passou a usar `TextView` nativo e edição usa `EditText` nativo, reduzindo custo de recomposição.
- Renderização de páginas PDF movida para I/O com carregamento assíncrono.
- Leitura da lista de arquivos ZIP e análise de APK movidas para I/O; ZIPs muito grandes usam prévia limitada.
- Metadados dos itens (nome, tipo, tamanho, data e extensão) são capturados uma única vez na listagem, evitando chamadas repetidas ao sistema de arquivos durante recomposições.
- Atualizações de busca receberam debounce e listagens antigas são canceladas ao iniciar uma nova, reduzindo trabalho duplicado.
- Consultas de armazenamento deixaram de bloquear a thread principal durante a atualização.
- Versão sincronizada para `0.1.0-alpha.11` / `versionCode 11`.

## 0.1.0-alpha.10

- Aplicado o `Explorador XP Icon Pack v2` aos ícones principais de navegação, seleção e operações.
- Ícones da barra superior ampliados e reforçados visualmente, mantendo a barra sem rolagem horizontal.
- Botão lateral de opções atualizado: removida a antiga bolinha azul e adotado o novo ícone retangular clássico.
- O mesmo novo ícone de opções passou a ser usado também na visualização em grade.
- Tipografia compacta normalizada: legendas da barra, endereço, metadados e barra de status ficaram mais legíveis e consistentes.
- Removidos do cabeçalho principal os controles decorativos de minimizar, maximizar e fechar.
- Removido o modo tela inteira/imersivo; as barras de status e navegação do Android permanecem visíveis.
- Visualizador interno teve textos de interface pequenos normalizados para 12sp, mantendo a barra de status compacta.
- Versão sincronizada para `0.1.0-alpha.10` / `versionCode 10`.

## 0.1.0-alpha.9

- Toque longo em arquivo/pasta entra diretamente no modo de seleção, sem abrir janela central.
- A barra de ferramentas troca temporariamente para sete ações: Copiar, Mover, Excluir, Renomear, Compartilhar, Propriedades e Selecionar tudo.
- Ao concluir/cancelar a seleção, a barra normal de navegação volta automaticamente.
- Menu contextual do botão de opções remodelado como menu compacto em lista, mais próximo do Explorer clássico.
- Menus Arquivo, Editar, Exibir, Favoritos, Ferramentas e Ajuda mantidos como menus suspensos compactos.
- Ferramentas agora usa `Organizar ›` com submenu lateral para Nome, Data, Tamanho, Tipo e preferência persistente `Pastas primeiro`.
- Removida a necessidade da janela separada de ordenação.
- Ajuda ganhou Manual de Ajuda, Sobre e Doação.
- Sobre exibe `Desenvolvido por Adriedson Lemos`.
- Doação exibe a chave PIX `adriedson@outlook.com`.
- Adicionado visualizador interno para imagens, textos/código, HTML, PDF, ZIP, áudio/vídeo e APK.
- Arquivos de texto/código podem ser editados e salvos dentro do Explorador XP.
- HTML oferece visualização renderizada e código-fonte.
- ZIP mostra o conteúdo e permite extração segura para uma pasta ao lado do arquivo.
- Formatos ainda não suportados continuam abrindo pelo seletor de aplicativos do Android.
- Versão sincronizada para `0.1.0-alpha.9` / `versionCode 9`.

## 0.1.0-alpha.8

- Corrigido erro de compilação em `ExplorerScreen.kt` introduzido na alpha.7.
- `FileGrid` agora recebe corretamente `onContextMenu` e `onBlankLongPress`, usados pelo menu contextual e pelo toque longo em área vazia.
- Corrigidos os erros `No parameter with name onContextMenu`, `No parameter with name onBlankLongPress` e referências não resolvidas associadas.
- Versão sincronizada para `0.1.0-alpha.8` / `versionCode 8`.

## 0.1.0-alpha.7

- Toque longo em arquivo/pasta agora abre um **menu contextual central**, sem substituir o cabeçalho do Explorer.
- Menu contextual inclui Abrir, Copiar, Mover, Renomear, Excluir, Compartilhar (arquivos), Favoritar/Desfavoritar, Propriedades e Selecionar.
- Toque longo em área vazia abre ações da pasta atual: Colar, Nova pasta, Selecionar tudo, Atualizar e Propriedades.
- Ao copiar ou mover, o botão **Downloads** da barra de ferramentas é temporariamente substituído por **Colar**.
- A área de transferência é limpa após uma colagem concluída e pode ser cancelada pelo menu Editar.
- Seleção múltipla não substitui mais o cabeçalho; suas ações ficam disponíveis pelo menu **Editar**.
- Barra de ferramentas redimensionada para caber Voltar, Avançar, Início, Subir, Pesquisar, Downloads/Colar e Exibir sem rolagem horizontal.
- Ícones da barra foram reduzidos para melhorar a adaptação a telas estreitas.
- Novo ícone do aplicativo: pasta XP em fundo azul clássico com lupa, incluindo launcher legado e adaptativo.
- Versão sincronizada para `0.1.0-alpha.7` / `versionCode 7`.

## 0.1.0-alpha.6

- Aplicado o layout aprovado do Explorer XP móvel como referência principal da interface.
- Adicionado botão **Início** na barra de ferramentas.
- O campo **Endereço** passou a funcionar também como seletor de armazenamento.
- O menu do endereço mostra **Armazenamento interno** e, quando disponível, **Cartão SD**.
- O cartão com usado/livre/total passou a aparecer apenas na página inicial do armazenamento interno.
- Navegação para cartão SD passou a respeitar o limite da raiz do volume ao usar Subir.
- Atualizado o conjunto visual principal de pastas, ações e dispositivos com ícones mais próximos do Windows XP.

# Changelog

## 0.1.0-alpha.5

- Navegação inferior removida e substituída por barra de status no estilo Windows Explorer XP.
- Barra de status mostra quantidade de objetos e tamanho dos arquivos exibidos; com seleção, mostra quantidade e tamanho selecionados.
- Botão **Pastas** da barra de ferramentas substituído por **Downloads**.
- Ícones e linhas da visualização em lista ficaram menores e mais compactos.
- Visualização em grade também foi compactada.
- Tons de fundo, bordas, seleção, armazenamento e barra de status foram harmonizados com a família azul do cabeçalho.
- Mantidos armazenamento compacto, arquivos ocultos, data/hora, favoritos e operações de arquivo.


## 0.1.0-alpha.4

- Cabeçalho redesenhado para reproduzir mais fielmente o Windows Explorer do XP em formato móvel.
- Nova barra de título azul com ícone do Explorador e controles visuais de janela.
- Adicionada barra de menus clássica: Arquivo, Editar, Exibir, Favoritos, Ferramentas e Ajuda.
- Barra de ferramentas agora usa Voltar, Avançar, Subir, Pesquisar, Pastas e Exibir com os ícones XP existentes.
- Botão Avançar conectado ao histórico real de navegação já existente.
- Barra de endereço compacta com caminho atual e menu dos diretórios ancestrais.
- Busca passa a ocupar a própria barra de endereço quando ativada.
- Opção Mostrar/Ocultar arquivos ocultos movida para o menu Exibir, liberando espaço vertical.
- Mantido o medidor compacto de armazenamento da alpha.3.
- Mantidas as abas Arquivos, Downloads e Favoritos e todas as operações de arquivo existentes.
- Versão sincronizada para `versionCode 4`.

## 0.1.0-alpha.3

- Interface principal compactada para aproveitar melhor telas de celular.
- Botões Voltar, Subir e Exibir movidos para a barra superior, antes do título Explorador.
- Barra de localização redesenhada no estilo de endereço do Windows Explorer e reduzida em altura.
- Nova opção persistente para mostrar ou ocultar arquivos ocultos.
- Arquivos ocultos ficam escondidos por padrão.
- Data e horário de criação passam a aparecer nos itens em lista e grade, com fallback para a data de modificação quando o sistema de arquivos não fornece criação.
- A aba Recentes foi substituída por Downloads e abre diretamente a pasta Download do armazenamento.
- Cartão de armazenamento compactado: usado dentro da área verde, livre dentro da área livre e total fora do marcador, com o ícone alinhado à barra.
- Modo tela inteira imersivo com barras do sistema ocultas e reaparecimento temporário por gesto.
- Propriedades agora exibem data de criação e modificação.
- Versão sincronizada para `versionCode 3`.

## 0.1.0-alpha.2

- Corrigida a compilação no GitHub Actions: adicionado o import de `androidx.activity.compose.setContent` em `MainActivity.kt`.
- Removido código não utilizado da Activity.
- Workflow atualizado para obter o `versionName` diretamente de `app/build.gradle.kts` ao nomear e publicar o APK.
- Versão sincronizada para `versionCode 2`.

## 0.1.0-alpha.1

- Início do projeto Android nativo em Kotlin + Jetpack Compose.
- Implementação da tela principal baseada no mockup móvel aprovado.
- Integração dos 147 ícones XP gerados para o projeto.
- Navegação real por diretórios do armazenamento compartilhado.
- Lista/grade, busca, ordenação e breadcrumb.
- Histórico Voltar/Avançar e navegação Subir.
- Seleção múltipla e ações copiar, mover, colar, excluir e compartilhar.
- Criar pasta e renomear.
- Favoritos e arquivos recentes persistentes.
- Abertura de arquivos via FileProvider.
- Tela de propriedades básica.
- Fluxo de permissão para acesso amplo ao armazenamento.
- Ícone adaptativo inicial do aplicativo.
