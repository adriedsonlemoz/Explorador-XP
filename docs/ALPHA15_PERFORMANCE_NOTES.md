# Explorador XP — alpha.15 — desempenho

## Objetivo

Reduzir travamentos percebidos ao rolar listas e navegar entre pastas sem alterar o visual XP nem remover funcionalidades.

## Alterações aplicadas

1. **Snapshot de diretório**: o disco é lido ao entrar/atualizar uma pasta; busca, ordenação, `Pastas primeiro` e ocultos operam em memória.
2. **LRU de navegação**: até 12 snapshots recentes ficam em cache para resposta imediata em Voltar/Avançar, com atualização real em segundo plano.
3. **CPU/I/O fora da UI**: filtro e ordenação usam `Dispatchers.Default`; leitura de metadados, validação de diretório e checagens de abrir/compartilhar usam `Dispatchers.IO`.
4. **Scroll mais leve**: data/hora/tamanho chegam pré-formatados no `FileItem`; `animateItem` foi removido; a camada duplicada de gesto da área externa foi retirada.
5. **Filesystem mais barato**: a UI não resolve `canonicalPath` durante composição; localização de volumes é cacheada por sessão.
6. **Capacidade sob demanda**: `StatFs` só é consultado quando a tela inicial realmente mostra o cartão de armazenamento.
7. **Build de desempenho**: workflow publica `assemblePerformance`, baseado em Release, com R8 e `shrinkResources`, usando assinatura debug apenas para a fase alpha.
8. **Qualidade**: primeiros testes JVM cobrem busca, ocultos, prioridade de pastas e ordenação; CI também executa lint.

## Mantido sem alteração nesta etapa

- Visual/ícones XP.
- Fluxo de permissões e `MANAGE_EXTERNAL_STORAGE`.
- Operações de copiar, mover, excluir, compartilhar e visualizadores internos.
- Conjunto de dependências Compose/AndroidX, para evitar misturar atualização de bibliotecas com a medição desta otimização.

## Próximo passo recomendado

Medir a alpha.15 em aparelho real com pastas pequenas, médias e muito grandes. Depois criar módulo de Macrobenchmark + Baseline Profile para quantificar jank, tempo de abertura de pasta e inicialização. Com essa linha de base, atualizar Compose/AndroidX e revisar carregamento/decodificação dos PNGs usados na lista sem alterar a identidade visual.
