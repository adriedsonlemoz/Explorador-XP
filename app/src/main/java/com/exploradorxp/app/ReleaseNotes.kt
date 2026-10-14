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
                "Filtros rápidos de aplicativos redesenhados em chips compactos com ícones, numa única linha rolável.",
                "Identificação visual de Todos, Usuário, Sistema e das classificações por nome, tamanho e datas.",
                "Seleção dos filtros mantida, com estados acessíveis e botão Início da barra inferior preservado.",
            )
        )
}
