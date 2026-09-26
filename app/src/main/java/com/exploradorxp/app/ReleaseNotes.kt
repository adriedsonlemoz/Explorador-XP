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
                "Os ícones do Explorador XP foram padronizados em uma única linguagem visual vetorial, substituindo o antigo pacote de PNGs misturados.",
                "Arquivos agora usam ícones por categoria — documentos, imagens, áudio, vídeo, compactados, código, aplicativos e outros — mantendo a identificação sem poluir a interface.",
                "Pastas, armazenamento, navegação, ações e estados também passaram a usar vetores consistentes e nítidos em qualquer densidade de tela.",
                "O ícone do aplicativo/launcher foi convertido para um Adaptive Icon vetorial, sem bitmap incorporado.",
                "A remoção dos bitmaps de interface reduz recursos gráficos no código-fonte e elimina o antigo cache de decodificação de PNG usado pelas listas.",
            )
        )
}
