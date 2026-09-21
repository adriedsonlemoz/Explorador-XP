# Alpha 55 — centralização de janelas e ícones

## Centralização

O `Dialog` do Compose usa uma Window Android que, com `decorFitsSystemWindows` no valor padrão, já fornece ao conteúdo uma área ajustada às barras do sistema. A alpha.52 adicionou `safeDrawingPadding()` dentro dessa área novamente. Em aparelhos edge-to-edge isso produzia um segundo inset assimétrico e fazia a janela parecer mais baixa que o centro do aplicativo.

Na alpha.55 o inset duplicado foi removido dos hosts de diálogo. O espaçamento externo passou a ser apenas o padding simétrico do próprio layout, mantendo cabeçalho, conteúdo e rodapé dentro da janela sem alterar a rolagem interna.

## Ícones

A inspeção dos PNGs confirmou que `folder_videos.png`, `folder_music.png` e `folder_images.png` já traziam o glifo sobreposto truncado no arquivo de origem. `ContentScale.Fit` só reduzia o bitmap inteiro e não poderia recuperar pixels que já não existiam.

Os três recursos foram reconstruídos a partir de recursos já presentes no projeto: `folder.png` como base e os glifos `videos.png`, `music.png` e `pictures.png`. Não foi usada geração de imagem nem mockup. Os arquivos finais mantêm 192 × 192 px e uma margem alfa segura de 11 px nas bordas externas.
