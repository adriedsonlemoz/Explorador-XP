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
                "Corrigida a falha de compilação relatada no workflow Gerar APK 89.",
                "Definidos limites de segurança para ZIP, extração, preview e arquivos externos.",
                "Cache de pré-visualizações agora elimina arquivos antigos e controla o espaço total utilizado.",
                "Mantidas as proteções de substituição de arquivos e visualização HTML introduzidas na versão anterior.",
            )
        )
}
