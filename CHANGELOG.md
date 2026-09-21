# CHANGELOG

## 0.1.0-alpha.62

- A quebra automática de linha deixa de ficar escondida apenas nas configurações: agora há ação direta **Quebra linha** na barra do editor/visualizador e a mesma opção também aparece em **Mais**.
- O estado da quebra de linha continua persistente entre arquivos e funciona também em modo somente leitura, eliminando a necessidade de rolagem horizontal quando ativado.
- Arquivos de texto/código recebidos por **Abrir com** passam a permitir edição sobre a cópia segura do cache.
- Quando o aplicativo de origem concede `FLAG_GRANT_WRITE_URI_PERMISSION`, a ação **Salvar original** grava explicitamente de volta na URI recebida; sem essa permissão, o botão não tenta sobrescrever a origem.
- **Salvar como** para arquivos recebidos externamente passa a usar o seletor `CreateDocument` do Android, evitando salvar uma cópia dentro do cache temporário que seria apagado ao fechar.
- A versão original do arquivo externo recebe um SHA-256 ao ser aberta. Antes de **Salvar original**, o conteúdo atual da URI é comparado com esse hash; se outro aplicativo alterou o arquivo, o Explorador XP interrompe a sobrescrita e orienta usar **Salvar como**.
- A gravação em `content://` valida o SHA-256 após escrever e tenta restaurar uma cópia curta do conteúdo anterior se o provedor falhar durante a sobrescrita.
- Permissões persistíveis oferecidas pelo provedor são conservadas quando disponíveis, sem alterar a identidade do aplicativo.
- Mantidos destaque de sintaxe, autoindentação, pares automáticos, números de linha, localizar/substituir, desfazer/refazer, UTF-8 e proteções para arquivos grandes da alpha.61.
- Nenhuma imagem ou mockup foi criada ou modificada.
- README, tela Sobre, documentação, validações e `github-manager.json` atualizados.
- Versão sincronizada para `0.1.0-alpha.62` / `versionCode 62`.

## 0.1.0-alpha.61

