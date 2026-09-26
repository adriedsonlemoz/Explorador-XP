package com.exploradorxp.app

/**
 * Mantém a explicação de Acesso ao uso independente da tela de Novidades.
 * A orientação foi introduzida na alpha.88 e deve aparecer uma única vez após
 * uma atualização real, somente quando o acesso ainda não estiver liberado.
 */
internal object UsageAccessEducationPolicy {
    const val INTRODUCED_VERSION_CODE = 88

    fun shouldShow(
        installedThroughUpdate: Boolean,
        currentVersionCode: Int,
        lastSeenEducationVersionCode: Int,
        hasUsageAccess: Boolean,
    ): Boolean = installedThroughUpdate &&
        !hasUsageAccess &&
        currentVersionCode >= INTRODUCED_VERSION_CODE &&
        lastSeenEducationVersionCode < INTRODUCED_VERSION_CODE
}
