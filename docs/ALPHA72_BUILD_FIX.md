# Explorador XP 0.1.0-alpha.72

## Correção de build

O workflow **Gerar APK** falhava em `:app:compileDebugKotlin` porque `InternalViewer.kt` importava `horizontalScroll` do pacote incorreto `androidx.compose.foundation.layout`.

A alpha.72 passa a importar `androidx.compose.foundation.horizontalScroll`, que corresponde ao modificador usado pela galeria interna.

Erro corrigido:
- `InternalViewer.kt:40:43 Unresolved reference horizontalScroll`
- `InternalViewer.kt:600:22 Unresolved reference horizontalScroll`

Versão: `0.1.0-alpha.72` / `versionCode 72`.