- Corrigida falha de compilação no inspetor de APK: `getPermissionInfo()` volta a usar o overload compatível com `Int`, removendo a referência inválida a `PackageManager.PermissionInfoFlags`.
- O editor interno recebeu destaque de sintaxe ampliado para HTML/XML, CSS, JavaScript/TypeScript, JSON, Markdown, YAML, TOML/INI/properties/.env, Kotlin/Java/Gradle, Python, Shell, BAT/CMD, PowerShell, PHP, Ruby, C/C++, Go, Rust, Swift, Dart, SQL, Vue/Svelte, TeX e CSV.
- Adicionadas autoindentação e continuação inteligente após blocos, inclusive para Enter vindo do teclado virtual, além de fechamento automático de `()`, `[]`, `{}`, aspas simples e duplas.
- Nova configuração persistente do editor permite escolher TAB ou espaços (2/4/8) e ativar/desativar quebra automática de linha.
- Números de linha passam a usar um índice incremental, com gutter dimensionado pela quantidade de linhas e numeração correta mesmo com quebra visual; linha/coluna também deixam de depender de varredura completa a cada tecla.
- Localizar/substituir agora mostra total de ocorrências e posição atual, navega anterior/próxima com retorno circular e preserva substituições literais como `$` e `\`.
- Desfazer/refazer foi preservado e o estado “não salvo” passou a usar IDs de revisão independentes do limite do histórico, evitando falsos estados de salvo após muitas edições.
- Preview web mantém o editor vivo por baixo, preservando histórico, seleção lógica e alterações ainda não salvas ao alternar Código/Preview/tela cheia.
- Arquivos maiores continuam protegidos; destaque completo é limitado em documentos grandes, regex de sintaxe são reutilizados em cache e o histórico passa a ter orçamento de memória além do limite de operações, mantendo a edição responsiva sem remover desfazer/refazer da operação mais recente.
- O fluxo **Abrir com** continua copiando para cache e sem “Salvar de volta” nesta etapa, mas agora mantém URI, MIME, nome e permissões da origem em uma estrutura separada, preparando a próxima evolução com segurança.
- Nenhuma imagem ou mockup foi criada ou modificada.
- README, tela Sobre, documentação, validações e `github-manager.json` atualizados.
- Versão sincronizada para `0.1.0-alpha.61` / `versionCode 61`.

## 0.1.0-alpha.60

- Renovado o visualizador/instalador de APK para fazer uma inspeção mais completa antes de chamar o instalador do Android.
- Comparação entre **versão do APK** e **versão instalada** agora mostra versão e `versionCode`, distinguindo instalação nova, atualização, reinstalação e downgrade.
- Adicionada verificação de compatibilidade com o Android atual usando `minSdk`, além de leitura das arquiteturas nativas (`lib/<ABI>/*.so`) do APK e comparação com as ABIs suportadas pelo aparelho.
- Assinaturas do APK e do aplicativo instalado passam a ser lidas com `GET_SIGNING_CERTIFICATES`/fallback legado e exibidas como SHA-256; atualizações com assinatura incompatível são sinalizadas antes da tentativa de instalação.
- O painel agora lista as permissões declaradas pelo APK, usando os rótulos do Android quando disponíveis e destacando permissões classificadas como perigosas pelo sistema.
- Criadas seções de **Versões**, **Compatibilidade e segurança**, **Detalhes técnicos**, **Permissões solicitadas** e **Ações**, mantendo o visual do Explorador XP.
- O botão de instalar fica indisponível quando o APK é claramente incompatível com a versão do Android, ABI do aparelho, assinatura do pacote instalado ou quando representa downgrade direto; **Gerenciar app** abre a tela do Android para o usuário decidir sobre a versão instalada.
- A autorização **Permitir instalação nesta fonte** ganhou ação própria; se a instalação tiver sido iniciada antes de conceder a permissão, ela continua automaticamente ao voltar para o Explorador XP.
- Ao voltar de **Gerenciar app**, o painel é reanalisado para refletir instalação/desinstalação ou outras mudanças no pacote.
- Adicionados `ApkInspector.kt`, `ApkInspectorSupport.kt` e testes JVM para relação de versões, assinatura, ABIs e SHA-256.
- Nenhuma imagem ou mockup foi criada ou modificada.
- README, tela Sobre, documentação, validações e `github-manager.json` atualizados.
- Versão sincronizada para `0.1.0-alpha.60` / `versionCode 60`.

## 0.1.0-alpha.59

- O visualizador ZIP passa a montar um índice hierárquico reutilizável ao abrir o arquivo, incluindo pastas implícitas que não possuem uma entrada de diretório própria.
- Pastas internas agora mostram tamanho total recursivo, tamanho compactado acumulado, quantidade de arquivos e subpastas; ordenação por tamanho/data usa esses valores agregados.
- Corrigida a ordenação decrescente do ZIP para manter pastas antes dos arquivos, em vez de inverter também os grupos.
- O caminho `ZIP:/...` virou breadcrumb clicável para retornar diretamente a qualquer nível.
- A pesquisa em todo o ZIP passa a mostrar o caminho de cada resultado, reduzindo ambiguidade quando há nomes repetidos.
- Seleção ganhou **Selecionar tudo**, **Tudo**, **Inverter** e **Limpar**, além do tamanho real que será extraído.
- A barra inferior do ZIP mostra resumo do nível atual (arquivos, pastas e tamanho) ou quantidade de resultados da pesquisa.
- Informações de pasta exibem estatísticas recursivas; arquivos passam a mostrar também a taxa de compressão quando disponível.
- Pré-visualização de itens grandes agora mostra progresso, pode ser cancelada e usa cache temporário estável por ZIP/entrada; arquivos parciais só são promovidos ao cache após escrita completa.
- Adicionado `ArchiveBrowserIndex.kt` e teste JVM para pastas implícitas, estatísticas recursivas, pesquisa e ordenação com pastas primeiro.
- Nenhuma imagem ou mockup foi criada ou modificada.
- README, tela Sobre, documentação, validações e `github-manager.json` atualizados.
- Versão sincronizada para `0.1.0-alpha.59` / `versionCode 59`.

## 0.1.0-alpha.58

- A janela de operações agora oferece **Pausar** e **Continuar** sem cancelar ou reiniciar a tarefa em andamento.
- A pausa é cooperativa: o planejador de árvores verifica checkpoints durante a enumeração, cópias verificam a pausa entre blocos de 256 KiB e exclusões verificam antes de cada item removido.
- Copiar, mover, mover para a Lixeira, restaurar, apagar permanentemente e esvaziar a Lixeira compartilham o mesmo mecanismo de pausa.
- Ao continuar, a operação retoma do ponto já processado; arquivos e bytes concluídos não são refeitos.
- O cálculo de velocidade e tempo restante passa a descontar o período pausado, evitando que a estimativa fique artificialmente lenta após uma pausa longa.
- **Cancelar** continua disponível durante a pausa e encerra também corrotinas paradas no gate de espera.
- Adicionado teste de regressão para confirmar que uma cópia grande realmente para entre blocos e continua até produzir arquivo idêntico.
- README, tela Sobre, documentação, validações e `github-manager.json` atualizados.
- Versão sincronizada para `0.1.0-alpha.58` / `versionCode 58`.

## 0.1.0-alpha.57

- Próxima etapa das operações de arquivos: cópia, movimentação, exclusão e Lixeira passam a transportar progresso por **bytes**, além da contagem de entradas já existente.
- A janela de transferência mostra quantidade processada, porcentagem e, em cópia/movimentação, **velocidade média** e **estimativa de tempo restante**.
- O copiador de `FileOperationPlan` relata bytes a cada bloco de 256 KiB sem reler o arquivo apenas para calcular progresso; cancelamento continua sendo verificado entre blocos.
- Ao colar em um destino que já possui item com o mesmo nome, o Explorador XP agora pergunta entre **Substituir**, **Ignorar** e **Manter ambos**, com opção **Aplicar esta escolha a todos os próximos conflitos**.
- Copiar um item para a própria pasta protege a origem: a opção destrutiva não é aplicada ao próprio arquivo e o app cria automaticamente uma cópia com nome único.
- O visualizador interno de imagens vira uma galeria da pasta atual: botões **Anterior/Próxima**, contador e gesto horizontal permitem trocar de foto sem fechar o visualizador.
- A galeria recebe o snapshot que já estava carregado pelo Explorer, não faz busca global nem percorre subpastas; somente imagens da pasta aberta entram na sequência, respeitando ordenação e visibilidade de ocultos atuais.
- Imagens abertas por `Abrir com`, Favoritos, ZIP ou atalhos fora da pasta atual continuam isoladas, evitando misturar arquivos de outros locais.
- README, tela Sobre, documentação, validações e `github-manager.json` atualizados.
- Versão sincronizada para `0.1.0-alpha.57` / `versionCode 57`.

## 0.1.0-alpha.56

- Terceira etapa da otimização estrutural de desempenho, focada em **recomposição do Compose, metadados e análise de armazenamento**.
- `TransferState`, `StorageScanState` e o estado da Lixeira foram retirados do `ExplorerUiState` principal e expostos em `StateFlow`s independentes; atualizações frequentes de progresso deixam de invalidar a árvore inteira da tela principal.
- Hosts pequenos coletam esses fluxos somente onde são necessários: diálogo de transferência, janela de armazenamento, Lixeira e indicador do cabeçalho.
- `FileItem` ganha cache LRU por caminho/assinatura para reutilizar ícone, classificação e textos formatados quando tipo, tamanho, data e favorito não mudaram.
- Removida a consulta extra `File.isHidden` por item; no armazenamento Android/Linux o prefixo `.` já representa o caso relevante para a navegação do app.
- `LazyColumn` e `LazyVerticalGrid` passam `contentType` para pastas/arquivos, ajudando o Compose a reutilizar composição de células compatíveis.
- A barra inferior indexa os itens por caminho uma vez; selecionar/desmarcar deixa de filtrar toda a pasta em cada toque e a contagem direta passa a usar uma única travessia da lista.
- A análise de armazenamento reaproveita `treeSize` persistido pelas novas entradas da Lixeira, evitando uma travessia recursiva adicional ao final da análise. Entradas antigas mantêm fallback compatível.
- Estados de UI de alta frequência foram marcados como imutáveis para melhorar a capacidade de skipping do Compose.
- README, CHANGELOG, validações, tela Sobre e `github-manager.json` atualizados.
- Versão sincronizada para `0.1.0-alpha.56` / `versionCode 56`.

## 0.1.0-alpha.55

- Corrigida a centralização vertical das janelas modais: o `Dialog` do Android já entrega uma área útil ajustada às barras do sistema, e o app reaplicava `safeDrawingPadding()`, criando um segundo inset e deslocando visualmente as janelas para baixo.
- **Ajuda, Sobre, Lixeira, Armazenamento e Informações do dispositivo** agora usam padding externo simétrico e ficam centralizadas na área útil real do aplicativo.
- A mesma correção de inset duplicado foi aplicada aos diálogos menores de transferência, menus contextuais, editor de texto/código e operações de ZIP para manter comportamento consistente.
- Confirmado que o corte de **Movies, Music e Pictures/DCIM** não era apenas de `ContentScale`: os próprios PNGs `folder_videos`, `folder_music` e `folder_images` continham o elemento sobreposto truncado na borda.
- Esses três recursos foram reconstruídos usando somente o `folder.png` e os glifos XP já existentes (`videos.png`, `music.png`, `pictures.png`), preservando identidade, proporção e margem sem gerar arte nova.
- Mantido `ContentScale.Fit` e a área segura interna do `FileVisual`, agora trabalhando com recursos que possuem conteúdo completo.
- README, CHANGELOG, validações e `github-manager.json` atualizados.
- Versão sincronizada para `0.1.0-alpha.55` / `versionCode 55`.

## 0.1.0-alpha.54

- Segunda etapa da otimização estrutural de desempenho, focada em **copiar, mover, excluir e Lixeira**.
- Criado um plano iterativo reutilizável de operação (`FileOperationPlan`): cada árvore de arquivos é enumerada uma única vez e a mesma lista planejada é usada para cópia/exclusão, evitando a antiga sequência `countEntries()` + nova travessia recursiva.
- Cópia/recorte com fallback, exclusão permanente, mover para Lixeira, restaurar, apagar da Lixeira e esvaziar Lixeira deixam de chamar `listFiles()` repetidamente sobre a mesma árvore apenas para progresso.
- Movimentações que conseguem `renameTo()` continuam rápidas e agora reaproveitam a contagem já obtida no plano, sem uma segunda contagem da pasta após renomear.
- O copiador passa a usar buffer de 256 KiB e checagens periódicas de cancelamento, reduzindo overhead em arquivos maiores sem perder a opção **Cancelar**.
- Novas entradas da Lixeira gravam `treeSize` e `entryCount` no metadado; ao reabrir a Lixeira, o tamanho dessas entradas pode ser mostrado sem recalcular recursivamente toda a pasta. Entradas antigas continuam compatíveis pelo fallback existente.
- A exclusão planejada é iterativa e processa filhos antes dos pais, reduzindo também o risco de stack profundo em árvores muito aninhadas.
- Adicionado teste JVM para planejamento, cópia e exclusão da árvore sem alterar imagens, mockups ou recursos visuais.
- README, CHANGELOG, documentação de desempenho, validações e `github-manager.json` atualizados.
- Versão sincronizada para `0.1.0-alpha.54` / `versionCode 54`.

## 0.1.0-alpha.53

- Iniciada a primeira etapa da grande melhoria de desempenho, priorizando navegação e rolagem antes das próximas evoluções funcionais.
- Listagem de diretórios muito grandes agora verifica cancelamento em lotes, evitando que uma pasta anterior continue lendo metadados depois que o usuário já navegou para outro local.
- Voltar/Avançar reaproveita o snapshot LRU imediatamente e adia por 220 ms a releitura em background; navegações rápidas cancelam essa releitura antes de gerar I/O desnecessário.
- Estatísticas recursivas da barra inferior continuam somando arquivos, subpastas e tamanho, mas agora usam cache curto LRU, scanner único e atraso de baixa prioridade para não competir com a abertura da pasta.
- Miniaturas de imagens/vídeos deixam de iniciar decodificação enquanto lista/grade está rolando; vídeos recebem um pequeno atraso adicional e trabalhos são cancelados quando a rolagem recomeça.
- Ícones XP de arquivos/pastas passam a usar uma área segura interna mantendo proporção e `ContentScale.Fit`, corrigindo o corte visual mais perceptível em **Movies** e **Music** sem trocar os recursos originais.
- Corrigido o botão **Instalar/Atualizar/Reinstalar APK**: o fluxo principal usa `ACTION_INSTALL_PACKAGE`, que não é capturado pelo `ACTION_VIEW` registrado para **Abrir com**. Há fallback explícito para um instalador externo em ROMs que não expõem a ação específica.
- Tela **Sobre**, README, CHANGELOG e `github-manager.json` atualizados; assinatura permanente e `applicationId` preservados.
- Versão sincronizada para `0.1.0-alpha.53` / `versionCode 53`.

## 0.1.0-alpha.52

- Criado um contêiner modal reutilizável para as janelas grandes do Explorador XP, com centralização real dentro da área segura, largura limitada e altura proporcional à tela.
- **Ajuda**, **Sobre**, **Lixeira**, **Armazenamento** e **Informações do dispositivo** deixam de ocupar quase toda a altura disponível e passam a manter respiro visível acima e abaixo.
- Cabeçalho e rodapé permanecem fixos enquanto somente o conteúdo central rola, evitando a sensação de painel sem limite inferior.
- Adicionados divisores entre cabeçalho, conteúdo e rodapé, além de ação **Fechar** também no rodapé das janelas principais.
- A tela **Sobre** teve os atalhos movidos para o rodapé fixo e as novidades atualizadas para refletir a padronização visual.
- Nenhuma função foi removida; Lixeira, análise de armazenamento, exportação/compartilhamento de informações e demais ações continuam preservadas.
- Versão sincronizada para `0.1.0-alpha.52` / `versionCode 52`.

## 0.1.0-alpha.51

- Integrado o Explorador XP ao seletor **Abrir com** do Android por `ACTION_VIEW`, sem registrar `*/*` ou `application/octet-stream`.
- Registrados somente MIME types realmente atendidos pelos leitores internos: texto/código, ZIP, PDF, APK, imagens, áudio e vídeo suportados.
- Adicionado recebimento de URIs `content://`/`file://`, resolução do nome real via `OpenableColumns` e cópia segura para cache privado antes de abrir no visualizador baseado em `File`.
- Arquivos externos podem abrir mesmo antes da permissão global de armazenamento; ao fechar, o cache temporário da abertura é removido.
- Durante a preparação de uma URI externa é exibido **Abrindo arquivo...** sem solicitar `MANAGE_EXTERNAL_STORAGE`; ao fechar o leitor externo, o app retorna ao aplicativo de origem.
- Textos recebidos externamente são abertos em modo somente leitura, evitando que o usuário edite uma cópia temporária acreditando estar alterando o arquivo original.
- `launchMode=singleTop` e `onNewIntent()` permitem abrir outro arquivo compatível quando o Explorador XP já está no topo.
- Adicionada validação compartilhada de formatos externos e teste JVM para aceitar ZIP/Markdown/JSON/mídia compatível e rejeitar RAR/SVG/binários ainda sem leitor interno.
- Versão sincronizada para `0.1.0-alpha.51` / `versionCode 51`.

