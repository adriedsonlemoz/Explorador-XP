# Explorador XP — alpha.44

Versão: `0.1.0-alpha.44` (`versionCode 44`)

## Compactação de seleção

- O modo de seleção ganhou **Mais > Compactar em ZIP** para um ou vários arquivos/pastas.
- O diálogo permite escolher nome do ZIP e pasta de destino.
- Pastas são percorridas recursivamente e sua hierarquia é preservada.
- Itens de origem com o mesmo nome recebem raiz única dentro do ZIP.
- O progresso usa bytes lidos + itens concluídos, com porcentagem, velocidade e item atual.
- Cancelamento e erro removem o arquivo temporário; o ZIP final só aparece após conclusão.
- Colisão do nome de saída usa nome livre automático, sem substituir arquivo existente.
- A saída é ignorada durante a coleta, evitando auto-inclusão caso o destino esteja dentro de uma pasta selecionada.
- Conclusão oferece **Abrir ZIP**, **Abrir pasta** e **Fechar**.

Nenhuma imagem ou mockup foi criada ou modificada nesta etapa.
