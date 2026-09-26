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
                "Nova tela de novidades aparece automaticamente uma única vez após cada atualização do aplicativo.",
                "A tela de novidades respeita a identidade visual do Explorador XP e não interrompe arquivos recebidos pelo Abrir com.",
                "Informações do dispositivo deixou de abrir como pop-up e agora ocupa toda a área disponível do aplicativo.",
                "As novidades da versão foram centralizadas para manter a tela automática e Sobre sempre com o mesmo conteúdo.",
            ),
        )
}
