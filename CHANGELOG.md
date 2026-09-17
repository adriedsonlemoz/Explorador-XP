# CHANGELOG

## 0.1.0-alpha.32

- Corrigido o vazamento de artefatos de Lixeira na navegação e na análise: `.ExploradorXP_Lixeira`, `.$recycle_bin$`, `$recycle.bin`, `.trashed-*` e `.trash-*` deixam de aparecer como arquivos/pastas comuns. Artefatos do sistema são apenas ocultados, não apagados pelo app.
- **Esvaziar Lixeira** agora remove diretamente todas as entradas físicas e órfãs da pasta gerenciada pelo Explorador XP e apaga a própria raiz quando fica vazia.
- `Android/data` e `Android/obb` vazios por restrição do sistema passam a mostrar **Acesso limitado pelo Android** em vez de afirmar que a pasta está vazia.
- Arquivos sem extensão passam a ser identificados como **Arquivo sem extensão**; Propriedades reutiliza o classificador central e ganhou **Copiar nome** e **Copiar caminho**.
- Corrigidas pluralizações do Explorer (`1 selecionado`, `2 selecionados`, `1 item`, `2 itens`) e removidos textos `item(ns)`/`selecionado(s)` dos fluxos principais.
- Toolbar de seleção simplificada para quatro ações visíveis: **Copiar**, **Mover**, **Excluir** e **Mais**; Renomear, Enviar, Detalhes e Selecionar todos ficam no menu Mais.
- Lixeira ficou mais compacta: cabeçalho mostra quantidade/tamanho, linhas ocupam menos altura, Restaurar fica direto e exclusão permanente migrou para o menu de ações do item.
- Favoritos passa a abrir sempre em lista compacta, com cabeçalho menor, evitando cards exagerados para nomes longos.
- A análise de armazenamento deixa explícito que percentuais por categoria consideram somente os arquivos acessíveis analisados.
- A Lixeira aparece como bloco próprio na análise de armazenamento, com quantidade e tamanho, e não entra nos rankings normais de pastas/arquivos.
- Diálogo de progresso foi compactado, usa o ícone correto de **Mover**, mostra contagem pluralizada e força 100% quando a operação já atingiu o total.
- Grade aproximada do visual XP com cantos menos arredondados; barra inferior deixa de exibir `0 B` quando não há tamanho útil.
- Card de armazenamento da tela inicial ficou mais baixo e direto, liberando mais área para a lista sem perder porcentagem, usados/livres e acesso a Analisar.
- Mantido o conjunto de imagens/ícones da alpha.31 sem criar, gerar ou substituir recursos visuais.
- Versão sincronizada para `0.1.0-alpha.32` / `versionCode 32`.

## 0.1.0-alpha.31

- Continuado o polimento visual sem trocar o pacote de ícones XP e sem adicionar imagens ou mockups.
- A barra de menus superior ganhou texto ligeiramente maior e rolagem horizontal em telas estreitas, evitando compressão de Arquivo/Editar/Exibir/Favoritos/Ferramentas/Ajuda.
- O modo de seleção deixou de espremer sete ações na largura da tela: a toolbar contextual agora usa botões com largura estável e rolagem horizontal, melhorando leitura e toque.
- O card de armazenamento foi redesenhado para nunca esconder os rótulos quando o volume está muito vazio ou muito cheio; agora mostra usado/total/livre, porcentagem e uma barra separada com acesso claro a **Analisar**.
- A listagem passou a separar **tipo + tamanho** da **data + hora**, aumentando a identificação visual do arquivo sem depender apenas da extensão ou do ícone.
- A barra inferior agora informa também quantas pastas e arquivos existem no diretório atual.
- Janelas XP receberam botão Fechar vermelho no padrão das janelas clássicas, com título protegido contra corte.
- A Lixeira teve o cabeçalho reorganizado para não apertar informações e ações em telas estreitas; **Atualizar** e **Esvaziar Lixeira** ficam em uma faixa própria.
- A análise de armazenamento agora usa percentuais reais dentro do total categorizado em vez de barras relativas apenas à maior categoria, tornando a comparação mais intuitiva.
- O visualizador interno passou a mostrar diretamente **tipo do arquivo + tamanho** na toolbar, removendo o texto redundante “Visualizador interno”.
- Versão sincronizada para `0.1.0-alpha.31` / `versionCode 31`.

