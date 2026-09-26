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
                "Padronizado o fechamento das telas: o X vermelho do topo foi removido das janelas e telas do Explorador XP.",
                "Informações do dispositivo e Lixeira voltaram a usar o botão Fechar na barra inferior, mantendo o mesmo padrão visual.",
                "O visualizador interno também passa a fechar pela barra inferior, sem o X vermelho no cabeçalho.",
                "Diálogos de ação continuam usando Cancelar/Fechar na parte inferior; abas e painéis internos mantêm seus controles próprios.",
                "Corrigido o teste do inspetor de APK que ainda chamava o nome antigo da função SHA-256 e fazia o workflow falhar.",
            )
        )
}
