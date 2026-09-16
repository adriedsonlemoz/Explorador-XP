package com.exploradorxp.app

/**
 * Projeções puras da listagem já carregada do armazenamento.
 *
 * Busca, filtro de ocultos e ordenação não devem provocar uma nova leitura de disco.
 * Esse objeto permite executar essas operações sobre o snapshot em memória e também
 * torna a lógica testável por testes JVM simples.
 */
object ExplorerItemTransforms {
    fun apply(
        snapshot: List<FileItem>,
        query: String,
        sortMode: SortMode,
        showHidden: Boolean,
        foldersFirst: Boolean,
    ): List<FileItem> {
        val normalizedQuery = query.trim()
        val filtered = snapshot.asSequence()
            .filter { showHidden || !it.isHidden }
            .filter { normalizedQuery.isBlank() || it.name.contains(normalizedQuery, ignoreCase = true) }
            .toList()

        val detailComparator = when (sortMode) {
            SortMode.NAME -> compareBy<FileItem, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
            SortMode.DATE -> compareByDescending<FileItem> { it.modifiedAt }
            SortMode.SIZE -> compareByDescending<FileItem> { it.size }
            SortMode.TYPE -> compareBy<FileItem> { it.extension }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        }

        return if (foldersFirst) {
            filtered.sortedWith(compareByDescending<FileItem> { it.isDirectory }.then(detailComparator))
        } else {
            filtered.sortedWith(detailComparator)
        }
    }
}
