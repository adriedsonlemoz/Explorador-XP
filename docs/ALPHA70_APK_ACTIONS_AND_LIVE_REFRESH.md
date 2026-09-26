# Explorador XP — alpha.70

## Objetivo

Esta versão concentra duas correções de uso solicitadas na interface do inspector de APK e na atualização da pasta atualmente aberta.

## Inspector de APK

Quando o APK corresponde a um aplicativo já instalado, as três ações principais passam a ocupar uma única linha:

- **Reinstalar** (ou o rótulo de instalação correspondente à relação de versão);
- **Abrir**;
- **Gerenciar**.

Os antigos rótulos **Abrir app** e **Gerenciar app** foram encurtados para melhorar o encaixe em telas menores. Se o pacote não possuir uma tela inicial, **Abrir** continua visível, mas desabilitado. Para APKs ainda não instalados, a ação de instalação continua ocupando a largura disponível.

## Atualização automática da pasta aberta

O Explorer agora cria um observador somente para o diretório que está visível nas abas Arquivos/Downloads. Alterações relevantes geradas pelo próprio Android ou por outros aplicativos disparam uma releitura automática da pasta.

Eventos acompanhados incluem criação, finalização de escrita, exclusão e movimentação de entradas. Como uma única gravação pode produzir mais de um evento, o `ExplorerViewModel` aplica um debounce de 350 ms antes de atualizar a listagem.

Ao navegar para outra pasta, o observador acompanha o novo diretório. Em Favoritos ou sem permissão de acesso, ele é desligado. Ao destruir o `ViewModel`, o observador também é encerrado.

## Versão

- `versionName`: `0.1.0-alpha.70`
- `versionCode`: `70`
- `applicationId`: `com.exploradorxp.app`
