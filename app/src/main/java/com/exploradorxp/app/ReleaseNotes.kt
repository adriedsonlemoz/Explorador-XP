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
                "APK: ícones reais na lista e grade quando o Android consegue lê-los, com cache.",
                "Exibir: Lista compacta com ícones menores e preferência persistente.",
                "Exibir: classificação por nome, data, tamanho e tipo, e Pastas primeiro.",
                "Aplicativos instalados: filtro discreto por tamanho/estado e ordenação por datas.",
            )
        )
}
