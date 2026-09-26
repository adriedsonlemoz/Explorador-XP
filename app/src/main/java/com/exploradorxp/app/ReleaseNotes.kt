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
                "O visualizador de APK agora permite solicitar a desinstalação de um aplicativo diretamente, sem precisar abrir primeiro a tela Detalhes do app.",
                "A nova ação Instalar versão anterior aparece quando o APK tem versionCode menor e usa um fluxo assistido: remover a versão atual e, após a remoção ser confirmada, abrir a instalação do APK selecionado.",
                "O downgrade exibe as versões instalada e selecionada e alerta que a desinstalação pode apagar dados locais antes de qualquer ação destrutiva.",
                "As ações existentes de instalar, atualizar, reinstalar, abrir, gerenciar, compartilhar, extrair ícone e inspecionar manifesto foram preservadas.",
            )
        )
}
