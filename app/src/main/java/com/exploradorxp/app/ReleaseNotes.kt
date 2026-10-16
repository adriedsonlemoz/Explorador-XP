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
                "Os quatro botões do ZIP agora ficam em uma única linha: Extrair, Salvar, Verificar e Info.",
                "Editor de texto/código com barra inferior de seleção, copiar, colar, recortar e selecionar tudo.",
                "Ações respeitam seleção e somente leitura; preservados salvamento e histórico do editor.",
            )
        )
}
