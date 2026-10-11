# Explorador XP 0.1.0-alpha.93

## Confiabilidade e segurança

- **Copiar, mover e substituir:** primeiro prepara a nova cópia em arquivo/pasta temporário; só então troca o destino, com backup de recuperação durante a operação. Falhas antes do commit preservam o arquivo antigo e removem a cópia temporária.
- **ZIP:** extração em temporários, substituição recuperável e proteção contra expansão de arquivos e arquivos de pré-visualização enormes. São aceitas no máximo 20.000 entradas e até 8 GiB de bytes descompactados em uma operação; preview de até 512 MiB por item.
- **Preview HTML/JS:** JavaScript desativado por padrão, ativável manualmente no editor. Sem acesso do preview a arquivos locais, content providers ou rede. Imagens/estilos externos não carregam no modo seguro.
- **Abertura externa:** cópia temporária limitada a 4 GiB por arquivo, com verificação de espaço a cada bloco.
- **Busca avançada:** atualização em lotes para não ordenar o mesmo conjunto repetidamente.
- **Privacidade:** buscas online por imagem/identificação do dispositivo ficam desligadas por padrão em novas instalações, mas a preferência existente é respeitada. Backup Android do app foi desativado.

## Planejamento para 0.1.0-alpha.94 (não implementado nesta versão)

- Acesso aos arquivos de outro aparelho pela rede local Wi-Fi, com autenticação e permissões explícitas.
- Avaliar suporte a SMB2/SMB3 ou WebDAV, mais descoberta opcional; evitar expor compartilhamentos sem autenticação. Testar em Android/Windows e respeitar acesso offline e economia de bateria.

Versão: `0.1.0-alpha.93`  
Version code: `93`

**Validação:** compile/teste no workflow GitHub Actions ou Android Studio antes de distribuir APK; o ZIP contém o código-fonte, não o APK assinado.
