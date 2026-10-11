# Explorador XP alpha.99: correções de rede e orientação na interface

Relato: ao procurar aparelhos na alpha.98, a tela mostrava apenas "Procurando aparelhos compatíveis". O usuário também não compreendia "Meus compartilhamentos" e outros termos.

## Problema de uso

NSD/mDNS detecta somente anúncios WebDAV HTTPS quando o servidor remoto está **ativo** e ambos os aparelhos compartilham a rede. Não realiza varredura genérica de telefones Android e não acessa SMB. A interface não explicava que é necessário selecionar uma pasta **no outro aparelho** e ativar o servidor.

## Mudanças

- Página inicial com duas opções claras para acessar outro celular ou compartilhar arquivos deste celular; estado desligado explícito.
- Compartilhamento em 3 passos com configuração opcional de escrita e dados de conexão apenas após ativar o servidor.
- Descoberta orienta primeiro a ativar o servidor remoto e exibe diagnóstico com alternativa por IP.
- NSD usa `NsdServiceInfo.getHostAddresses()` em API 34+ e prefere um IPv4 privado disponível; versões anteriores usam `getHost()`.
- O botão Parar não apaga os dispositivos já descobertos.
- Não há armazenamento de credenciais do outro aparelho nem redução da checagem de SHA-256.

## Validação

Compilação Android completa e testes em dispositivos reais não executados neste ambiente. Verificar especialmente descoberta com celulares em Wi-Fi/hotspot, acesso por IP, troca de rede, parada e retomada da busca.
