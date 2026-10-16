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
                "Visualizador ZIP com Extrair, Salvar e Verificar na mesma linha; informações seguem no menu.",
                "APK: versão instalada e versão do arquivo destacadas no cartão superior.",
                "APK instalado: Reinstalar/Atualizar, Abrir, Desinstalar e Gerenciar em uma linha compacta.",
            )
        )
}
