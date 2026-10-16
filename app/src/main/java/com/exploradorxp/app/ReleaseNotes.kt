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
                "Corrigida a compilação do visualizador ZIP: importações AndroidX para permissão de notificações durante a extração.",
                "Verificação preventiva de importações reforçada para detectar a mesma falha antes do build.",
                "Mantidos Extrair, Verificar e Salvar, com cópia automática do ZIP para Armazenamento interno/XP.",
            )
        )
}
