package com.exploradorxp.app

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

private fun qrBitmap(value: String): Pair<Bitmap, Int> {
    // A matriz em resolução mínima define o número exato de módulos. Replicar pixels em escala
    // inteira evita barras de larguras diferentes criadas pelo redimensionamento do ZXing.
    val grid = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, 1, 1,
        mapOf(EncodeHintType.MARGIN to 4, EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M))
    val module = (768 / grid.width).coerceAtLeast(1)
    val pixels = grid.width * module
    val colors = IntArray(pixels * pixels) { index ->
        if (grid.get((index % pixels) / module, (index / pixels) / module))
            android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    return Bitmap.createBitmap(colors, pixels, pixels, Bitmap.Config.ARGB_8888) to grid.width
}

@Composable
internal fun NetworkQrPreview(connection: NetworkQrConnection) {
    val value = remember(connection) { NetworkQrCode.create(connection) }
    val (bitmap, modules) = remember(value) { qrBitmap(value) }
    val density = LocalDensity.current
    // Tamanho visível é um múltiplo EXATO dos módulos na resolução física da tela.
    val qrSize = with(density) {
        val modulePixels = (248.dp.roundToPx() / modules).coerceAtLeast(1)
        (modules * modulePixels).toDp()
    }
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Image(bitmap.asImageBitmap(), contentDescription = "QR para conectar ao compartilhamento deste celular",
                modifier = Modifier.size(qrSize), contentScale = ContentScale.Fit, filterQuality = FilterQuality.None)
        }
        Text("QR com senha temporária. Mostre apenas a pessoas autorizadas.",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
    }
}
