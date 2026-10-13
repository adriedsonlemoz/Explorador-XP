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
                "Corrigidos dois erros do Android Lint no carregamento assíncrono de músicas e capas.",
                "Mantidas a fila, as tags e a arte do álbum fora da thread principal, com estado Compose seguro.",
                "Adicionada verificação preventiva de estados assíncronos ao workflow de compilação.",
            )
        )
}
