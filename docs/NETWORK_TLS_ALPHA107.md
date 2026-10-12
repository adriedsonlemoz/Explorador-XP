# Investigação e endurecimento do handshake TLS — alpha.107

## O que o print comprova
O cliente reconheceu o QR, preencheu IP e campos de segurança e falhou durante o TLS. O código `TLS_HANDSHAKE` não indica por si só incompatibilidade de certificado (seria `TLS_PIN` quando o cliente detecta diferença de SHA-256) e também não comprova que o par aceitou o certificado. Autenticação Basic e PROPFIND ainda não ocorreram.

## Ajustes implementados

1. `NetworkTls.serverContext()` expõe um único par chave/certificado, por alias fixo do AndroidKeyStore. O fingerprint QR é calculado desse mesmo certificado. Sem fallback a keystores inseguros.
2. `NetworkDavClient` limita protocolos de aplicação a HTTP/1.1, único suportado pelo servidor WebDAV. TLS 1.2/1.3 continuam habilitados, sem enfraquecer cifras por conta de um print.
3. `NetworkDavServer.serve()` registra categorias de falha SSL da negociação no estado efêmero da tela Compartilhar e elimina a mensagem após handshake bem-sucedido. Classes, não exceções completas: dados de autenticação não são copiados.
4. Instruções de `TLS_HANDSHAKE` passam a solicitar ambos os diagnósticos.

## Teste de reprodução

- Instalar alpha.107 em AMBOS os celulares, desligar e religar o compartilhamento. Conferir endereço IP escolhido da interface Wi-Fi, especialmente se o aparelho tiver hotspot, VPN ou múltiplas interfaces.
- A partir do outro Android, ler o QR e tentar Conectar. Se houver erro, copiar `Detalhes da conexão > Copiar diagnóstico` no cliente.
- No aparelho servidor, abrir `Rede > Compartilhar`. Se existir `Falha TLS recebida neste celular`, copiar diagnóstico do servidor também.
- Se o servidor não registrar erro, hipótese de IP/rede/roteamento ou falha antes de atingir a camada TLS daquele processo. Não afirmar sem observação de rede.
- Apenas após handshake bem-sucedido tentar listagem, GET e PUT autorizado.

### Riscos/limites

O gerenciador dedicado precisa de validação de compatibilidade com Conscrypt e AndroidKeyStore em aparelhos reais. Ainda não há evidência suficiente para afirmar correção do `TLS_HANDSHAKE` original. Nenhuma opção que desabilita pin SHA-256 foi adicionada. A interface do servidor exibe diagnóstico apenas localmente.
