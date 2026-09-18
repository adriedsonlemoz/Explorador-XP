# Explorador XP — alpha.41

Versão: `0.1.0-alpha.41` (`versionCode 41`)

## Problema observado

Em aparelho real, telas longas e alguns diálogos podiam avançar visualmente para a região da barra de navegação do Android. Na tela **Informações do dispositivo**, o card de relatório e o botão de exportação chegavam à área dos botões do sistema. A tela **Sobre** também ficava visualmente deslocada e ainda havia contornos cinza-escuros em botões/cards que não combinavam com os componentes novos.

## Causa técnica

A raiz da `MainActivity` usava `safeDrawingPadding()`. Esse modificador aplica **e consome** os insets do sistema para os descendentes. Como os `Dialog` são renderizados em uma janela própria, eles ainda precisavam conhecer os mesmos insets; porém, dentro da árvore Compose eles podiam receber o valor já consumido e perder a proteção inferior.

## Correção

- A raiz do app passou a aplicar `WindowInsets.safeDrawing.asPaddingValues()` como padding normal. Assim a tela principal continua respeitando status/navigation bars, mas os insets não são consumidos antes dos diálogos.
- Diálogos de tela cheia continuam aplicando `safeDrawingPadding()` em sua própria janela.
- Diálogos do editor e menus de contexto de arquivo/pasta ganharam host seguro explícito.
- `DeviceInfoDialog` recebeu também margem de conteúdo inferior adicional.
- A tela Sobre foi limitada a 92% da altura segura e centralizada no espaço disponível.

## Padronização visual

- Criados tokens de borda/fundo para controles claros (`XpControlBorder`, `XpControlBackground`, `XpControlPressed`, `XpCardBorder`).
- `XpDialogButton` deixou o contorno escuro e passou a usar borda clara, cantos discretos e estados pressionado/desabilitado coerentes.
- O mesmo tratamento foi aplicado aos botões do visualizador interno e do editor, aos menus de contexto e a superfícies claras do player de vídeo.
- Cards da tela Sobre usam borda azul-cinza clara e cantos suaves.

## Auditoria de telas

Foram revisados os caminhos de Ajuda, Sobre, Informações do dispositivo, Lixeira, Armazenamento, progresso de operações, confirmações, propriedades, renomear/criar, ordenação, menus de contexto, editor de texto/código e superfícies claras do player de vídeo.

## Validação local

O projeto não inclui `gradlew` e o ambiente desta sessão não possui Gradle/Android SDK configurados, portanto o APK não pôde ser compilado localmente. Foram feitas checagens estruturais, de referências e de sintaxe Kotlin possível sem classpath Android/Compose. O build completo permanece para o workflow do GitHub.

Nenhuma imagem ou mockup foi criada/adicionada nesta etapa.