## 0.1.0-alpha.50

- Corrigida a falha ao abrir arquivos Markdown e outros textos no editor interno, exibida como `Attempt to invoke interface method ... on a null object reference`.
- A causa era um callback Kotlin do `CodeEditText` acessado por `onSelectionChanged()` durante a construção do `EditText`, antes da inicialização dos campos da subclasse; em builds minificados a interface aparecia com nome obfuscado.
- Callbacks de texto/histórico agora são anuláveis durante a construção e só são invocados com segurança após `configure()`, evitando o fallback de erro sem alterar os recursos do editor.
- Versão sincronizada para `0.1.0-alpha.50` / `versionCode 50`.

## 0.1.0-alpha.49

- Visualizador de APK redesenhado com card de identidade do aplicativo, status de instalação mais claro e painel de informações com separadores.
- Ações principais deixaram de ficar empilhadas: **Instalar/Reinstalar/Atualizar** e **Abrir app** aparecem lado a lado quando aplicável.
- Botões do APK ganharam ícones e hierarquia visual de ação primária/secundária.
- O fluxo de **Permitir desta fonte** agora mantém uma instalação pendente e, ao retornar da tela do Android com a permissão concedida, abre automaticamente o instalador sem exigir novo toque.
- Adicionado observador de ciclo de vida como fallback para continuar a instalação mesmo quando a tela de Configurações não devolve um resultado confiável.
- O Android não permite que o app feche à força a tela de **Instalar apps desconhecidos** no exato momento em que o switch é ativado; por isso a interface orienta usar **Voltar** uma única vez e continua sozinha.
- Assinatura permanente e `applicationId` preservados.
- Versão sincronizada para `0.1.0-alpha.49` / `versionCode 49`.

