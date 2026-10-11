# Explorador XP alpha.95: rede local

## Escopo efetivo

Cliente e servidor WebDAV HTTPS sobre Wi-Fi/hotspot, sem dependência de internet; integração pelo menu Ferramentas. Servidor aceita PROPFIND Depth 0/1, OPTIONS, HEAD, GET e PUT (somente criação, quando escrita ligada). Outros métodos proibidos. Cliente faz PROPFIND, GET e PUT. Não implementados SMB/CIFS, descoberta automática e sincronização.

## Privacidade e segurança

- Seleção de diretórios pelo seletor SAF do Android: aliases aleatórios, acesso somente às árvores selecionadas e persistência apenas das URIs dessas árvores.
- Autorização de acesso: Basic sobre TLS 1.2/1.3 com senha forte aleatória (192 bits) por sessão. Não existe servidor iniciado automaticamente no boot.
- Certificado RSA 2048 do Android Keystore e fingerprint SHA-256 em tela; cliente exige pin explícito e confere certificado exato e validade antes de enviar senha. Ignora hostname somente após fixar o certificado completo; NÃO é uma conexão TLS sem validação.
- Acesso restrito a endereços privados/locais no servidor; cliente não acessa host público. Nenhum redirecionamento automático.
- Upload permitido apenas mediante habilitação do usuário. Não exclui nem sobrescreve itens existentes. Usa documento temporário e tenta rename ao final; provedores SAF precisam suportar rename. Revogar pastas exige desligar o servidor.
- XML da listagem com entidades externas/DTD desativados, limite 1500 itens, limite de transferência de 4 GiB, conexão com timeout, fila de clientes limitada.

## Fluxo de teste físico recomendado

1. Dois aparelhos no mesmo Wi-Fi/hotspot, instalar APK gerado do workflow após compilação.
2. No aparelho A, abrir Ferramentas > Rede local, selecionar pasta com fotos de teste e ativar servidor, deixando escrita desligada.
3. No aparelho B, abrir a mesma tela, preencher HTTPS/IP/porta, fingerprint e senha de A. Conectar, navegar e baixar arquivo de teste. Verificar hash SHA-256 no A e B.
4. Testar senha incorreta, fingerprint incorreto, IP externo, pasta não compartilhada, nome com `..`, perda de Wi-Fi, interrupção no meio do download e parada do servidor.
5. Ativar escrita no A (reiniciar servidor para gerar novas credenciais), conectar novamente em B e enviar arquivo de teste. Verificar que nomes existentes não são sobrescritos e que uploads cancelados não deixam arquivos finais parciais.
6. Testar com tela apagada e processo em segundo plano. Em Android 15, FGS dataSync possui limite temporal; o serviço deverá encerrar ao timeout.

## Limitações conhecidas

- Certificados próprios exigem conferência manual do SHA-256. Não há código QR/pareamento automático.
- Servidores WebDAV de terceiros podem variar em PROPFIND XML/respostas, exigindo validação de interoperabilidade.
- Envio depende de rename suportado pelo provider SAF da pasta; alguns providers limitam operações.
- Sem suporte SMB2/SMB3 nesta alpha.95.
- Ainda não existe um build Android integral ou um teste em dispositivos reais neste ambiente.
