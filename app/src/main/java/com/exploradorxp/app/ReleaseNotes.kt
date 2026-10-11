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
                "Corrigidos erros de compilação do cliente WebDAV identificados no build Gerar APK 91.",
                "Cliente HTTPS mantém a restrição de DNS local e a conferência do certificado SHA-256.",
                "Parser WebDAV reforçado contra XML malicioso e entidades externas.",
                "Compartilhamento de pastas e busca de dispositivos na rede Wi-Fi permanecem disponíveis.",
            )
        )
}
