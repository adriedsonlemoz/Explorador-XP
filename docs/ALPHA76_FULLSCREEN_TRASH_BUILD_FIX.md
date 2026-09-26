# Explorador XP 0.1.0-alpha.76

## Informações do dispositivo
- Removido o botão inferior **Fechar**.
- O cabeçalho usa o mesmo X vermelho das janelas XP.

## Lixeira
- Convertida de `XpModalWindow` para tela de tela cheia.
- O conteúdo usa toda a área do aplicativo e fecha pelo X superior ou pelo botão Voltar do Android.
- Diálogos continuam sendo usados apenas para confirmação de exclusão definitiva e esvaziamento.

## Correção do build
O workflow falhava em `:app:compileDebugKotlin` porque `sha256File(File)` existia no novo `ApkInspectorSupport.kt` e também em helpers privados usados por `MainActivity.kt` e `TextCodeEditorViewer.kt`, gerando ambiguidade no compilador Kotlin 2.0.21.

A função do inspetor foi renomeada para `sha256ApkFile(File)` e sua chamada em `ApkInspector.kt` foi atualizada.
