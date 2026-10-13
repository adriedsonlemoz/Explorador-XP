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
                "Visualizador ZIP simplificado: ações compactas, busca opcional, Info resumida e Abrir com no menu.",
                "Lixeira compacta com filtros, agrupamento por data e restauração/exclusão em lote com confirmação.",
                "Informações do dispositivo: cópia por toque de especificações públicas e catálogo offline do Redmi Note 11 Pro+ 5G com Dimensity 920.",
            )
        )
}
