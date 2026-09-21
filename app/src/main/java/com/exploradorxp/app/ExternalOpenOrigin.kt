package com.exploradorxp.app

/**
 * Metadados do arquivo recebido via ACTION_VIEW.
 *
 * Nesta versão o conteúdo ainda é copiado para o cache e aberto em modo somente leitura.
 * A origem é mantida separada do arquivo temporário para permitir que uma versão futura
 * implemente "Salvar de volta" de forma explícita e segura, sem confundir cache com origem.
 */
data class ExternalOpenOrigin(
    val uri: String,
    val mimeType: String?,
    val displayName: String,
    val grantReadPermission: Boolean,
    val grantWritePermission: Boolean,
    val grantPersistablePermission: Boolean,
) : java.io.Serializable
