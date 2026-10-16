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
                "Botão Salvar do ZIP agora grava automaticamente em Downloads/XP, sem escolher pasta.",
                "Após salvar o ZIP com sucesso, o Explorador XP fecha e retorna à tela anterior.",
                "Falhas de salvamento não fecham o aplicativo; verificação e extração continuam inalteradas.",
            )
        )
}
