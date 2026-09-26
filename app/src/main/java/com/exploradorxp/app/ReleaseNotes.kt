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
                "Informações do dispositivo agora identifica o nome comercial por correspondência exata de fabricante/marca + código do modelo, mantendo os dados brutos do Android separados.",
                "Foi adicionado catálogo local versionado e cache para atualização usando a lista pública de dispositivos compatíveis com Google Play, sem bloquear o modo offline.",
                "Quando há identificação confiável, o app pode procurar uma imagem real do modelo via Wikidata/Wikimedia Commons e só aceita entidades com nome e fabricante compatíveis.",
                "Imagem, URL, autor e licença ficam em cache; resultados ambíguos, falhas de rede ou modelos sem imagem usam o ícone genérico em vez de arriscar uma foto errada.",
                "Ferramentas > Configurações ganhou a opção para desligar consultas externas de identificação/imagem; desativada, a tela usa somente dados locais e do Android.",
                "A tela continua separando arquitetura, ABIs, SoC, GPU, RAM, armazenamento, bateria, rede e sensores sem inventar valores ausentes.",
            )
        )
}
