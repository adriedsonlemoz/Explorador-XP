# Explorador XP — alpha.11

## Ícone

- Novo launcher grafite/dourado aplicado.
- Sem borda branca.
- `ic_launcher` adaptativo e `ic_launcher_round` usam o novo desenho.
- Mipmaps mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi foram regenerados com transparência nas bordas.

## Permissão na primeira abertura

- Se o acesso amplo aos arquivos ainda não estiver liberado, o app não inicia a leitura do armazenamento.
- Um `AlertDialog` central aparece antes da interface principal.
- O botão **Liberar acesso** leva às configurações corretas do Android e, ao retornar com a permissão ativa, a listagem é carregada.

## Visualizadores e desempenho

- Imagem: decode amostrado em `Dispatchers.IO`.
- Texto/código: leitura assíncrona, prévia limitada e widgets nativos Android para leitura/edição.
- PDF: página renderizada em I/O.
- ZIP: índice lido em I/O e limitado a 3000 entradas na prévia.
- APK: metadados carregados em I/O.
- Lista principal: metadados dos arquivos armazenados no `FileItem`, evitando novas consultas ao filesystem durante recomposições.
- Busca: debounce de 180 ms e cancelamento da atualização anterior.
