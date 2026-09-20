# Explorador XP — alpha.47

Versão: `0.1.0-alpha.47` (`versionCode 47`)

## Correção

O CI da alpha.46 parou em `:app:compileDebugKotlin` com:

`ArchiveViewer.kt:205:22 Unresolved reference horizontalScroll`

A função já era usada corretamente na nova barra de ações do ZIP, mas o arquivo não importava `androidx.compose.foundation.horizontalScroll`. O import foi adicionado e o restante das mudanças foi preservado.
