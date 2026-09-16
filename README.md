# Explorador XP

Gerenciador de arquivos Android nativo em **Kotlin + Jetpack Compose**, inspirado no Windows XP e redesenhado para uso confortável em telas de celular.

**Versão atual:** `0.1.0-alpha.16` (`versionCode 16`)  
**Pacote:** `com.exploradorxp.app`  
**Min SDK:** 26  
**Target/Compile SDK:** 35

## O que já está implementado

- Interface principal baseada no Explorer clássico do Windows XP: barra de título azul, menus Arquivo/Editar/Exibir/Favoritos/Ferramentas/Ajuda, barra de ferramentas compacta, barra de endereço, indicador de armazenamento, lista/grade e barra de status inferior.
- 152 recursos PNG no conjunto visual XP, integrados diretamente em `res/drawable-nodpi`.
- Reconhecimento visual de dezenas de tipos de arquivo: PDF, Word, Excel, PowerPoint, HTML, CSS, JS, JSON, XML, APK, ZIP, RAR, 7Z, imagens, áudio, vídeo, código e outros.
- Navegação real pelo armazenamento compartilhado primário.
- Histórico de navegação com Voltar e Avançar, além da ação Subir.
- Busca no diretório atual.
- Modos Lista e Grade.
- Ordenação por nome, data, tamanho e tipo.
- Toque longo entra diretamente no modo de seleção; a barra de ferramentas troca temporariamente para ações de seleção e volta ao normal ao concluir/cancelar.
- Copiar, recortar/mover e colar, incluindo pastas recursivas; quando há conteúdo na área de transferência, **Downloads** vira temporariamente **Colar** na barra de ferramentas. Cópia, mover e exclusão exibem uma janela de progresso com o item atual, contagem e barra animada, com opção de **Cancelar** a qualquer momento.
- Criar pasta, renomear e excluir.
- Visualizador interno para imagens, textos/código editáveis, HTML, PDF, ZIP, áudio/vídeo e informações de APK; formatos não suportados continuam disponíveis via `Abrir com...`.
- Abertura externa por aplicativo compatível via `FileProvider` quando necessário.
- Compartilhar arquivos.
- Favoritos persistentes.
- Atalho fixo para a pasta Downloads do armazenamento.
- Data e horário do item são mantidos em cache durante a listagem para reduzir acesso repetitivo ao armazenamento; propriedades continuam exibindo criação e modificação.
- Opção persistente para mostrar/ocultar arquivos ocultos.
- Propriedades básicas de arquivos/pastas.
- Na primeira abertura sem permissão, um pop-up central obrigatório explica o acesso aos arquivos e leva diretamente à tela do Android para conceder `MANAGE_EXTERNAL_STORAGE`.
- Launcher legado/adaptativo redesenhado com fundo grafite arredondado, pasta dourada e máscara compatível com ícones adaptativos/round do Android.
- Barras de status e navegação do Android permanecem visíveis; o modo imersivo/tela inteira foi removido.

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
  FileIconMapper.kt
  PreferencesStore.kt

app/src/test/java/com/exploradorxp/app/
  ExplorerItemTransformsTest.kt

app/src/main/res/drawable-nodpi/
  152 PNGs do pacote visual XP

docs/
  mockup_explorador_android_xp.png
  catalogo_icones.png
  ICON_FILES.txt
```

## Acesso aos arquivos

O app usa acesso amplo ao armazenamento compartilhado porque sua função principal é gerenciamento de arquivos. Em Android 11+, o usuário precisa conceder manualmente **Acesso a todos os arquivos**. Em versões anteriores, o app solicita as permissões legadas necessárias.

A primeira alpha prioriza o armazenamento compartilhado primário. O suporte dedicado a SD/USB por SAF (`ACTION_OPEN_DOCUMENT_TREE`) está planejado para a próxima etapa, para cobrir volumes que não podem ser tratados diretamente por `java.io.File`.

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
