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
                "Fotos e Vídeos: agora abrem nas pastas de origem, sem misturar todos os arquivos.",
                "Galerias: menus Arquivo, Editar, Exibir, Favoritos, Ferramentas e Ajuda, com Voltar, Avançar, Início e Subir funcionais.",
                "Seleção total, cópia e compartilhamento respeitam a pasta de mídia aberta.",
                "Aplicativos instalados: lista mais compacta com ícones menores, preservando tamanho e pacote.",
                "Painel de Ferramentas: cartões equilibrados com ícones maiores e pequenas descrições.",
                "Correção do teste ReleaseNotesScopeTest que interrompeu o workflow Gerar APK 107.",
            )
        )
}
