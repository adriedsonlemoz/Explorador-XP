package com.exploradorxp.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Gera uma imagem de compartilhamento somente com dados já presentes em DeviceInfoSnapshot.
 * Não captura a tela, não lê arquivos do usuário e não inclui identificadores únicos.
 */
object DeviceInfoShare {
    fun renderBitmap(info: DeviceInfoSnapshot): Bitmap {
        val width = 1080
        val height = 2048
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(246, 250, 255))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val left = 64f
        val right = width - 64f

        // Cabeçalho
        paint.color = Color.rgb(10, 103, 224)
        canvas.drawRoundRect(RectF(36f, 36f, width - 36f, 260f), 42f, 42f, paint)
        drawText(canvas, paint, "Explorador XP", 72f, 105f, 44f, Color.WHITE, true)
        drawText(canvas, paint, "Informações do dispositivo", 72f, 166f, 34f, Color.WHITE, true)
        drawText(canvas, paint, "Dados reais informados pelo Android", 72f, 215f, 25f, Color.argb(220, 255, 255, 255), false)

        // Resumo do aparelho
        drawCard(canvas, paint, left, 300f, right, 500f)
        drawText(canvas, paint, info.deviceName, 92f, 365f, 44f, Color.rgb(20, 39, 68), true)
        drawText(canvas, paint, "${info.manufacturer} • ${info.model}", 92f, 414f, 27f, Color.rgb(97, 113, 138), false)
        drawText(canvas, paint, "Android ${info.androidVersion}  •  ${humanBytes(info.ramTotalBytes)} RAM  •  ${humanBytes(info.storageTotalBytes)}", 92f, 467f, 27f, Color.rgb(11, 92, 189), true)

        // Informações principais
        var y = 540f
        y = drawInfoBlock(
            canvas, paint, left, y, right, "Sistema",
            listOf(
                "Android" to "${info.androidVersion} • API ${info.apiLevel}",
                "Patch de segurança" to info.securityPatch,
                "Processador" to processorForShare(info),
                "CPU" to "${info.cpuCores} núcleos • ${if (info.is64Bit) "64 bits" else "32 bits"}",
                "Tela" to "${info.displayWidthPx} × ${info.displayHeightPx}px${if (info.refreshRateHz > 0f) " • ${info.refreshRateHz.toInt()} Hz" else ""}",
            ),
        )
        y += 24f
        y = drawInfoBlock(
            canvas, paint, left, y, right, "Estado atual",
            listOf(
                "RAM livre" to humanBytes(info.ramAvailableBytes),
                "Armazenamento livre" to humanBytes(info.storageAvailableBytes),
                "Bateria" to (info.batteryPercent?.let { "$it% • ${info.batteryStatus}" } ?: info.batteryStatus),
                "Sensores detectados" to info.sensorCount.toString(),
            ),
        )

        y += 32f
        drawText(canvas, paint, "Recursos e sensores", left, y, 31f, Color.rgb(16, 44, 87), true)
        y += 42f
        val chips = buildList {
            add("NFC" to info.hasNfc)
            add("Bluetooth" to info.hasBluetooth)
            add("GPS" to info.hasGps)
            add("Câmera" to info.hasCamera)
            add("Digital" to info.hasFingerprint)
            add("Acelerômetro" to info.hasAccelerometer)
            add("Giroscópio" to info.hasGyroscope)
            add("Bússola" to info.hasMagnetometer)
            add("Luz" to info.hasLightSensor)
            add("Proximidade" to info.hasProximitySensor)
            add("Barômetro" to info.hasBarometer)
            add("Passos" to (info.hasStepCounter || info.hasStepDetector))
        }
        val chipWidth = (right - left - 24f) / 2f
        chips.chunked(2).forEach { row ->
            row.forEachIndexed { index, item ->
                val x = left + index * (chipWidth + 24f)
                val available = item.second
                paint.color = if (available) Color.rgb(233, 247, 238) else Color.rgb(239, 243, 248)
                canvas.drawRoundRect(RectF(x, y, x + chipWidth, y + 66f), 20f, 20f, paint)
                drawText(
                    canvas,
                    paint,
                    "${if (available) "✓" else "–"} ${item.first}",
                    x + 22f,
                    y + 43f,
                    24f,
                    if (available) Color.rgb(31, 125, 62) else Color.rgb(105, 120, 141),
                    true,
                )
            }
            y += 78f
        }

        drawText(canvas, paint, "Gerado pelo Explorador XP ${info.appVersionName}", left, height - 92f, 23f, Color.rgb(97, 113, 138), false)
        drawText(canvas, paint, "Sem IMEI, serial, Android ID, MAC, localização ou arquivos pessoais.", left, height - 52f, 20f, Color.rgb(97, 113, 138), false)
        return bitmap
    }

    fun savePng(context: Context, uri: Uri, info: DeviceInfoSnapshot) {
        val bitmap = renderBitmap(info)
        try {
            context.contentResolver.openOutputStream(uri, "w")?.use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "Falha ao gravar PNG." }
            } ?: error("Não foi possível abrir o destino.")
        } finally {
            bitmap.recycle()
        }
    }

    fun createShareIntent(context: Context, info: DeviceInfoSnapshot): Intent {
        val dir = File(context.cacheDir, "device_info_share").apply { mkdirs() }
        val file = File(dir, deviceInfoImageFileName())
        val bitmap = renderBitmap(info)
        try {
            FileOutputStream(file).use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "Falha ao criar PNG." }
            }
        } finally {
            bitmap.recycle()
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, info.toShareSummary())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun drawCard(canvas: Canvas, paint: Paint, left: Float, top: Float, right: Float, bottom: Float) {
        paint.color = Color.WHITE
        canvas.drawRoundRect(RectF(left, top, right, bottom), 32f, 32f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.rgb(205, 221, 239)
        canvas.drawRoundRect(RectF(left, top, right, bottom), 32f, 32f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawInfoBlock(
        canvas: Canvas,
        paint: Paint,
        left: Float,
        top: Float,
        right: Float,
        title: String,
        rows: List<Pair<String, String>>,
    ): Float {
        val height = 86f + rows.size * 54f
        drawCard(canvas, paint, left, top, right, top + height)
        drawText(canvas, paint, title, left + 28f, top + 50f, 31f, Color.rgb(16, 44, 87), true)
        var y = top + 96f
        rows.forEach { (label, value) ->
            drawText(canvas, paint, label, left + 28f, y, 23f, Color.rgb(97, 113, 138), false)
            drawRightText(canvas, paint, value, right - 28f, y, 23f, Color.rgb(20, 39, 68), true)
            y += 54f
        }
        return top + height
    }

    private fun drawText(
        canvas: Canvas,
        paint: Paint,
        text: String,
        x: Float,
        baseline: Float,
        size: Float,
        color: Int,
        bold: Boolean,
    ) {
        paint.textSize = size
        paint.color = color
        paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(text, x, baseline, paint)
    }

    private fun drawRightText(
        canvas: Canvas,
        paint: Paint,
        text: String,
        x: Float,
        baseline: Float,
        size: Float,
        color: Int,
        bold: Boolean,
    ) {
        paint.textSize = size
        paint.color = color
        paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        paint.textAlign = Paint.Align.RIGHT
        val safeText = if (paint.measureText(text) <= 520f) text else text.take(34) + "…"
        canvas.drawText(safeText, x, baseline, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun processorForShare(info: DeviceInfoSnapshot): String =
        listOfNotNull(info.socManufacturer, info.socModel).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { info.hardware }
}
