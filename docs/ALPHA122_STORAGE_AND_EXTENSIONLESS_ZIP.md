# Alpha.122: armazenamento e ZIP sem extensão

- O resumo dos aplicativos usa `StorageStatsManager` com Acesso ao uso. É calculado em segundo plano, iniciado somente quando a tela Armazenamento é aberta. Se nem todos os pacotes responderem, indica **Total parcial** e a fração medidos/instalados; não soma APKs de pacotes sem estatística.
- A leitura `dataBytes` já inclui `cacheBytes`, portanto o total é `appBytes + dataBytes`.
- As abas Categorias/Pastas/Arquivos reutilizam a mesma varredura. A aba Arquivos apresenta e filtra somente os 100 maiores arquivos visíveis para o Android, não o armazenamento privado de apps.
- A categoria OBB reconhece dados de jogos no sistema de arquivos; não adicione essa categoria ao total dos apps, pois há possibilidade de sobreposição.
- Artefatos externos sem extensão e MIME `application/octet-stream`, `application/x-binary` ou `application/x-octet-stream` são candidatos somente a ZIP. O app verifica os quatro bytes da assinatura antes de gastar espaço no cache e confirma o tipo após copiar; documentos arbitrários com MIME genérico não são abertos nesse fluxo.
- O Android pode ocultar o Explorador XP no Abrir com se o aplicativo de origem enviar MIME diferente, URL remota, ou não oferecer `ACTION_VIEW` com acesso à URI.
- Testes no GitHub Actions e Android físico necessários: ZIP grande e corrompido, nomes sem extensão, permissão negada/revogação de Acesso ao uso, OBB e filtros.