## 0.1.0-alpha.48

- Removido **Atualizar** da barra principal de ícones; a ação agora fica no menu **Exibir**, liberando espaço horizontal no topo.
- O indicador de armazenamento da barra inferior foi alargado e agora mostra **livre + percentual usado** em uma única linha, evitando corte na navegação Android.
- A informação da pasta na barra inferior passou a calcular em segundo plano e somar recursivamente **arquivos, subpastas e tamanho** de todo o conteúdo acessível dentro da pasta atual.
- Ao selecionar uma única pasta, a barra inferior também mostra arquivos internos, subpastas e tamanho total acessível.
- Corrigida a detecção de aplicativos já instalados no visualizador APK com visibilidade de pacotes apropriada no Android moderno.
- Adicionadas as permissões `REQUEST_INSTALL_PACKAGES` e `QUERY_ALL_PACKAGES` para permitir instalação solicitada pelo usuário e conferência confiável do pacote instalado.
- O botão do APK agora abre diretamente o instalador do Android; quando necessário, leva primeiro à tela **Instalar apps desconhecidos** e retorna ao fluxo de instalação.
- Quando o pacote já está instalado, o visualizador diferencia **Atualizar**, **Reinstalar** ou **Instalar esta versão** e oferece **Abrir aplicativo** quando houver atividade inicial.
- Ajuda e Sobre foram atualizados; assinatura permanente e `applicationId` foram preservados.
- Versão sincronizada para `0.1.0-alpha.48` / `versionCode 48`.

