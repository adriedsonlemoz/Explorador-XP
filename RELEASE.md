# Explorador XP 0.1.0-alpha.120

**Versão:** `0.1.0-alpha.120`  
**Version code:** `120`

## Correção do build 111

- Em `ExplorerQuickAccess.kt`, importada a extensão Compose `androidx.compose.foundation.layout.fillMaxHeight`, utilizada na barra de seleção de Fotos/Vídeos.
- Verificação de importações Compose antes do Gradle estendida a todos os arquivos Kotlin, cobrindo `horizontalScroll` e `fillMaxHeight`.
- Teste de Novidades adaptado para exibir somente o escopo da alpha.120.
- Funcionalidades da alpha.119 preservadas (mini-player, Media3, reprodução de fundo e controles de mídia).

## Pendências

- Executar o workflow `Gerar APK` e confirmar `:app:testDebugUnitTest`, `:app:lintDebug` e `:app:assemblePerformance`.
- Testar player de áudio, notificação, fones/Bluetooth, retomada e economia de bateria em aparelho Android.
