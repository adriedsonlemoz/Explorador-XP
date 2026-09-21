package com.exploradorxp.app

/**
 * Metadados do arquivo recebido via ACTION_VIEW.
 *
 * O conteúdo é aberto a partir de uma cópia de trabalho no cache. A URI original e as
 * permissões concedidas pelo aplicativo de origem permanecem separadas para que o editor
 * possa salvar de volta explicitamente quando houver permissão de escrita. O hash registra
 * a versão da origem copiada e permite detectar alterações externas antes de sobrescrever.
 */
data class ExternalOpenOrigin(
    val uri: String,
    val mimeType: String?,
    val displayName: String,
    val grantReadPermission: Boolean,
    val grantWritePermission: Boolean,
    val grantPersistablePermission: Boolean,
    val sourceSha256: String? = null,
) : java.io.Serializable