## 0.1.0-alpha.47

- Corrigido o erro de compilação em `ArchiveViewer.kt`: a faixa horizontal de ações do visualizador ZIP usava `horizontalScroll(...)` sem importar a extensão `androidx.compose.foundation.horizontalScroll`.
- As melhorias visuais de **Info**, **Verificar**, **Extrair** e **Ordenar** foram preservadas; nenhuma funcionalidade foi removida para contornar o erro.
- Mantidas as mudanças da alpha.46 na tela principal, barra inferior de armazenamento e ações rápidas de seleção.
- Assinatura permanente e `applicationId` preservados; versão sincronizada para `0.1.0-alpha.47` / `versionCode 47`.

## 0.1.0-alpha.46

- Removido o cartão grande de armazenamento da tela principal; espaço livre e percentual usado agora aparecem de forma compacta e clicável na barra inferior.
- Ao tocar no indicador de armazenamento, a análise detalhada continua disponível com total/usado/livre, categorias, pastas e arquivos grandes.
- A capacidade exibida passa a acompanhar o volume da pasta atual (armazenamento interno ou cartão SD), sem iniciar a análise pesada automaticamente.
- Barra principal ganhou atalhos **Atualizar** e **Novo**; **Novo** abre ações para **Nova pasta** e **Novo arquivo**.
- Implementada criação de arquivo vazio com validação de nome; arquivos de texto compatíveis podem abrir diretamente no editor após a criação.
- Barra de seleção agora exibe diretamente **Copiar**, **Mover**, **Renomear**, **Compartilhar**, **ZIP**, **Excluir** e **Mais**, reduzindo ações escondidas.
- Ajuda, Sobre e validações internas foram atualizados para refletir o novo fluxo.
- Assinatura permanente e `applicationId` preservados; versão sincronizada para `0.1.0-alpha.46` / `versionCode 46`.

## 0.1.0-alpha.45

- Visual do topo do visualizador ZIP refinado: os comandos **Info**, **Verificar**, **Extrair** e **Ordenar** agora usam botões com ícones no estilo XP, hierarquia visual melhor e leitura mais clara em telas estreitas.
- A navegação interna do ZIP ganhou um botão **Subir** com ícone dedicado e o estado da classificação fica visível ao lado do caminho atual.
- Abertura de arquivos de texto/HTML extraídos temporariamente do ZIP passou a usar um fluxo mais seguro, em **somente leitura**, reduzindo risco de travamento e edição acidental sobre arquivos de cache.
- O editor interno recebeu um fallback defensivo: se a inicialização do editor avançado falhar, o app mostra uma área de erro legível em vez de fechar abruptamente.
- Avisos mais claros foram adicionados quando o conteúdo aberto é uma pré-visualização temporária do ZIP.
- Assinatura permanente e `applicationId` foram preservados; nenhuma imagem/mockup foi criada ou alterada.
- Versão sincronizada para `0.1.0-alpha.45` / `versionCode 45`.

## 0.1.0-alpha.44

- Adicionada a ação **Compactar em ZIP** no menu **Mais** quando um ou vários arquivos/pastas estão selecionados.
- A compactação aceita seleção mista de arquivos e pastas, preserva a estrutura interna e permite informar nome e pasta de destino.
- O fluxo mostra progresso real com porcentagem, bytes, item atual e velocidade, além de permitir cancelar com limpeza do arquivo parcial.
- Quando a compactação termina, o usuário pode **Abrir ZIP** ou **Abrir pasta** diretamente pelo resumo final.
- Versão sincronizada para `0.1.0-alpha.44` / `versionCode 44`.

## 0.1.0-alpha.43

