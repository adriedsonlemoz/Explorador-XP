package com.exploradorxp.app

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

private fun qrBitmap(value: String, pixels: Int = 600): Bitmap {
    val matrix = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, pixels, pixels,
        mapOf(EncodeHintType.MARGIN to 2))
    val colors = IntArray(pixels * pixels) { index ->
        if (matrix.get(index % pixels, index / pixels)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    return Bitmap.createBitmap(colors, pixels, pixels, Bitmap.Config.ARGB_8888)
}

@Composable
internal fun NetworkQrPreview(connection: NetworkQrConnection) {
    val value = remember(connection) { NetworkQrCode.create(connection) }
    val bitmap = remember(value) { qrBitmap(value) }
    Column {
        Image(bitmap.asImageBitmap(), contentDescription = "QR para conectar ao compartilhamento deste celular",
            modifier = Modifier.size(240.dp))
        Text("Mostre o QR somente a quem você autorizar: ele contém a senha temporária.")
    }
}
