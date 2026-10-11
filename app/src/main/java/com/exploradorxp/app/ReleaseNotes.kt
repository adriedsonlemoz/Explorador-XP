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
                "Aplicativos Instalados: ações Abrir, Limpar, Remover e Sistema compactas junto ao nome.",
                "Estado do aplicativo passa a indicar Habilitado ou Desativado, sem confundir com execução.",
                "Fotos e Vídeos: miniaturas reais, tamanhos de arquivos, grade/lista, paginação e seleção múltipla.",
                "Galeria: copiar sem sobrescrever ou enviar fotos e vídeos selecionados pelo compartilhamento Android.",
                "Rede Wi-Fi: mostrar QR Code temporário com endereço, senha e certificado do compartilhamento WebDAV.",
                "Outro celular: ler QR pela câmera, preencher dados e confirmar antes de conectar.",
                "SMB2/SMB3, WebDAV HTTPS e as funcionalidades anteriores permanecem disponíveis.",
            )
        )
}
