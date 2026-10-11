# Explorador XP 0.1.0-alpha.100

## Acesso Wi-Fi: SMB2/SMB3 e cópia seletiva

- Cliente SMB2/SMB3 somente leitura para pastas compartilhadas de Windows, NAS e Android com servidor SMB ativo.
- Conexão por IP privado, nome do compartilhamento, usuário e senha; assinatura SMB obrigatória.
- Descoberta opcional de anúncios `_smb._tcp.` via mDNS, mantendo conexão manual por IP.
- Navegação por pastas e seleção de vários arquivos remotos para copiar para pasta escolhida no Android, sem sobrescrever nomes existentes.
- Integração com Android Sharesheet para envio avulso via Quick Share ou apps compatíveis, sem instalar Explorador XP no destinatário.
- Interface WebDAV, compartilhamento HTTPS/TLS e barra de atalhos preservados.
- Não é possível navegar em armazenamento de celular sem serviço de compartilhamento e autorização.

**Validação:** verificação sintática e testes JVM locais de endereços/caminhos. Compilação Gradle do APK e teste real Android/SMB ainda pendentes.

Versão: `0.1.0-alpha.100`  
Version code: `100`
