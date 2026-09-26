package com.exploradorxp.app

data class ReleaseInfo(
    val versionName: String,
    val versionCode: Int,
    val title: String,
    val changes: List<String>,
)

/**
 * Fonte única das novidades exibidas após uma atualização e em Sobre > Novidades desta versão.
 * Atualize a lista abaixo junto com versionName/versionCode em toda nova versão do aplicativo.
 */
object ReleaseNotes {
    val current: ReleaseInfo
        get() = ReleaseInfo(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            title = "O Explorador XP foi atualizado",
            changes = listOf(
                "Corrigida a abertura de Ferramentas > Aplicativos instalados: o cabeçalho agora recebe uma ação dedicada para abrir o módulo, eliminando o erro de compilação causado por acesso a estado fora do escopo.",
                "O gerenciador de aplicativos da alpha.86 foi preservado integralmente, incluindo lista, busca, filtros, tamanhos, permissões e ações seguras de abrir/desinstalar/limpar dados via Android.",
                "As demais funções do Explorador XP permanecem inalteradas nesta versão de correção.",
            )
        )
}
