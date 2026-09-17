package com.exploradorxp.app

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Formata os textos exibidos na listagem antes de eles chegarem à composição.
 * Os formatadores java.time são imutáveis/thread-safe e podem ser reutilizados.
 */
object FileDisplayFormatter {
    private val locale = Locale.forLanguageTag("pt-BR")
    private val zone = ZoneId.systemDefault()
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .withZone(zone)
    private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(locale)
        .withZone(zone)

    fun date(time: Long): String {
        if (time <= 0L) return "Data desconhecida"
        return dateFormatter.format(Instant.ofEpochMilli(time))
    }

    fun time(time: Long): String {
        if (time <= 0L) return "--:--"
        return timeFormatter.format(Instant.ofEpochMilli(time))
    }

    fun bytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var index = -1
        while (value >= 1024 && index < units.lastIndex) {
            value /= 1024.0
            index++
        }
        return if (value >= 100) String.format(locale, "%.0f %s", value, units[index])
        else String.format(locale, "%.1f %s", value, units[index])
    }

    fun listDetail(modifiedAt: Long, size: Long, isDirectory: Boolean, extension: String): String {
        val type = if (isDirectory) "Pasta de arquivos" else FileTypeClassifier.labelForExtension(extension)
        val timestamp = "${date(modifiedAt)} • ${time(modifiedAt)}"
        return if (isDirectory) "$type • $timestamp" else "$type • ${bytes(size)} • $timestamp"
    }

    fun gridDetail(size: Long, isDirectory: Boolean, extension: String): String {
        return if (isDirectory) "Pasta" else "${FileTypeClassifier.labelForExtension(extension)} • ${bytes(size)}"
    }
}
