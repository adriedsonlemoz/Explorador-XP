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
                "Resumo do APK: a versão do arquivo continua à direita do nome e o status colorido agora fica ao lado da versão instalada, sem o texto fixo 'Versão Instalada'.",
                "Quando o app já está instalado, a linha abaixo do nome mostra a versão instalada em azul e, ao lado, o selo de status em verde, azul ou vermelho conforme atualização, mesma versão ou anterior.",
                "Ações rápidas do APK reorganizadas para Abrir, Gerenciar, Desinstalar e Instalar/Atualizar, preservando as validações de assinatura e compatibilidade.",
            )
        )
}
