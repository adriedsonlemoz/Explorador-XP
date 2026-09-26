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
                "Ferramentas ganhou o módulo Aplicativos instalados, com lista pesquisável de apps de usuário e do sistema, ícone real, pacote, versão, estado e tamanho.",
                "Com Acesso ao uso concedido, o Explorador XP mostra armazenamento completo por aplicativo, separando código, dados e cache; sem essa autorização, mostra somente o tamanho real dos APKs instalados.",
                "A ficha de cada aplicativo reúne versão/versionCode, SDK mínimo e alvo, datas de instalação/atualização, instalador informado pelo Android e permissões declaradas com estado concedido/não concedido.",
                "O gerenciador permite abrir aplicativos, solicitar desinstalação pelo fluxo oficial do Android, copiar o pacote e acessar os detalhes do sistema. Limpar dados respeita a proteção do Android e encaminha para a tela oficial do aplicativo para conclusão pelo usuário.",
                "Aplicativos do sistema são identificados separadamente; o Explorador XP não promete remoção quando o Android pode bloquear a ação ou limitar a operação à remoção de atualizações.",
                "As funções anteriores do explorador, visualizadores, instalador/inspector de APK, informações do dispositivo, armazenamento, lixeira, busca e editor permanecem disponíveis.",
            )
        )
}
