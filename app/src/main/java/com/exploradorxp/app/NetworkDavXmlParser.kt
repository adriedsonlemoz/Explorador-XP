package com.exploradorxp.app

import org.w3c.dom.Document
import org.xml.sax.SAXException
import javax.xml.parsers.DocumentBuilderFactory

/**
 * XML WebDAV não confiável: sem DOCTYPE, entidades externas, XInclude ou DTD externo.
 * As propriedades JAXP são literais porque algumas APIs Android não expõem
 * javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD / ACCESS_EXTERNAL_SCHEMA.
 */
internal object NetworkDavXmlParser {
    private const val ACCESS_EXTERNAL_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD"
    private const val ACCESS_EXTERNAL_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema"

    fun parse(xml: ByteArray): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
            // JAXP em alguns Androids não reconhece as propriedades opcionais.
            runCatching { setAttribute(ACCESS_EXTERNAL_DTD, "") }
            runCatching { setAttribute(ACCESS_EXTERNAL_SCHEMA, "") }
        }
        return factory.newDocumentBuilder().apply {
            // Defesa adicional caso a implementação ignore algum controle opcional.
            setEntityResolver { _, _ -> throw SAXException("Entidade XML externa bloqueada") }
        }.parse(xml.inputStream())
    }
}