- Visualizador ZIP refeito para navegar por pastas internas em vez de uma lista plana; inclui Subir/Voltar, pesquisa e ordenação por nome, tamanho, tipo e data.
- Toque simples abre pasta ou visualiza arquivos compatíveis extraindo apenas o item para cache; toque longo seleciona itens e permite extração parcial.
- Tela de extração permite escolher pasta de destino, lembra o último destino usado em **Extrair para...**, cria pasta com o nome do ZIP e pode abrir o destino automaticamente ao terminar.
- Progresso de extração mostra porcentagem, itens concluídos, bytes processados, total, arquivo atual e velocidade; operações podem ser canceladas e o arquivo parcial em escrita é removido quando possível.
- Conflitos de nomes podem ser tratados por Renomear automaticamente, Substituir ou Ignorar, aplicados à operação inteira.
- Ao concluir, o resumo mostra extraídos, renomeados, ignorados e erros, com **Abrir pasta** e, quando somente um arquivo foi extraído, **Abrir arquivo**.
- Adicionado suporte real a ZIP protegido por senha (Zip Standard/AES suportados pela Zip4j) e verificação de integridade lendo cada entrada até o CRC.
- Proteção contra Zip Slip e caminhos absolutos/`..`; espaço necessário é comparado com o espaço livre antes da extração.
- Notificação de progresso é usada em extrações longas quando a permissão de notificações estiver concedida; a extração continua normalmente sem ela.
- Explorer principal ganha `Abrir com...`, `Extrair aqui` e `Extrair para...` no menu de arquivos ZIP; as mesmas ações aparecem no menu Mais ao selecionar um único ZIP.
- `Extrair aqui` cria por padrão uma pasta com o nome do ZIP e evita sobrescrever silenciosamente conteúdo existente.
- Adicionada dependência `net.lingala.zip4j:zip4j:2.11.5`, testes de segurança de caminho e nomes únicos, sem adicionar ou alterar imagens/mockups.
- Assinatura permanente introduzida na alpha.42 foi preservada; `applicationId` continua `com.exploradorxp.app`.
- Versão sincronizada para `0.1.0-alpha.43` / `versionCode 43`.

## 0.1.0-alpha.42

