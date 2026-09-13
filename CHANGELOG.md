# CHANGELOG

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
