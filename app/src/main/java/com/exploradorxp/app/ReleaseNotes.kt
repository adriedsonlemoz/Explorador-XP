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
                "A tela Armazenamento foi recriada como um painel mais limpo: capacidade usada/livre/total, atalho direto para Aplicativos instalados, ações rápidas, categorias em lista, maiores pastas e arquivos grandes.",
                "O módulo Aplicativos instalados ganhou uma orientação mais clara para liberar Acesso ao uso. A mensagem amarela abre Informações do Explorador XP e explica o caminho dos 3 pontos > Permitir configurações restritas quando o Android exigir essa etapa.",
                "Após Novidades, a atualização mostra uma explicação única sobre Acesso ao uso quando a autorização ainda não estiver liberada. O recurso continua opcional e nenhuma função anterior foi removida.",
            )
        )
}
