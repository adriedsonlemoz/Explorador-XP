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
                "Nova barra inferior fixa com atalhos Fotos, Vídeos, Downloads, Rede e Lixeira, inclusive nas telas internas.",
                "Downloads e Lixeira saíram da barra superior; agora ela oferece Atualizar e Favoritos/Colar.",
                "Fotos e Vídeos acessam arquivos do índice de mídia Android em diversas pastas.",
                "Rede local ganhou painel organizado e botão + com busca automática, IP, compartilhamento e aviso sobre SMB futuro.",
                "Cliente e servidor WebDAV HTTPS, pin SHA-256 e descoberta Wi-Fi permanecem preservados.",
            )
        )
}
