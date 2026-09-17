# Explorador XP — alpha.29 — polimento visual

Esta versão aplica as correções visuais identificadas no vídeo de uso da alpha.28 sem substituir o pacote de ícones XP escolhido pelo usuário.

## Explorer

- Barra superior mais compacta.
- Menus clássicos com linhas menores e feedback de toque.
- Busca em modo focado, ocultando controles que ocupavam espaço enquanto o teclado está aberto.
- Barra de seleção contextual em vez de manter menus comuns durante seleção.
- Endereço em breadcrumb clicável.
- Grade adaptativa com mínimo de 112 dp, botão de opções no canto e seleção reforçada.
- Lista e grade com miniaturas locais de imagem/vídeo, cache limitado e fallback para os ícones XP.
- Estados vazios e barra de status mais informativos.

## Diálogos

Criar pasta, renomear, excluir, propriedades e solicitação inicial de acesso usam a mesma moldura XP. Não há mudança no formato dos dados do usuário.

## Visualizadores

- Cabeçalho, toolbar e status inferior unificados.
- ZIP em formato de listagem do Explorer com ícones, nome/caminho e tamanho.
- TXT/código com fonte monoespaçada, estado de edição, aviso de alterações e linha/coluna.
- APK com ícone/nome reais quando legíveis pelo `PackageManager`, além de pacote, versões, SDKs, tamanho e estado de instalação.
- `.mov` incluído no visualizador de mídia interno.

## Desempenho

As miniaturas são geradas fora da thread principal, limitadas a duas decodificações simultâneas e armazenadas em cache LRU de 18 MiB. Nenhum bitmap extra foi adicionado ao APK; as miniaturas são produzidas localmente a partir dos arquivos do usuário.

## Informações do dispositivo

O conteúdo moderno existente foi mantido. Apenas a moldura e o cabeçalho foram aproximados da identidade XP para reduzir a sensação de uma tela visualmente separada do restante do aplicativo.
