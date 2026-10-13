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
                "Armazenamento organizado em abas Categorias, Pastas e Arquivos, com filtros de arquivos grandes e dados OBB.",
                "Resumo medido dos aplicativos com indicação de resultados parciais e classificação rápida por tamanho e datas.",
                "Abertura de arquivos ZIP sem extensão recebidos do GitHub Manager, com verificação de assinatura antes de copiar.",
            )
        )
}
