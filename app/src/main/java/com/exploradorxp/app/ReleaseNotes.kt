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
                "A tela Armazenamento foi recriada com um painel mais limpo: indicador circular do espaço usado, cartões de usado/livre/total e melhor aproveitamento da tela.",
                "Aplicativos instalados agora tem um atalho destacado dentro de Armazenamento, enquanto Analisar/Atualizar/Cancelar e Lixeira ficam em ações compactas logo abaixo do resumo.",
                "A análise detalhada ganhou resumo da última varredura e seções reorganizadas para categorias, maiores pastas e arquivos grandes, mantendo a separação entre dados totais do Android e arquivos realmente analisados.",
            )
        )
}
