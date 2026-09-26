# Explorador XP 0.1.0-alpha.87

## Correção de compilação

Esta versão corrige o erro encontrado no workflow **Gerar APK 83**:

`ExplorerScreen.kt:829:29 Unresolved reference 'showInstalledApps'`

O menu **Ferramentas > Aplicativos instalados** estava dentro do composable `XpHeader`, enquanto o estado `showInstalledApps` pertence à tela principal. A alpha.87 mantém o estado no componente proprietário e passa ao cabeçalho apenas a callback `onShowInstalledApps`, seguindo o mesmo padrão já usado para Configurações e Informações do dispositivo.

## Funções preservadas

O gerenciador de aplicativos da alpha.86 permanece completo: lista pesquisável, filtros Usuário/Sistema, ícones, versão, tamanho, estatísticas via `StorageStatsManager`, permissões e ações seguras. Nenhuma função anterior do Explorador XP foi removida nesta etapa.

Versão: `0.1.0-alpha.87`  
versionCode: `87`
