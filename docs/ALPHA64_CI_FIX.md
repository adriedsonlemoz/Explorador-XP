# Explorador XP — alpha.64

Versão: `0.1.0-alpha.64` (`versionCode 64`)

## Correção do Gerar-APK-61

O log mostrou que `:app:compileDebugKotlin` e `:app:testDebugUnitTest` concluíram com sucesso. O build foi interrompido somente pelo Android Lint em `MainActivity.kt`, regra `WrongConstant`, na chamada de `ContentResolver.takePersistableUriPermission()`.

A alpha.64 não usa supressão da regra. O código identifica separadamente as permissões de leitura e escrita recebidas no `Intent` e chama a API com uma das três combinações literais aceitas: leitura, escrita ou leitura + escrita.

Todo o comportamento de editor/visualizador e Modo arquivo grande da alpha.63 foi preservado.
