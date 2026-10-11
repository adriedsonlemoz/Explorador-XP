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
                "Corrigida a falha do R8 na geração do APK otimizado com SMB2/SMB3.",
                "Rede Wi-Fi: abrir pastas SMB2/SMB3 em Windows, NAS ou Android com compartilhamento autorizado.",
                "Informar IP, pasta compartilhada, usuário e senha; busca opcional de anúncios SMB na rede.",
                "Selecionar vários arquivos remotos e copiar para uma pasta escolhida neste celular, sem substituir arquivos locais.",
                "Enviar arquivos pelo compartilhamento do Android (Quick Share quando disponível).",
                "WebDAV HTTPS e barra inferior fixa preservados; acesso exige compartilhamento ativo no outro aparelho.",
            )
        )
}
