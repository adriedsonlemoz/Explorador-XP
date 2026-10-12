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
                "Aplicativos: ações abaixo do nome e indicadores Sistema/Estado ao lado do título.",
                "Cabeçalhos: seta Voltar corrigida; ícones claros e sem fundo branco.",
                "Ferramentas: grade de atalhos compacta, com quatro colunas em telas comuns.",
                "Lixeira: contador dentro do ícone, sem tamanho exibido ao lado.",
            )
        )
}
