# Explorador XP: alpha.93 e plano alpha.94

## Alpha.93: segurança de dados

- `SafeFileCommit.kt`: staging de arquivo ou pasta no mesmo diretório, backup oculto antes da troca e tentativa imediata de rollback caso falhe o commit. **Não há atomicidade contra interrupção de energia** entre renames; um arquivo `.xp-backup-*` pode permanecer recuperável. A limpeza só ocorre após commit concluído.
- `FileRepository.paste`: operações novas copiam ao staging (inclusive recortes que não conseguem renomear) e só excluem a origem depois de confirmar o destino.
- `FileOperationPlan`: tamanho copiado validado e limpeza da folha incompleta, com sincronização de escrita.
- `ArchiveManager`: limite por extração de 8 GiB/20.000 entradas, limite de 512 MiB por preview, contagem de bytes em tempo real, staging por entrada e limpeza de caches antigos. **A extração de um ZIP inteiro não é transação única:** itens já confirmados são mantidos caso outro item falhe.
- Preview WebView: JavaScript desligado inicialmente; opção explícita por arquivo para ligar, sem rede nem acesso a URLs `file://`/`content://`. Recursos externos não serão carregados.
- `MainActivity`: limite de 4 GiB no cache por abertura externa, com verificação de espaço.
- `PreferencesStore`: consultas remotas desativadas por padrão para novas instalações; `allowBackup=false` no AndroidManifest.

## Validação recomendada

1. Substituir um arquivo com destino existente, provocar erro durante a leitura da origem, conferir que o destino antigo não mudou.
2. Cancelar transferência após o primeiro bloco, conferir que o staging é apagado e que não resta destino incompleto.
3. Tentar substituir pasta por pasta, arquivo por pasta e pasta por arquivo; conferir que backup e recuperação não corrompem conteúdos.
4. Interromper extração ZIP em entradas grandes e com metadados inconsistentes; o item anterior deve ser preservado.
5. Testar HTML com links externos, CSS remoto, imagens `file://` e JavaScript `fetch`; nada deve ter acesso à rede/armazenamento via preview.
6. Testar pré-visualização com JS desligado e ligado pelo usuário, em modo normal e tela cheia.
7. Validar uso do espaço temporário após operações e testar builds com lint + unit tests + instalados em Android 8–15.

## Alpha.94: arquivos de outro aparelho (planejado, não entregue na alpha.93)

- **Objetivo:** navegar/copiar/abrir arquivos de um segundo aparelho na **mesma rede Wi-Fi** com autenticação.
- **Opção A:** cliente SMB2/SMB3, excelente interoperabilidade com Windows/NAS, requer biblioteca mantida e testes Android.
- **Opção B:** WebDAV HTTPS, compatível com dispositivos que disponibilizam WebDAV, bom para Android a Android com servidor separado.
- **Experiência:** tela Conectar, endereço IP/hostname, credenciais, teste da conexão, favoritos; sem credenciais em logs e sem exigir exposição à Internet.
- **Segurança:** credenciais em Android Keystore (ou nenhuma persistência), TLS quando disponível, nenhum compartilhamento aberto sem autenticação.
- **Escopo inicial:** leitura, listagem e downloads com progresso/cancelamento; upload e operações destrutivas só após validar integridade e permissões.
- **Bateria:** conexão apenas sob demanda; descoberta opcional e tempo limite curto.
