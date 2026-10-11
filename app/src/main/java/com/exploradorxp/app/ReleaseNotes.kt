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
                "Nova busca de aparelhos pela rede Wi-Fi: Ferramentas > Rede local > Procurar aparelhos.",
                "Servidor WebDAV anunciado automaticamente enquanto você compartilhar uma pasta.",
                "Escolher um aparelho preenche o endereço, mas sempre exige senha e conferência do SHA-256.",
                "Busca encerrada ao sair da tela para economizar bateria; conexão manual continua disponível.",
            )
        )
}
