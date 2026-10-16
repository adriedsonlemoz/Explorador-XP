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
                "Adicionado botão Salvar ZIP, com escolha de pasta pelo Android e cópia sem recompressão.",
                "Ações de arquivos ZIP reorganizadas em duas linhas para telas pequenas.",
                "Cópia em streaming, com verificação do tamanho e limpeza de destino incompleto quando possível.",
            )
        )
}
