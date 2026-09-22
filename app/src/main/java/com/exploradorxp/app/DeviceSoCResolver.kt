package com.exploradorxp.app

import java.util.Locale

/**
 * Catálogo conservador de SoCs conhecidos.
 *
 * O Android fornece SOC_MODEL/SOC_MANUFACTURER apenas em versões recentes e fabricantes podem
 * publicar identificadores técnicos diferentes. O resolvedor só promove um nome comercial quando
 * encontra um alias exato conhecido; caso contrário os valores originais continuam sendo exibidos.
 * Novos chips devem ser acrescentados como dados nesta tabela, sem condicionais na interface.
 */
internal object DeviceSoCResolver {
    data class Identity(
        val commercialName: String?,
        val manufacturer: String?,
        val technicalId: String,
        val gpu: String? = null,
        val processNm: Int? = null,
        val matchedCatalog: Boolean = false,
    ) {
        val processLabel: String? get() = processNm?.let { "$it nm" }

        val primaryLabel: String
            get() = commercialName ?: technicalId

        val technicalLabel: String
            get() = listOfNotNull(manufacturer?.takeIf { it.isNotBlank() }, technicalId.takeIf { it.isNotBlank() })
                .joinToString(" ")
                .ifBlank { technicalId }
    }

    private data class CatalogEntry(
        val commercialName: String,
        val manufacturer: String,
        val aliases: Set<String>,
        val gpu: String? = null,
        val processNm: Int? = null,
    )

    private val catalog = listOf(
        CatalogEntry("Helio P35", "MediaTek", aliases("MT6765"), "PowerVR GE8320", 12),
        CatalogEntry("Helio G35", "MediaTek", aliases("MT6765G"), "PowerVR GE8320", 12),
        CatalogEntry("Helio P22", "MediaTek", aliases("MT6762"), "PowerVR GE8320", 12),
        CatalogEntry("Helio A22", "MediaTek", aliases("MT6761"), "PowerVR GE8320", 12),

        CatalogEntry("Snapdragon 660", "Qualcomm", aliases("SDM660"), "Adreno 512", 14),
        CatalogEntry("Snapdragon 665", "Qualcomm", aliases("SM6125"), "Adreno 610", 11),
        CatalogEntry("Snapdragon 662", "Qualcomm", aliases("SM6115"), "Adreno 610", 11),
        CatalogEntry("Snapdragon 680", "Qualcomm", aliases("SM6225"), "Adreno 610", 6),
        CatalogEntry("Snapdragon 685", "Qualcomm", aliases("SM6225AD"), "Adreno 610", 6),
        CatalogEntry("Snapdragon 695 5G", "Qualcomm", aliases("SM6375"), "Adreno 619", 6),
        CatalogEntry("Snapdragon 480 5G", "Qualcomm", aliases("SM4350"), "Adreno 619", 8),
        CatalogEntry("Snapdragon 4 Gen 1", "Qualcomm", aliases("SM4375"), "Adreno 619", 6),
        CatalogEntry("Snapdragon 4 Gen 2", "Qualcomm", aliases("SM4450"), "Adreno 613", 4),
        CatalogEntry("Snapdragon 6 Gen 1", "Qualcomm", aliases("SM6450"), "Adreno 710", 4),
        CatalogEntry("Snapdragon 7s Gen 2", "Qualcomm", aliases("SM7435AB"), "Adreno 710", 4),
        CatalogEntry("Snapdragon 7+ Gen 2", "Qualcomm", aliases("SM7475AB"), "Adreno 725", 4),
        CatalogEntry("Snapdragon 7 Gen 3", "Qualcomm", aliases("SM7550AB"), "Adreno 720", 4),
        CatalogEntry("Snapdragon 855", "Qualcomm", aliases("SM8150"), "Adreno 640", 7),
        CatalogEntry("Snapdragon 865", "Qualcomm", aliases("SM8250"), "Adreno 650", 7),
        CatalogEntry("Snapdragon 888", "Qualcomm", aliases("SM8350"), "Adreno 660", 5),
        CatalogEntry("Snapdragon 8 Gen 1", "Qualcomm", aliases("SM8450"), "Adreno 730", 4),
        CatalogEntry("Snapdragon 8+ Gen 1", "Qualcomm", aliases("SM8475"), "Adreno 730", 4),
        CatalogEntry("Snapdragon 8 Gen 2", "Qualcomm", aliases("SM8550AB", "SM8550AC"), "Adreno 740", 4),
        CatalogEntry("Snapdragon 8 Gen 3", "Qualcomm", aliases("SM8650AB", "SM8650AC"), "Adreno 750", 4),
    )

    private val entriesByAlias: Map<String, CatalogEntry> = buildMap {
        catalog.forEach { entry -> entry.aliases.forEach { alias -> put(alias, entry) } }
    }

    fun resolve(socManufacturer: String?, socModel: String?, hardware: String): Identity {
        val model = socModel.orEmpty().trim()
        val hardwareValue = hardware.trim().takeUnless { it.equals("Não disponível", ignoreCase = true) }.orEmpty()
        val technicalId = model.ifBlank { hardwareValue }.ifBlank { "Não disponível" }

        val candidates = listOf(model, hardwareValue)
            .filter { it.isNotBlank() }
            .map(::normalizeIdentifier)
            .filter { it.isNotBlank() }
            .distinct()

        val entry = candidates.firstNotNullOfOrNull(entriesByAlias::get)
        if (entry != null) {
            return Identity(
                commercialName = entry.commercialName,
                manufacturer = entry.manufacturer,
                technicalId = model.ifBlank { hardwareValue },
                gpu = entry.gpu,
                processNm = entry.processNm,
                matchedCatalog = true,
            )
        }

        return Identity(
            commercialName = null,
            manufacturer = canonicalManufacturer(socManufacturer),
            technicalId = technicalId,
            matchedCatalog = false,
        )
    }

    private fun aliases(vararg values: String): Set<String> = values.mapTo(linkedSetOf(), ::normalizeIdentifier)

    private fun normalizeIdentifier(value: String): String = value
        .uppercase(Locale.ROOT)
        .filter { it.isLetterOrDigit() }

    private fun canonicalManufacturer(value: String?): String? {
        val raw = value.orEmpty().trim()
        if (raw.isBlank()) return null
        val normalized = raw.lowercase(Locale.ROOT)
        return when {
            "qualcomm" in normalized -> "Qualcomm"
            "mediatek" in normalized || normalized == "mtk" -> "MediaTek"
            "samsung" in normalized -> "Samsung"
            "unisoc" in normalized || "spreadtrum" in normalized -> "UNISOC"
            "google" in normalized -> "Google"
            else -> raw
        }
    }
}
