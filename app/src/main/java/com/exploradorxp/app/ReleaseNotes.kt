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
                "Mini-player de música durante a navegação, com pausar, avançar, voltar e encerrar.",
                "Reprodução em segundo plano pelo Media3 MediaSessionService e controles na notificação/tela bloqueada.",
                "Mesmo player e fila ao voltar para a música, inclusive por controles do sistema.",
            )
        )
}
