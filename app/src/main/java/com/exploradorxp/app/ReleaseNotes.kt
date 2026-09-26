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
                "Informações do dispositivo agora tem apenas um botão de fechar: o X vermelho no topo, no mesmo estilo das janelas do Explorador XP.",
                "A Lixeira deixou de abrir como popup e agora ocupa toda a área do aplicativo, igual à tela de informações do dispositivo.",
                "A Lixeira mantém atualizar, restaurar, excluir e esvaziar, com confirmações apenas para ações destrutivas.",
                "Corrigido o erro do último build causado por conflito entre funções SHA-256 do editor e do novo inspetor de APK.",
            )
        )
}
