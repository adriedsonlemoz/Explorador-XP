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
                "Ajuda agora abre em tela completa, mantendo os tópicos expansíveis e o botão Fechar na barra inferior.",
                "Armazenamento agora abre em tela completa e mantém análise, cancelamento e abertura das pastas encontradas.",
                "A análise por tipo foi redesenhada em cartões compactos, com no máximo cinco categorias visíveis.",
                "Os números agora diferenciam claramente o armazenamento total informado pelo Android dos arquivos efetivamente analisados pelo Explorador XP.",
                "Contagens e porcentagens foram revisadas para usar apenas o mesmo conjunto de arquivos analisados, sem estimar dados inacessíveis.",
            )
        )
}
