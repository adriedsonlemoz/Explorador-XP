package com.exploradorxp.app

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * Renderiza ícones locais do Explorador XP.
 *
 * Desde a alpha.83 os ícones da interface são VectorDrawable XML. Não há mais
 * decodificação/cópia de PNG nem cache de Bitmap para ícones; o Compose resolve
 * o vetor diretamente e o mesmo recurso permanece nítido em qualquer densidade.
 */
@Composable
fun CachedResourceIcon(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Image(
        painter = painterResource(resId),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}

/**
 * Mantido por compatibilidade com chamadas existentes. Vetores não precisam de
 * pré-decodificação, então esta função intencionalmente não executa trabalho.
 */
@Composable
fun PreloadResourceIcons(@Suppress("UNUSED_PARAMETER") resIds: List<Int>) = Unit