- Criada configuração de assinatura Android permanente para os APKs instaláveis.
- `performance` e `release` passam a usar a mesma chave via GitHub Actions Secrets.
- Workflow valida e reconstrói o keystore a partir de `ANDROID_KEYSTORE_BASE64`.
- Secrets esperados: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` e `ANDROID_KEY_PASSWORD`.
- Arquivos `.jks`/`.keystore` e propriedades locais de assinatura foram adicionados ao `.gitignore`.
- `github-manager.json` mantido no schema já usado pelo GitHub Manager e sincronizado para `0.1.0-alpha.42` / `versionCode 42`.

## 0.1.0-alpha.41

- Corrigido o problema de telas e diálogos invadindo a barra de navegação do Android: a raiz do app deixa de consumir os `WindowInsets.safeDrawing` antes das janelas filhas, permitindo que os modais apliquem corretamente a própria área segura.
- **Informações do dispositivo** ganhou margem inferior adicional no conteúdo rolável para manter o último card e o botão de exportação visualmente separados da navigation bar.
- A tela **Sobre o Explorador XP** foi recentralizada dentro da área útil, ganhou limite de altura mais previsível e mantém rolagem interna sem encostar nas barras do sistema.
- Cards da tela Sobre passaram a usar fundo e borda suaves, com cantos discretos; os botões **Ajuda**, **Informações técnicas** e demais `XpDialogButton` deixaram o contorno cinza-escuro e usam um padrão claro/azulado.
- O mesmo padrão de borda foi aplicado a menus suspensos, menus de contexto, botões do visualizador interno e botões/diálogos do editor de texto/código.
- Menus de contexto de arquivo/pasta e diálogos do editor agora também possuem host explícito com `safeDrawingPadding`, evitando futuros vazamentos para status/navigation bars em telas pequenas.
- Painéis claros e menu de velocidade do player de vídeo tiveram o contorno escuro substituído pela borda clara padrão, sem alterar os controles próprios do player sobre fundo escuro.
- Revisados os principais `Dialog` do projeto: Ajuda, Sobre, Informações do dispositivo, Lixeira, Armazenamento, progresso, confirmações, propriedades/nomes/ordenação, menus de contexto e editor.
- README, documentação de validação e `github-manager.json` atualizados.
- Nenhuma imagem ou mockup foi criada ou adicionada.
- Versão sincronizada para `0.1.0-alpha.41` / `versionCode 41`.

## 0.1.0-alpha.40

- Refeita a interface da **Lixeira** com base no problema observado em aparelho real, eliminando linhas praticamente vazias e devolvendo prioridade visual ao nome do arquivo.
- Corrigida a causa do sumiço/espremimento dos nomes: `XpDialogButton` não força mais `fillMaxWidth()` internamente, evitando que botões sem largura explícita ocupem toda a linha e comprimam o conteúdo ao lado.
- Cada item da Lixeira agora exibe nome em destaque, tipo/tamanho, data da exclusão e caminho de origem em blocos legíveis.
- Ações por item foram simplificadas para **Restaurar** e **Apagar**, sempre visíveis e lado a lado; a exclusão permanente continua protegida por confirmação.
- **Atualizar** e **Esvaziar Lixeira** ficam lado a lado no cabeçalho em larguras comuns, com fallback vertical apenas em telas extremamente estreitas.
- O diálogo **Esvaziar Lixeira?** volta a mostrar corretamente **Cancelar** e **Esvaziar**, além de informar quantidade de itens e espaço ocupado.
- Novos itens enviados à Lixeira gravam também `originalName` no metadado; itens antigos continuam usando o nome derivado do caminho original/arquivo físico como fallback.
- Adicionado teste unitário para a resolução do nome exibido na Lixeira.
- README, documentação de validação e `github-manager.json` atualizados.
- Versão sincronizada para `0.1.0-alpha.40` / `versionCode 40`.

## 0.1.0-alpha.39

- Corrigidos os dois erros Kotlin apontados pelo workflow `Gerar APK` em `ExplorerScreen.kt` nas linhas do diálogo de exclusão.
- A largura disponível do `BoxWithConstraints` agora é salva em `availableWidth` antes da criação do `Row`, evitando acesso inválido ao receiver implícito de `maxWidth`.
- O comportamento visual da alpha.38 foi preservado: três ações lado a lado quando há espaço, nomes dos itens selecionados e rótulos compactos em telas estreitas.
- Nenhuma funcionalidade da Lixeira, player, editor ou demais visualizadores foi removida.
- README, documentação de validação e `github-manager.json` atualizados.
- Versão sincronizada para `0.1.0-alpha.39` / `versionCode 39`.

## 0.1.0-alpha.38

- Refinada a tela da **Lixeira** para corrigir o sumiço do nome do arquivo na listagem e reforçar a leitura do caminho/origem do item.
- Cabeçalho da Lixeira reorganizado com ações responsivas: **Atualizar** e **Esvaziar Lixeira** permanecem visíveis em larguras menores, sem sair da janela.
- Diálogo **Excluir itens?** agora exibe os nomes dos arquivos/pastas selecionados e tenta manter **Mover para a Lixeira**, **Apagar** e **Cancelar** lado a lado em telas comuns, com rótulos compactos quando necessário.
- `XpDialogButton` passou a aceitar quebra controlada em até 2 linhas para evitar cortes em ações mais longas.
- `TrashItemRow` foi compactado e reorganizado para priorizar nome, metadados e origem, mantendo **Restaurar** como ação principal e exclusão permanente no menu secundário.
- README, documentação de validação e `github-manager.json` atualizados.
- Versão sincronizada para `0.1.0-alpha.38` / `versionCode 38`.

## 0.1.0-alpha.37

- Corrigidos os dois erros de compilação Kotlin restantes encontrados no workflow `Gerar APK` após a alpha.36 avançar além de `checkDebugAarMetadata`.
- `InternalViewer.kt`: callback `requestClose` agora possui tipo `() -> Unit` explícito e não propaga `Unit?` ao `ViewerTitleBar`.
- `TextCodeEditorViewer.kt`: `WebResourceError` recebido pelo `WebViewClient` agora é tratado com acesso nulo seguro ao montar a mensagem de falha do preview.
- Nenhuma funcionalidade do player, editor, Lixeira, armazenamento ou demais visualizadores foi removida/alterada nesta correção.
- Media3 permanece em 1.9.4, mantendo compatibilidade com `compileSdk 35`.
- README, documentação de validação e `github-manager.json` atualizados.
- Versão sincronizada para `0.1.0-alpha.37` / `versionCode 37`.

## 0.1.0-alpha.36

- Corrigida a falha do workflow `Gerar APK` em `:app:checkDebugAarMetadata`.
- Causa confirmada nos logs: Media3 1.11.1 exige `compileSdk 36`, enquanto o projeto usa `compileSdk 35` com Android Gradle Plugin 8.7.3.
- `media3-exoplayer` e `media3-ui` foram ajustados de 1.11.1 para **1.9.4**, linha compatível com `compileSdk 35`, sem alterar as funcionalidades implementadas no player.
- Mantidos `compileSdk 35`, `targetSdk 35`, AGP 8.7.3 e Gradle 8.9 do workflow para evitar uma atualização ampla e desnecessária da toolchain nesta correção.
- Editor de texto/código da alpha.35 e demais funções do Explorador XP permanecem inalterados.
- README, documentação de validação e `github-manager.json` atualizados.
- Versão sincronizada para `0.1.0-alpha.36` / `versionCode 36`.

## 0.1.0-alpha.35

- Visualizador de texto/código substituído por um editor interno leve e dedicado em `TextCodeEditorViewer.kt`, sem alterar vídeo, áudio, PDF, ZIP ou demais visualizadores.
- Edição habilitada para TXT, HTML/HTM, CSS, JavaScript, JSON, XML, Markdown, YAML, CSV, LOG, INI, properties, Kotlin, Java e diversos outros formatos textuais conhecidos.
- Adicionados **Salvar**, **Salvar como** (na mesma pasta), desfazer/refazer, selecionar tudo, copiar, recortar, colar, localizar, localizar/substituir e ir para linha.
- Barra de estado mostra linha, coluna, quantidade de linhas, estado não salvo e codificação detectada.
- Leitura reconhece UTF-8, UTF-8 com BOM, UTF-16 LE/BE com BOM e texto Windows-1252 quando seguro; conteúdo com aparência binária é recusado pelo editor e mantém abertura externa.
- Salvamento passou a usar arquivo temporário, `fsync`, releitura/validação e substituição atômica quando disponível; fallback cria backup temporário para preservar o original se a troca falhar.
- Saída/fechamento com alterações pendentes exige escolher entre salvar, descartar ou cancelar.
- Editor monoespaçado ganhou números de linha, Tab com quatro espaços, manutenção básica de indentação ao pressionar Enter e realce leve para HTML, CSS, JavaScript, JSON e XML.
- HTML/HTM ganhou modo **Código / Visualizar** com `WebView`; recursos CSS/JS relativos da mesma pasta são resolvidos pelo diretório do arquivo. CSS e JS também têm preview simples para inspeção rápida.
- Preview pode ser atualizado manualmente, é renovado após salvamento e possui modo de tela cheia dentro da área segura do aplicativo com retorno rápido ao código.
- Arquivos acima do limite de edição são carregados parcialmente e apenas para leitura, com aviso claro, evitando colocar conteúdo muito grande inteiro na memória.
- Ao fechar um visualizador, a pasta atual é atualizada para refletir edições e arquivos criados por **Salvar como**.
- Nenhuma imagem ou mockup foi criado/adicionado; identidade visual XP e pacote de recursos existentes foram preservados.
- Versão e metadados sincronizados para `0.1.0-alpha.35` / `versionCode 35`.

## 0.1.0-alpha.34

- Player interno de vídeo migrado de `VideoView`/`MediaController` para **AndroidX Media3/ExoPlayer 1.11.1**, sem alterar o player de áudio nem os demais visualizadores.
- Adicionados play/pause próprios, barra de progresso, tempo atual/duração, voltar 10 s, avançar 10 s e reiniciar.
- Controle de velocidade com `0.5x`, `0.75x`, `1x`, `1.25x`, `1.5x` e `2x`.
- Modo **Tela cheia** oculta temporariamente o chrome do visualizador e as barras do Android, restaurando-as ao sair/fechar; o botão Voltar sai primeiro da tela cheia.
- Rotação preserva o arquivo aberto e os principais estados do player; a posição é salva periodicamente e ao fechar para permitir retomada quando o arquivo continua sendo o mesmo.
- Modos **Ajustar** e **Preencher** adicionados; Ajustar permanece como padrão para mostrar o vídeo inteiro sem corte.
- Controles ficam sobrepostos somente quando necessários, desaparecem automaticamente durante a reprodução e reaparecem ao toque.
- Painel de informações mostra nome, tipo, tamanho, resolução, duração e caminho quando disponíveis.
- Erros de codec, contêiner, corrupção, arquivo ausente e falta de permissão agora usam mensagens legíveis e mantêm **Abrir com outro aplicativo** como fallback.
- Ao fechar o visualizador, o ExoPlayer remove listeners, salva a posição e libera os recursos; ao o app ir para segundo plano, a reprodução é pausada.
- Nenhuma imagem, mockup ou Material Icon novo foi criado/adicionado; os controles reutilizam recursos XP existentes e símbolos de texto.
- Versão e metadados sincronizados para `0.1.0-alpha.34` / `versionCode 34`.

## 0.1.0-alpha.33

- Revisado o sistema de janelas: diálogos comuns agora usam largura responsiva, limite de altura, conteúdo interno rolável quando necessário e `safeDrawingPadding` para respeitar status/navigation bars.
- Ajuda, Lixeira, Armazenamento, Sobre e progresso ganharam contêineres seguros para impedir conteúdo visualmente fora da janela em telas menores.
- Diálogo de exclusão reorganizado com **Mover para a Lixeira**, **Apagar permanentemente** e **Cancelar**, usando os ícones XP existentes; em telas largas as três ações ficam lado a lado e em telas estreitas se adaptam verticalmente.
- **Apagar permanentemente** recebeu borda/fundo destrutivos, mantendo diferença visual clara em relação às ações reversíveis.
- Tela **Sobre o Explorador XP** refeita em blocos para informações do app, `versionCode`, desenvolvedor, PIX com copiar chave, novidades da versão e atalhos para Ajuda/Informações técnicas.
- Análise de armazenamento passou a alinhar usados/livres e **% usado/% livre** e deixa explícito que a barra principal representa o armazenamento total do Android.
- Percentuais por categoria agora são rotulados como percentuais **dos arquivos acessíveis analisados**, evitando confusão com o armazenamento total e mantendo o aviso sobre áreas protegidas do Android.
- Lixeira recebeu linhas mais compactas, hierarquia de nome/tipo-data/caminho original, ação Restaurar direta e exclusão permanente mantida no menu secundário.
- Lista/grade de arquivos receberam pequenos ajustes de densidade, tipografia, ícones, espaçamento e seleção sem aumentar cards.
- Barra de seleção permanece em **Copiar / Mover / Excluir / Mais**; o menu Mais usa os nomes **Compartilhar** e **Propriedades** para maior consistência.
- Informações do dispositivo preserva o visual moderno atual, mas melhora encaixe de textos longos, proporção dos rótulos, espaço inferior e organização responsiva dos botões Copiar/Salvar PNG/Compartilhar.
- Nenhuma imagem, mockup ou novo pacote de ícones foi criado; os recursos visuais existentes foram reutilizados.
- Pipeline de APK mantido sem alteração funcional e metadados sincronizados para `0.1.0-alpha.33` / `versionCode 33`.

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
