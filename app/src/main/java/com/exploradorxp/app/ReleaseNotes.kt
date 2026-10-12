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
                "Rede local: servidor TLS apresenta somente o certificado SHA-256 usado no QR.",
                "Cliente WebDAV utiliza HTTP/1.1, mesmo protocolo do servidor embutido.",
                "Compartilhar: exibe última falha TLS recebida e permite copiar diagnóstico seguro.",
                "Conexão HTTPS: orientações para comparar os diagnósticos dos dois celulares.",
                "Rede: atalho Quick Share removido; a transferência entre Exploradores XP permanece pelo Wi-Fi com WebDAV HTTPS.",
                "Conectar por IP: ações QR, Segurança e Conectar lado a lado; Conectar em verde quando pronto.",
                "QR com módulos mais nítidos; leitor com orientação em português e vibração no QR válido e na conexão.",
                "Erros HTTPS diferenciados: código de PIN, validade do certificado e negociação TLS com dicas práticas.",
                "Diagnósticos técnicos copiados sem senha; pin SHA-256 obrigatório foi preservado.",
                "Rede local redesenhada com três cartões: Acessar, Compartilhar e Ler QR.",
                "Compartilhamento compacto: pastas, ativação e informações de acesso em blocos curtos.",
                "QR de conexão em janela com câmera integrada; não exige abrir a tela de captura inteira.",
                "Endereço IP simplificado: QR ao lado do campo, botão Conectar e opções de segurança recolhidas.",
                "Falhas de rede explicadas em português com ajuda (?) e diagnóstico copiável sem credenciais.",
                "QR do servidor sob demanda e cópia do IP ou dos dados completos com aviso de senha sensível.",
                "Correção do build 97: câmera declarada opcional para o leitor de QR, permitindo instalação em dispositivos sem câmera.",
                "Correção do build 96: referência ensureActive resolvida no carregamento e cópia da galeria.",
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