## 0.1.0-alpha.30

- Implementada **Lixeira real** integrada ao fluxo de exclusão: ao apagar arquivo ou pasta, o usuário escolhe entre **Mover para a Lixeira**, **Apagar permanentemente** ou cancelar.
- A Lixeira usa os ícones XP já existentes `trash_empty`/`trash_full`, lista itens com tipo, tamanho, data da exclusão e caminho original, e permite **Restaurar**, apagar definitivamente, atualizar e **Esvaziar Lixeira**.
- Restauração recria o diretório original quando necessário e evita sobrescrever arquivos existentes, criando um nome com sufixo `(restaurado)` em caso de conflito.
- A toolbar principal foi reorganizada: a ação duplicada **Exibir** saiu da barra e deu lugar à **Lixeira**; Lista/Grade continuam disponíveis no menu Exibir. A Lixeira também pode ser aberta pelo menu Arquivo.
- Identificação de arquivos foi centralizada em `FileTypeClassifier`, com descrições legíveis como **Imagem JPEG**, **Documento PDF**, **Vídeo MP4**, **Arquivo ZIP** e **Aplicativo Android (APK)**.
- Lista, grade, barra de status, Lixeira e visualizadores internos agora reutilizam a mesma classificação de tipo; a grade mostra tipo + tamanho e a lista prioriza tipo, tamanho e data.
- Barra de status foi refinada para mostrar quantidade/tamanho dos itens e, em seleção única, o tipo do arquivo selecionado.
- O card de armazenamento passou a abrir uma tela detalhada com **espaço total, usado e livre**, distribuição por categorias, maiores pastas e arquivos grandes.
- A análise de armazenamento é iniciada somente sob demanda, executada em `Dispatchers.IO`, atualiza a quantidade de arquivos verificados, pode ser cancelada e ignora a pasta interna gerenciada pela Lixeira.
- Pastas e arquivos exibidos nos resultados da análise podem ser abertos no próprio Explorer, evitando criar outro navegador ou duplicar funções.
- **Sobre o Explorador XP** agora reúne informações do app, versão, desenvolvedor, chave PIX com botão de copiar e resumo das novidades da versão; o diálogo separado de Doação foi removido.
- **Ajuda** foi reorganizada em tópicos expansíveis para Navegação, Arquivos e pastas, Copiar e mover, Lixeira, Armazenamento, Pesquisa, Favoritos, Visualizadores e Arquivos ocultos.
- Visualizador de imagens agora diferencia arquivo ausente/sem acesso, arquivo vazio ou corrompido e formato não decodificado pelo Android, oferecendo **Abrir com outro aplicativo** quando a leitura interna falhar.
- Refinados tamanhos e alinhamentos: legenda da toolbar ligeiramente maior, textos das ações de seleção mais claros, detalhes da grade podem ocupar duas linhas e ações da Lixeira foram reorganizadas para não esmagar conteúdo em telas estreitas.
- Nenhuma imagem ou mockup novo foi criado/adicionado; a atualização reutiliza exclusivamente os recursos visuais já presentes no projeto.
- Versão sincronizada para `0.1.0-alpha.30` / `versionCode 30`.

## 0.1.0-alpha.29

