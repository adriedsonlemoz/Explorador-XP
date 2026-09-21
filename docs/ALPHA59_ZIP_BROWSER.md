# Explorador XP — alpha.59

Versão: `0.1.0-alpha.59` (`versionCode 59`)

## Navegador ZIP mais completo

- O conteúdo do ZIP agora é indexado uma vez ao abrir, incluindo pastas implícitas que não possuem cabeçalho próprio no arquivo compactado.
- Pastas internas passam a mostrar tamanho descompactado total, tamanho compactado acumulado, quantidade de arquivos e quantidade de subpastas.
- A ordenação por tamanho/data usa os dados agregados das pastas e mantém pastas antes de arquivos mesmo em ordem decrescente.
- O caminho `ZIP:/...` virou uma trilha clicável: qualquer segmento pode ser tocado para voltar diretamente àquele nível.
- A pesquisa continua cobrindo todo o ZIP e agora mostra o caminho de origem de cada resultado.
- A seleção ganhou **Selecionar tudo**, **Tudo**, **Inverter** e **Limpar**, além do tamanho real que será extraído.
- A barra inferior informa arquivos, pastas e tamanho do nível atual ou a quantidade de resultados da pesquisa.
- Informações de pasta exibem contagem recursiva e tamanho agregado; arquivos mostram também tamanho compactado e taxa de compressão quando aplicável.
- Pré-visualizações grandes agora exibem progresso e podem ser canceladas antes de abrir o leitor interno/externo.
- A pré-visualização temporária usa cache por ZIP + caminho da entrada; reabrir o mesmo item sem alteração evita extrair novamente.
- Arquivos temporários são finalizados por rename após escrita completa para evitar que um preview parcial seja tratado como válido.

## Segurança

- As proteções existentes contra Zip Slip permanecem ativas.
- ZIP com senha continua exigindo senha antes de preview, verificação ou extração.
- Nenhum suporte falso a RAR/7Z/TAR/GZ foi registrado nesta etapa; o navegador interno continua sendo ZIP real.
- Nenhuma imagem ou mockup foi criada ou modificada nesta etapa.
