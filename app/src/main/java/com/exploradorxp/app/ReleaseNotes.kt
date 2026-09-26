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
                "A tela Sobre o Explorador XP agora abre como tela completa, no mesmo padrão adotado para a Lixeira e Informações do dispositivo.",
                "O layout Sobre deixou de usar popup/modal e passou a ocupar toda a área útil do aplicativo, mantendo o estilo XP.",
                "Ajuda, Informações técnicas e Fechar continuam disponíveis na barra inferior padronizada.",
                "Versão e documentação foram sincronizadas para a alpha.79.",
            )
        )
}
