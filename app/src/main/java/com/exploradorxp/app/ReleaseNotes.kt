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
                "Cópias, movimentos e substituições agora preparam o arquivo antes da troca, preservando o original em caso de falha.",
                "Extração ZIP reforçada: substituição recuperável, limite de expansão e pré-visualização com cache limitado.",
                "Preview HTML e JavaScript mais seguro: scripts desligados inicialmente, ativação manual e sem rede ou acesso a arquivos locais.",
                "Busca avançada atualiza os resultados por lotes, reduzindo classificações repetidas.",
                "Consultas online de identificação do dispositivo agora exigem ativação manual nas preferências.",
            )
        )
}
