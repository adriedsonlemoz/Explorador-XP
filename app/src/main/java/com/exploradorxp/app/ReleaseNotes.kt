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
                "Informações do dispositivo agora separa arquitetura, ABIs do sistema e bitness real do processo do aplicativo sem deduzir a arquitetura física.",
                "CPU/SoC, GPU e processo de fabricação agora mostram a origem do dado e evitam nomes comerciais ambíguos.",
                "RAM, armazenamento, bateria, rede e SIM ganharam métricas mais claras usando somente valores realmente expostos pelo Android.",
                "Sensores disponíveis agora podem mostrar fabricante, resolução, alcance, consumo e leituras reais do SensorManager.",
                "Foi adicionado um diagnóstico rápido de recursos e o relatório técnico passou a acompanhar os novos campos e suas fontes.",
            )
        )
}
