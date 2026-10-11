# Explorador XP 0.1.0-alpha.100: acesso a arquivos remotos por Wi-Fi

## Implementado nesta versão

- Cliente SMB2/SMB3 baseado no SMBJ 0.14.0, **somente leitura**, restrito a IP IPv4 privado, pasta compartilhada explícita e autenticação por usuário/senha/domínio. O cliente solicita assinatura SMB; criptografia depende do protocolo/servidor.
- Lista de pastas e arquivos remotos pelo mesmo explorador da conexão WebDAV HTTPS existente. Permite abrir diretórios, salvar um arquivo e selecionar vários arquivos de uma pasta para copiar para uma pasta escolhida pelo SAF no Android.
- Não substitui arquivos locais de mesmo nome durante cópia em lote; não grava no aparelho SMB. O SAF isola as permissões ao diretório escolhido. Download com verificação de tamanho e tentativa de limpeza em falhas, até 4 GiB por arquivo.
- Descoberta SMB via anúncio mDNS `_smb._tcp.` (opcional; muitos servidores Windows não anunciam mDNS): conexão manual por IP continua disponível.
- Ação de envio pelo Android Sharesheet/Quick Share: permite escolher um arquivo local e enviar para outro celular com consentimento do destinatário. **Não navega nas pastas do destinatário.**
- WebDAV HTTPS, certificado SHA-256 e compartilhamento de pastas do próprio celular continuam disponíveis.

## Como acessar arquivos de outro celular SEM Explorador XP

O celular remoto precisa executar **algum servidor SMB2/3 ou WebDAV HTTPS** e permitir acesso a uma pasta. O Explorador XP não pode ler armazenamento remoto apenas por estar no mesmo Wi-Fi, nem transformar Quick Share em explorador de arquivos. Se o telefone remoto não oferece um compartilhamento, a alternativa compatível é usar o Quick Share para envio pontual autorizado.

## Passos do teste SMB

1. No outro dispositivo, disponibilize uma pasta SMB2/SMB3, preferencialmente com um usuário dedicado e permissão somente leitura.
2. Conecte os aparelhos ao mesmo Wi-Fi/hotspot e confirme que o roteador não isola clientes.
3. No Explorador XP > Rede > + > Windows / Android SMB2/3, informe o IP privado do servidor, nome **exato** do compartilhamento (não o caminho `C:\\`), usuário, senha e domínio opcional.
4. Abra a pasta, marque arquivos e escolha **Copiar selecionados**. Selecione uma pasta do Android para receber os arquivos. Os arquivos existentes com o mesmo nome serão preservados.
5. Em dispositivos com SMB anunciado por mDNS, use **Procurar SMB** para preencher o IP e insira as credenciais separadamente.

## Segurança e limites

- Nenhuma conexão a IP público pelo cliente SMB; não há suporte SMB1. Não há varredura agressiva de portas na LAN.
- A senha SMB não é salva. A descoberta não concede acesso; conexão e autenticação são obrigatórias.
- SMB com assinatura não implica conteúdo cifrado; não usar em rede não confiável sem proteção adicional.
- Nome do compartilhamento é obrigatório. Listar todos os compartilhamentos de um servidor e acessar pastas Android sem servidor **não** foram implementados.
- Transferência em lote opera somente sobre arquivos selecionados da pasta atual (não copia recursivamente diretórios).
- É necessário validar compilação Gradle/Android e testes em dois aparelhos. SMBJ 0.14.0 será baixado do Maven Central no build; compatibilidade de execução em Android não foi verificada aqui.
