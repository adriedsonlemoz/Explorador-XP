# Explorador XP — alpha.34 — player interno de vídeo

## Situação anterior

O visualizador interno tratava vídeo e áudio pelo mesmo `VideoView` com `MediaController`. O vídeo iniciava automaticamente e dependia dos controles nativos básicos, sem velocidade, ±10 segundos, modo de encaixe, retomada persistente, tratamento detalhado de erro ou gerenciamento próprio de tela cheia. O arquivo aberto também não era salvo entre recriações da Activity, o que tornava a rotação frágil.

## Implementação desta etapa

- Vídeo separado do fluxo legado de áudio e movido para `VideoPlayerViewer.kt`.
- Reprodução baseada em `androidx.media3:media3-exoplayer:1.11.1` e superfície `PlayerView` de `media3-ui:1.11.1`.
- Interface própria em Compose para manter a identidade do Explorador XP sem adotar Material Icons.
- Play/pause, progresso, tempo atual/duração, ±10 s, reinício e velocidades 0.5x/0.75x/1x/1.25x/1.5x/2x.
- `Ajustar` usa `RESIZE_MODE_FIT` e é o padrão; `Preencher` usa `RESIZE_MODE_ZOOM` por escolha explícita do usuário.
- Tela cheia remove apenas o chrome do visualizador de vídeo e oculta temporariamente system bars com comportamento transitório por gesto.
- Arquivo aberto passou a ser guardado por caminho com `rememberSaveable`, permitindo reconstrução após rotação.
- Posição do vídeo é salva em `SharedPreferences` por identidade do arquivo (caminho, tamanho e última modificação), periodicamente e no fechamento. Posições muito próximas do início/fim não são mantidas.
- ExoPlayer é pausado quando a Activity vai para segundo plano e liberado no `onDispose`.
- O painel Info exibe nome, tipo, tamanho, resolução, duração e caminho quando disponíveis.
- Falhas de codec/contêiner/arquivo/permissão recebem mensagens amigáveis e sempre oferecem abertura externa.
- Controles desaparecem automaticamente após alguns segundos enquanto o vídeo está tocando e retornam ao toque; pausado, com erro ou com menus abertos, permanecem visíveis.

## Escopo preservado

- Player de áudio continua no comportamento anterior nesta etapa.
- Imagem, HTML, PDF, ZIP, APK e editor de texto não foram migrados nem redesenhados.
- Nenhuma imagem ou mockup foi criado.
- Nenhum Material Icon foi introduzido no player.

## Dependência

A versão usada é Media3 1.11.1, estável em 10/09/2026. Os módulos `media3-exoplayer` e `media3-ui` usam exatamente a mesma versão.
## Correção de compatibilidade posterior — alpha.36

O build de CI mostrou que Media3 1.11.1 exige `compileSdk 36`, enquanto o projeto permanece em `compileSdk 35` com AGP 8.7.3. Na alpha.36, `media3-exoplayer` e `media3-ui` foram fixados em **1.9.4**, preservando o código e as funcionalidades do player e evitando uma migração ampla da toolchain apenas para resolver essa incompatibilidade de metadados AAR.

