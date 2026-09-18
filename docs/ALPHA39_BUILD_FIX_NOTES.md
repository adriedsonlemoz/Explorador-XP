# Explorador XP — alpha.39

Versão: `0.1.0-alpha.39` (`versionCode 39`)

## Causa do build

O workflow da alpha.38 chegou normalmente a `:app:compileDebugKotlin`, mas o Compose recusou dois usos de `maxWidth` dentro do `Row` do diálogo de exclusão porque o receiver implícito do `BoxWithConstraints` já não estava disponível naquele contexto.

## Correção

A largura é capturada em `availableWidth` antes do `Row` e esse valor é reutilizado para escolher os rótulos responsivos. O layout e as ações da alpha.38 permanecem inalterados.
