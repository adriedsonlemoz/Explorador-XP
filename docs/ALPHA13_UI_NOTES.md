# Explorador XP — alpha.13

## Desempenho

- Propriedades de arquivo/pasta: tamanho, datas de criação/modificação e permissões de leitura/escrita passam a ser apuradas em `Dispatchers.IO` via `produceState`, em vez de chamadas diretas ao sistema de arquivos durante a composição.
- Barra de status inferior: contagem de itens e tamanho total passam a ser recalculados apenas quando a listagem ou a seleção mudam (`remember(items, selectedPaths)`), não a cada recomposição do restante da tela.

## Navegação e rolagem

- Lista e grade de arquivos passam a ter `LazyListState`/`LazyGridState` próprios por combinação de aba + pasta atual.
- Ao navegar para outra pasta ou trocar de aba, a rolagem reinicia no topo em vez de manter a posição da listagem anterior.

## Polimento visual

- Itens da lista e da grade animam a posição (`animateItem`) ao reordenar por nome, data, tamanho, tipo ou "Pastas primeiro".
- A barra de progresso do cartão de armazenamento anima a transição da proporção usada/livre ao trocar de volume (interno ↔ cartão SD).
- O diálogo de Propriedades usa `animateContentSize` para a troca entre o indicador de carregamento e os detalhes carregados, evitando um salto abrupto de altura.

## Sem mudanças de layout

- Nenhum elemento de interface foi reposicionado, renomeado ou removido nesta versão; o foco foi desempenho e transições mais suaves sobre a interface já aprovada.
