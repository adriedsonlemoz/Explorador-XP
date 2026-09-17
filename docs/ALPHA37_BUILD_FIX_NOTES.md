# Explorador XP — alpha.37

Versão: `0.1.0-alpha.37` (`versionCode 37`)

## Motivo

O workflow `Gerar APK` da alpha.36 confirmou que a incompatibilidade AAR do Media3 foi resolvida e avançou até `:app:compileDebugKotlin`. O compilador então apontou apenas dois erros de código.

## Correções

1. `InternalViewer.kt`
   - `requestClose` era inferido como `() -> Unit?` porque um ramo usava `guardedCloseRequest?.invoke()`.
   - O callback passou a declarar `() -> Unit` explicitamente e finaliza o ramo protegido com `Unit`.

2. `TextCodeEditorViewer.kt`
   - `WebViewClient.onReceivedError` recebe `WebResourceError?`.
   - A descrição do erro agora usa `error?.description`, mantendo a mensagem de fallback quando o objeto é nulo.

## Escopo

Correção estritamente de build. Não houve remoção de funcionalidade nem alteração funcional no editor de texto/código, player de vídeo ou demais módulos. Media3 permanece em 1.9.4.
