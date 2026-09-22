package com.exploradorxp.app

/** Regras puras do visualizador para manter os gestos previsíveis e testáveis. */
internal object ImageViewerLogic {
    const val MIN_SCALE = 1f
    const val MAX_SCALE = 6f
    const val DOUBLE_TAP_SCALE = 2.5f

    fun clampScale(value: Float): Float = value.coerceIn(MIN_SCALE, MAX_SCALE)

    fun doubleTapScale(current: Float): Float =
        if (current > MIN_SCALE + 0.01f) MIN_SCALE else DOUBLE_TAP_SCALE

    /** Índice, na galeria antes da remoção, do arquivo que deve continuar visível. */
    fun replacementSourceIndex(currentIndex: Int, sizeBeforeRemoval: Int): Int? {
        if (sizeBeforeRemoval <= 1 || currentIndex !in 0 until sizeBeforeRemoval) return null
        return if (currentIndex < sizeBeforeRemoval - 1) currentIndex + 1 else currentIndex - 1
    }
}
