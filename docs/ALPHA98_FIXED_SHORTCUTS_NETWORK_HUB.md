# Explorador XP alpha.98: barra fixa e central de rede

Versão: `0.1.0-alpha.98` / `versionCode 98`.

## Comportamento

- A barra fixa fica abaixo do conteúdo, fora das listas roláveis, tanto na tela principal quanto nas telas internas da navegação. Visores imersivos do Android podem ocupar a tela inteira por decisão de UX.
- Fotos/Vídeos usam MediaStore, leitura assíncrona e até 600 resultados, ignorando a Lixeira particular do XP. Entradas dependem de arquivos indexados e acessíveis ao app; sem criar duplicações.
- Downloads altera a aba/diretório já existente; Lixeira preserva fluxo de recuperação original; Rede preserva HTTPS WebDAV e NSD/mDNS.
- Parte superior não repete mais Downloads e Lixeira, e oferece Atualizar e Favoritos/Colar no lugar.
- Rede foi dividida em HOME, DISCOVERY, MANUAL, SHARE, REMOTE e WINDOWS (aviso não funcional). Opções aparecem pelo botão flutuante +.
- O servidor permanece desligado até ativação manual e somente leitura por padrão. Conexão cliente continua exigindo impressão SHA-256 e senha; ações de navegação/download/upload não foram retiradas.

## Pendências

- SMB2/SMB3 não implementado.
- Verificar Android Gradle build e teste em aparelhos reais, inclusive orientação de tela, rolagem e teclado virtual.