- Aplicado um pacote amplo de polimento visual baseado no vídeo de uso real da alpha.28, preservando o pacote de ícones XP restaurado e todas as funções existentes.
- Barra superior reorganizada para ocupar menos espaço: menus clássicos mais compactos, barra contextual exclusiva durante seleção e modo de pesquisa focado quando o teclado está em uso.
- Barra de endereço transformada em breadcrumb navegável, permitindo voltar diretamente para cada pasta do caminho; seletor de armazenamento permanece disponível no final da barra.
- Grade aumentada de 96 dp para 112 dp adaptativos, com bordas mais discretas, nomes centralizados, seleção reforçada e botão de opções sobreposto no canto para liberar espaço.
- Lista e grade agora usam miniaturas reais de imagens e vídeos quando disponíveis, carregadas em `Dispatchers.IO`, com no máximo duas decodificações simultâneas e cache LRU de 18 MiB; os PNGs XP continuam como fallback.
- Estados vazios ganharam hierarquia visual e instruções contextuais, especialmente em Favoritos e resultados de pesquisa.
- Barra de status inferior ficou mais legível e passa a mostrar quantidade de itens, quantidade selecionada e tamanho total/selecionado.
- Substituídos os diálogos Material de criar pasta, renomear, excluir, propriedades e permissão inicial por janelas no padrão visual XP, com botões clássicos e menor arredondamento.
- Menus Arquivo/Editar/Exibir/Favoritos/Ferramentas/Ajuda receberam linhas compactas, feedback de toque e submenus mais próximos do comportamento clássico do Windows.
- Visualizadores internos foram padronizados com barra de título XP, toolbar e status inferior comuns.
- Visualizador ZIP agora exibe nome, caminho, ícone XP, tamanho/tipo e ação de extração em layout semelhante ao Explorer.
- Editor de texto/código mantém fonte monoespaçada, diferencia leitura/edição, sinaliza alterações não salvas e mostra linha, coluna, quantidade de linhas/caracteres e UTF-8.
- Visualizador APK passa a tentar ler o nome e o ícone reais do aplicativo, pacote, versão, versionCode, minSdk, targetSdk, tamanho e se o pacote já está instalado.
- Adicionado suporte de visualização interna para vídeos `.mov`, mantendo abertura externa como alternativa.
- Tela Informações do dispositivo preserva seus cards modernos, sensores e relatórios, mas agora usa moldura menos arredondada e cabeçalho com gradiente XP para ficar mais integrada ao restante do aplicativo.
- Adicionados estados pressionados discretos aos principais botões clássicos para melhorar o feedback de toque sem introduzir animações pesadas.
- Versão sincronizada para `0.1.0-alpha.29` / `versionCode 29`.

## 0.1.0-alpha.28

- Revertida integralmente a experiência de ícones vetoriais da alpha.27 após teste no aparelho não mostrar ganho perceptível de desempenho e o visual anterior ser preferido.
- Restaurado o pacote visual da alpha.26/alpha.23: ícones PNG XP otimizados em `drawable-xxxhdpi`, variantes grandes apenas onde necessário e `CachedResourceIcon` para decodificação em background.
- Restaurados `FileIconMapper`, lista, grade, menus, toolbar e estados visuais para o comportamento anterior à alpha.27.
- Removidos do estado atual os ícones genéricos por categoria/etiqueta dinâmica e a tentativa de usar ícone real de APK introduzidos na alpha.27.
- Mantidas todas as funções da alpha.26: Conectividade, sensores, CPU ampliada, seções expansíveis, relatório para IA schema 3, copiar/salvar/compartilhar ficha do dispositivo e demais otimizações de desempenho anteriores.
- Nenhuma alteração de dados do usuário ou formato de armazenamento foi feita; a reversão é apenas visual/recursos de ícones e código associado.
- Versão sincronizada para `0.1.0-alpha.28` / `versionCode 28`.

## 0.1.0-alpha.27

