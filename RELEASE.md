# Explorador XP 0.1.0-alpha.106

## Rede local mais clara e diagnóstico HTTPS/TLS

- Removido o atalho Quick Share da entrada de Rede; o próprio Explorador XP continua permitindo cópia e envio autorizado de arquivos pela rede Wi-Fi, via WebDAV HTTPS.
- Tela Conectar por IP: QR, Segurança e Conectar são três botões lado a lado. O último fica verde quando os dados necessários estão completos.
- Leitura QR: orientação em português e vibração após QR válido; nova vibração após conexão efetiva.
- QR gerado com módulos quadrados precisos e margem de segurança, sem interpolação no desenho.
- Erros TLS: causa visualmente legível e passos específicos para PIN divergente, validade e falha de handshake. Diagnóstico copiável com nomes de classes sem senha.
- Não houve relaxamento da verificação de certificado. WebDAV/SMB e recursos anteriores mantidos.

**Versão:** `0.1.0-alpha.106`  
**Version code:** `106`

**Verificação:** testes Kotlin/JVM e análise sintática local. A compilação Android completa e o teste de rede entre dois celulares estão pendentes. O diagnóstico antigo TLS não é suficiente para determinar a causa raiz real.
