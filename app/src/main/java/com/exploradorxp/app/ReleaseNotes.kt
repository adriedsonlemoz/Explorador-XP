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
                "A tela de APK ficou mais compacta, com menos margens e espaços entre os cartões para aproveitar melhor a altura da tela.",
                "A comparação com o aplicativo instalado inicia recolhida e continua disponível com um toque, reduzindo a necessidade de rolagem na abertura.",
                "O botão Fechar do visualizador foi padronizado com o mesmo botão XP inferior usado nas demais telas, incluindo o ícone de fechamento.",
                "O workflow do GitHub agora cria uma release separada para cada versão, preservando o histórico e fazendo as versões novas aparecerem como releases mais recentes.",
            )
        )
}
