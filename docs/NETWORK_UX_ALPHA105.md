# Rede local: fluxos de UX — alpha.105

## Tela inicial
3 ações em cartões: **Acessar**, **Compartilhar**, **Ler QR**. O acesso Windows/SMB e Quick Share continuam como botões secundários.

## Acessar
Procura WebDAV HTTPS anunciado (somente serviços ativos), informa por que não aparece e oferece alternativa de endereço IP e QR. Conectar por IP mostra somente URL, QR, Conectar e expansão de credenciais. Ao editar o host, credenciais anteriores são descartadas para impedir envio acidental ao novo IP.

## Compartilhar
Uma pasta compartilhada é escolhida via Storage Access Framework. Servidor desligado por padrão, upload desativado. Após ativação: IP curto com botões QR, Copiar IP e Detalhes. Detalhes exibe usuário, senha temporária e SHA-256; cópia completa marca conteúdo sensível no clipboard. O QR temporário só aparece ao tocar no botão e expõe senha: não compartilhar com desconhecidos.

## QR
Leitor usa `DecoratedBarcodeView` em diálogo do Compose, pede permissão de câmera, pausa a câmera ao fechar e não faz conexão automática. O resultado passa pela validação existente de `NetworkQrCode` (IP local, porta, usuário, senha, certificado).

## Diagnósticos
Exceções de rede são classificadas por HTTP/TLS/endereço/tempo/encerramento; cada explicação é hipótese, não diagnóstico confirmado da causa. O '?' oferece ações e texto copiável sem URL nem credenciais. Não foi removida a verificação de certificado e não há fallback HTTP.

## Limitações
- WebDAV/SMB requer serviço remoto ativo e autorização. Wi-Fi por si só não expõe arquivos de outro Android.
- No Android real, permissões, tema, tamanho da câmera em diálogo e ciclo de vida do scanner precisam ser testados.
- Não haverá compilação Android validada até o workflow GitHub Actions.
