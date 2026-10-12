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
                "Painel de Ferramentas: oito atalhos em grade de quadrados, com ícones e nomes curtos.",
                "Organização de arquivos: ordenação e Pastas primeiro agora ficam em Configurações.",
                "Lixeira: contagem sobre o ícone e espaço ocupado ao lado, sem legendas fora do atalho.",
                "Novidades da atualização: exibidas apenas as alterações desta versão.",
            )
        )
}