- Substituído o pacote de 150 PNGs de tipos de arquivo e ações por um pipeline vetorial no caminho principal do Explorer.
- `FileIconMapper` agora classifica arquivos por categoria e fornece etiqueta dinâmica de extensão (`PDF`, `DOCX`, `ZIP`, `MP3`, `JSON` etc.), evitando um bitmap diferente para cada formato.
- Lista, grade, menus de contexto, propriedades, toolbar, armazenamento e estados vazios usam `ImageVector`/Material Icons, eliminando a decodificação dos antigos PNGs durante a rolagem.
- Arquivos APK tentam exibir o ícone real do aplicativo via `PackageManager`; a leitura acontece em `Dispatchers.IO`, limitada a uma por vez e com cache LRU de 4 MiB.
- Removido `CachedResourceIcon.kt` e o cache de bitmaps de recursos, que deixaram de ser necessários.
- Removido `drawable-xxxhdpi` com os 150 PNGs antigos e as variantes grandes `file_apk_large`, `folder_open_large` e `search_large`; somente o launcher mantém PNG dedicado.
- Recursos brutos em `app/src/main/res` caíram de aproximadamente 3,97 MB / 175 arquivos para 0,85 MB / 22 arquivos antes da otimização final do APK.
- Adicionados testes unitários para o novo mapeamento de categorias e etiquetas de extensão.
- Versão sincronizada para `0.1.0-alpha.27` / `versionCode 27`.

## 0.1.0-alpha.26

- Adicionada a seção **Conectividade** em Informações do dispositivo, usando dados reais expostos pelo Android.
- A tela agora identifica conexão atual (Wi‑Fi, rede móvel, Ethernet, VPN ou offline), validação de internet e VPN ativa.
- Quando disponível, o Wi‑Fi mostra banda (2,4/5/6/60 GHz), padrão conectado (Wi‑Fi 4/5/6/7) e velocidade do link, sem coletar SSID, BSSID ou endereço MAC.
- Rede móvel passa a mostrar 2G/3G/4G-LTE/5G quando o Android libera a informação, além da operadora quando disponível.
- Adicionada leitura básica de quantidade de slots SIM, SIMs prontos e suporte a eSIM e suporte a múltiplos perfis eUICC, sem coletar número de telefone, IMEI, IMSI ou ICCID.
- O processador ganhou arquitetura principal, frequências máximas expostas por núcleo/grupo e identificador de hardware, além de fabricante/modelo do SoC e número de núcleos.
- As setas de Sistema, Conectividade, Bateria, Recursos e Sensores agora são funcionais: tocar no cabeçalho expande ou recolhe a seção.
- A grade de sensores passou para **3 colunas**, reduzindo bastante a altura da seção sem esconder sensores; nomes maiores podem ocupar duas linhas.
- O relatório para IA foi atualizado para `schema_version=3` com blocos de conectividade e CPU ampliados; o resumo e a ficha PNG também incluem as informações principais.
- Adicionadas apenas permissões normais de estado de rede/Wi‑Fi e estado básico do telefone; nenhuma permissão de localização ou leitura de identificadores pessoais foi introduzida.
- Versão sincronizada para `0.1.0-alpha.26` / `versionCode 26`.

## 0.1.0-alpha.25

- Corrigido o rodapé da tela **Informações do dispositivo** para respeitar a barra de navegação do Android e permitir rolar o botão **Exportar relatório para IA** completamente acima dos controles do sistema.
- Adicionado espaço final extra no conteúdo rolável para evitar corte em aparelhos com navegação por três botões ou barra transparente.
- A detecção de sensores passou a usar `SensorManager`, lendo os sensores realmente expostos pelo aparelho.
- Nova seção **Sensores** com acelerômetro, giroscópio, magnetômetro/bússola, luz ambiente, proximidade, barômetro, contador/detector de passos, gravidade, aceleração linear, vetor de rotação, temperatura ambiente e umidade.
- O relatório para IA foi atualizado para `schema_version=2` e ganhou seção `[sensors]` com os novos estados.
- Adicionado **Copiar resumo**, que envia ao clipboard apenas um resumo legível das principais especificações.
- Adicionado **Salvar PNG**, que gera localmente uma ficha visual do aparelho sem capturar a tela e sem incluir identificadores sensíveis.
- Adicionado **Compartilhar imagem**, que gera a ficha em cache e abre o compartilhamento padrão do Android via `FileProvider`.
- A imagem compartilhável inclui sistema, CPU, RAM, armazenamento, tela, bateria e recursos/sensores, mas exclui IMEI, serial, Android ID, MAC, localização e arquivos pessoais.
- Ajustado o cabeçalho da tela para ocupar menos largura em aparelhos estreitos.
- Versão sincronizada para `0.1.0-alpha.25` / `versionCode 25`.

