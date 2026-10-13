# Explorador XP alpha.113 • fundo principal branco

## Escopo

- Paleta compartilhada: `XpBackground = #FFFFFF`, usada no Material3 e nas telas de arquivos, rede, mídia, ferramentas e novidades.
- Páginas antes com branco azulado: aplicativos instalados, detalhes de aplicativos, ferramentas, ajuda, configurações, sobre e dispositivo passam a `XpBackground`.
- Bordas: `XpCardBorder = #A9B9CA`. O cartão branco de detalhes do aplicativo usa a borda compartilhada para não se confundir com a página.
- Contornos de campos de entrada usam `XpControlBorder = #8496AA`, com contraste superior a 3:1 sobre branco.
- `Theme.ExploradorXP` estabelece `android:windowBackground` branco para o período antes da primeira composição.
- Preservados para contraste e semântica: barra superior azul e texto branco, navegação, texto escuro de listas, seleção azul e cores específicas de risco/erro/sucesso. Visualizadores de mídia podem usar fundo preto/cinza, intencionalmente.

## Critérios de aceitação (em aparelho)

1. Abrir Downloads, armazenamento interno, Fotos/Vídeos, aplicativos, seus detalhes e painel de ferramentas: o fundo principal deve aparecer branco.
2. Verificar leitura dos textos, badges, bordas de cartões, botões e separação de linhas.
3. Selecionar um arquivo: o realce azul deve permanecer visível e não confundir com fundo branco.
4. Abrir ajuda, configurações e informações do dispositivo: plano de fundo branco, blocos claramente diferenciados.
5. Abrir vídeo, imagem e menus: preservadas cores específicas dessas experiências.
6. Testar em aparelho com modo escuro do sistema: o tema do app permanece claro, como antes.

## Limites da validação

A verificação `python3 tools/validate_white_theme.py`, o parser Kotlin PSI de 5 fontes e a validação de XML/JSON passaram. Essas verificações não substituem compilação Android no CI nem inspeção visual real. Não foram mudados fluxos de arquivos, rede, permissão ou armazenamento.
