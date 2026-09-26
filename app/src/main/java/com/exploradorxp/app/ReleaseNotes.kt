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
                "Corrigido o fechamento ao abrir Ferramentas > Informações do dispositivo. Um espaçamento inválido na interface causava o erro ao mostrar os dados do aparelho.",
                "A busca opcional do nome e da imagem do modelo pela internet continua disponível, com fonte indicada e funcionamento offline.",
                "A tela Armazenamento renovada, Aplicativos instalados e as demais ferramentas continuam disponíveis.",
            )
        )
}