## 0.1.0-alpha.24

- Redesenhada diretamente em Jetpack Compose a tela **Informações do dispositivo**, usando a referência visual apenas como guia e sem adicionar imagens/fotos falsas do aparelho.
- Novo cabeçalho azul com ações de atualizar e fechar integradas, removendo o rodapé separado de atualização.
- Novo cartão principal do dispositivo com nome, fabricante/modelo, estado ativo e atalhos visuais para Android, RAM e armazenamento.
- Memória, armazenamento e bateria agora usam cartões compactos com ícones vetoriais, cores próprias e barras de progresso.
- Seções Sistema, Bateria e Recursos receberam hierarquia visual mais forte, divisores e ícones vetoriais leves.
- Recursos passaram a exibir claramente `Disponível` / `Não disponível`, mantendo somente dados reais coletados do Android.
- Adicionado destaque contextual quando o aparelho está conectado à energia.
- Cartão do Explorador XP e área **Relatório para IA** foram modernizados, mantendo o mesmo mecanismo de exportação e as mesmas regras de privacidade.
- Nenhuma imagem nova foi incorporada à tela; o redesign usa componentes Compose e Material Icons.
- Versão sincronizada para `0.1.0-alpha.24` / `versionCode 24`.

## 0.1.0-alpha.23

- Otimizado o pipeline de ícones que aparece durante a rolagem de arquivos e pastas.
- Os 150 PNGs comuns de 256×256 em `drawable-nodpi` foram convertidos para 192×192 em `drawable-xxxhdpi`, permitindo que o Android aplique a densidade correta e decodifique bitmaps menores conforme o aparelho.
- Adicionado `CachedResourceIcon`, com decodificação de PNG em `Dispatchers.IO` e cache LRU limitado a 6 MiB para retirar a primeira decodificação dos ícones da thread principal.
- Lista e grade agora usam o cache assíncrono para os ícones de cada `FileItem` e aquecem, em background, os tipos presentes nos primeiros itens da pasta.
- Mantido o visual original dos ícones; não foram trocados por desenhos genéricos ou vetores diferentes.
- Criadas variantes grandes separadas para `file_apk`, `folder_open` e `search`, preservando qualidade nas telas em que esses recursos aparecem com 72–86 dp sem obrigar a lista a carregar bitmaps grandes.
- O conjunto comprimido dos 150 ícones comuns caiu de aproximadamente 3,84 MiB para 3,00 MiB, antes da compactação final do APK.
- Corrigido o `DeviceInfoReportTest` para acompanhar o `versionCode` atual.
- Versão sincronizada para `0.1.0-alpha.23` / `versionCode 23`.

## 0.1.0-alpha.22

- Aplicado novo ícone do aplicativo ao projeto, substituindo o launcher anterior por uma pasta dourada com órbita azul.
- A arte do launcher recebeu margem de segurança extra para evitar corte do símbolo nas máscaras adaptativas do Android.
- O foreground do ícone passou a usar fundo transparente no adaptive icon, eliminando a borda/quadriculado escuro visto fora da arte.
- Atualizados `ic_launcher`, `ic_launcher_round` e `launcher_explorer_foreground.png` em todas as densidades.
- Ajustados `ic_launcher.xml` e `ic_launcher_round.xml` para usar fundo transparente e inset de segurança no foreground.
- Mantido o ícone monocromático separado para compatibilidade com themed icons do Android 13+.
- Versão sincronizada para `0.1.0-alpha.22` / `versionCode 22`.

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
