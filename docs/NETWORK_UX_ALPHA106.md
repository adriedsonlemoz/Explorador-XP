# Rede local: UX e hipóteses de TLS — alpha.106

## Mudanças implementadas

- Rede local removeu somente o botão Quick Share. WebDAV HTTPS continua permitindo navegação/cópia, e envio quando o servidor libera "Permitir receber arquivos". SMB2/3 e demais recursos mantidos.
- Em Conectar por IP, o endereço ganha largura total e as ações ficam na ordem QR / Segurança / Conectar (verde quando habilitado); a área avançada de SHA-256, usuário e senha é expandida por Segurança.
- QR emitido via ZXing com quiet zone de 4 módulos, upscale inteiro e FilterQuality.None; leitura em janela usa instrução curta em português e vibração para QR válido e para a conexão autenticada efetiva.
- Erro visível inclui descrição, orientação e botão de diagnóstico. TLS_PIN significa SHA-256 do certificado apresentado distinto do recebido pelo QR; TLS_DATE indica validade; TLS_HANDSHAKE indica falha de negociação sem afirmar que o QR está errado.
- Exportação limita detalhe técnico a nomes de classes, sem expor senha, URLs, mensagem bruta ou token. Certificado SHA-256 continua exigido.

## Investigação TLS

O print da alpha.105 apresentou código único `TLS`, que era produzido tanto por certificado diferente quanto por exceção TLS sem pin divergente. Portanto, **não é possível demonstrar a causa raiz a partir dele**.

Hipóteses práticas: IP aponta para outro servidor/dispositivo; servidor reiniciado ou certificado alterado; conflito de TLS Android/keystore; falha de conexão no hotspot. A alpha.106 separa as classes para obter o diagnóstico real. A certificação AndroidKeyStore exige assinatura compatível, e o servidor atual produz chave RSA com PURPOSE_SIGN, SHA-256 e RSA PKCS1/PSS, sem evidência no print de que tenha falhado. Testes de handshake em dois aparelhos são necessários para afirmar o motivo e corrigir o componente específico.

## Roteiro de verificação em aparelhos

1. Instale a alpha.106 nos dois telefones. Confirme Wi-Fi/hotspot na mesma rede local e nenhum VPN forçando rota diferente.
2. No servidor: Compartilhar > escolha pasta > Ativar compartilhamento; permita receber arquivos apenas para testar upload autorizado. Verifique o endereço IP atual e exiba QR recente.
3. No cliente: Ler QR e confirme a vibração curta. Compare o IP visível, pressione Conectar e confirme a vibração longa **somente quando houver acesso**.
4. Se houver erro, copie o novo diagnóstico com o código TLS_PIN, TLS_DATE ou TLS_HANDSHAKE e classes de exceção. Teste reinício do servidor e novo QR, sem desativar a verificação do pin.
5. Se conectou, confira listagem, download e upload somente com autorização. Valide também a interface no modo retrato e zoom de fonte do Android.

Build Android completo, câmera real e handshake TLS entre dois aparelhos não executados neste ambiente.
