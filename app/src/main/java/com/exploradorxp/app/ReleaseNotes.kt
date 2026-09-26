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
                "Tela de APK redesenhada com comparação clara entre o arquivo e o aplicativo já instalado.",
                "Versão, Target Android, Compile SDK, tamanho, assinatura e permissões agora podem ser comparados antes da instalação.",
                "Permissões novas e removidas são destacadas, com aviso especial quando uma nova permissão sensível aparece na atualização.",
                "O APK agora mostra SHA-256 real do arquivo, certificado, arquiteturas, data, compatibilidade e um resumo técnico do AndroidManifest.",
                "Novas ações permitem compartilhar o APK, abrir sua pasta, copiar pacote/SHA-256 e extrair o ícone sem sair do Explorador XP.",
                "O botão principal continua inteligente: Instalar, Atualizar, Reinstalar ou Instalar versão anterior conforme a situação.",
            )
        )
}
