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
                "O ícone de Armazenamento interno foi ajustado para não parecer recortado em telas com diferentes densidades.",
                "Os cartões de Vídeos, Imagens, Áudio, APK e Outros ficaram mais baixos e compactos.",
                "Tamanho, quantidade de arquivos e porcentagem continuam visíveis, agora com melhor aproveitamento vertical.",
                "A lógica e os números da análise de armazenamento foram preservados sem misturar dados do dispositivo com arquivos analisados.",
            )
        )
}
