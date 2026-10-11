# Explorador XP alpha.94: correção do build 89

## Falhas identificadas nos logs

- `ArchiveManager.kt` (12 referências não resolvidas): constantes de limite e função de limpeza de cache.
- `MainActivity.kt` (uma referência não resolvida): limite de 4 GiB por abertura externa.

## Implementação

- `app/src/main/java/com/exploradorxp/app/ArchiveResourceLimits.kt`: valores de segurança e limpeza em cache privado.
- `ArchiveManager.kt`: chamada da limpeza ao preparar e confirmar previews, proteção do arquivo atual e atualização da data de uso em cache hit.
- `ArchiveResourceLimitsTest.kt`: verifica constantes, expiração de cache, teto global e proteção de staging recente.

## Limites e ressalvas

- Limites de extração não configuráveis nesta versão: 8 GiB / 20.000 entradas. Preview por item: 512 MiB. Abertura externa: 4 GiB.
- Cache de previews: alvo de até 1 GiB, exceto quando o arquivo aberto/arquivos ainda em escrita precisam ser preservados.
- A limpeza por idade remove itens sem uso por sete dias. Não remove originais, apenas o cache privado.
- Antes do release verificar `gradle :app:testDebugUnitTest :app:lintDebug :app:assemblePerformance`.

## Recurso futuro

- Acesso remoto autenticado a outro aparelho via Wi-Fi (SMB2/3 ou WebDAV) fica para alpha.95, condicionado à validação do build de alpha.94.
