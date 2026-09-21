# Desempenho — alpha.56

Esta etapa reduz trabalho que ainda acontecia mesmo depois das otimizações de navegação (alpha.53) e operações recursivas (alpha.54). O foco agora é impedir que atualizações frequentes de progresso provoquem recomposição ampla da interface e evitar reconstruções repetidas de metadados já conhecidos.

## Estado de UI desacoplado

`ExplorerUiState` fica responsável apenas pelo estado da navegação principal. Três grupos foram separados em `StateFlow`s próprios:

- `transferState`: copiar, mover e excluir;
- `storageScanState`: análise de armazenamento;
- `trashState`: conteúdo/carregamento da Lixeira.

Os fluxos são coletados por hosts pequenos e específicos. Assim, uma nova porcentagem de transferência ou uma atualização de contagem durante a análise não invalida cabeçalho, lista, grade, barra de endereço e demais partes do Explorer.

## Metadados de arquivos

`FileRepository` mantém um LRU de até 2.048 `FileItem`s por caminho e assinatura mínima (tipo, tamanho, modificação e favorito). Quando o item não mudou, são reutilizados:

- classificação/tipo;
- recurso de ícone;
- extensão normalizada;
- textos já formatados para lista e grade.

A detecção de ocultos também deixa de chamar `File.isHidden` por item; no armazenamento compartilhado Android/Linux, o prefixo `.` é o critério relevante usado pelo app.

## Lista, grade e seleção

- `LazyColumn` e `LazyVerticalGrid` informam `contentType` separado para pasta/arquivo, facilitando reutilização de composição.
- A barra inferior cria um índice `path -> FileItem` uma vez por snapshot. Alterar a seleção passa a custar O(k) nos selecionados em vez de filtrar O(n) da pasta inteira a cada toque.
- Contagem direta de arquivos/pastas/bytes usa uma única passagem pela lista.

## Análise de armazenamento

A análise não revarre recursivamente as novas entradas da Lixeira ao final. Ela lê `treeSize` do `.trashinfo.json` por meio do mesmo parser usado pela Lixeira. Entradas antigas continuam com fallback recursivo, preservando compatibilidade.

## Validação local

- `ExplorerModels.kt` compilado isoladamente com stub da anotação `Immutable`.
- `ExplorerViewModel.kt` compilado contra stubs Android/Lifecycle/Repository e coroutines, validando a separação dos fluxos.
- `FileRepository.kt` + `FileOperationPlan.kt` compilados contra stubs Android/JSON e coroutines.
- JSON/XML/YAML e referências de recursos verificados estruturalmente.
- Imagens e workflows não foram modificados nesta etapa.

O build Android completo continua reservado ao CI porque este repositório não inclui `gradlew` e o ambiente local não possui Android SDK/Gradle configurados.
