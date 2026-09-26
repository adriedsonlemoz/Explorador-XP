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
                "Novo ícone oficial do Explorador XP aplicado ao Android com suporte a Adaptive Icon.",
                "Corrigida a integração do ícone que fazia o build falhar ao localizar a cor launcher_blue.",
                "O ícone mantém margem de segurança para não cortar a pasta e a lupa em formatos circulares ou arredondados.",
                "Ícones legacy e round continuam disponíveis para launchers e versões antigas do Android.",
            ),
        )
}
