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
                "ZIP: Extrair, Verificar e Salvar na mesma linha; Salvar copia automaticamente para Armazenamento interno/XP.",
                "Nomes de ZIP repetidos recebem numeração, sem sobrescrever arquivos da pasta XP.",
                "A cópia do ZIP preserva os bytes originais e remove arquivo parcial se falhar.",
            )
        )
}
