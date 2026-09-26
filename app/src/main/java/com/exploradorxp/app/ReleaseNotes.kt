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
                "Corrigido um erro de compilação no fallback de Informações do dispositivo: a resposta de imagem indisponível agora usa todos os campos obrigatórios da estrutura interna.",
                "A proteção contra falhas de Informações do dispositivo continua ativa: em erro de coleta, a tela deve mostrar uma mensagem amigável em vez de fechar o aplicativo.",
                "A nova tela Armazenamento da alpha.90 e todas as funções anteriores foram preservadas.",
            )
        )
}
