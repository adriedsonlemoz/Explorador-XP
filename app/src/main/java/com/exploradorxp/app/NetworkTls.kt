package com.exploradorxp.app

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Principal
import java.security.PrivateKey
import java.net.Socket
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedKeyManager
import java.security.cert.X509Certificate
import java.security.cert.CertificateException
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal

/** Categoria reconhecível para distinguir PIN errado de TLS sem reduzir a segurança. */
internal class NetworkPinMismatchException : CertificateException("O SHA-256 do certificado remoto difere do QR.")

internal object NetworkTls {
    private const val ALIAS = "explorador_xp_network_tls_v1"

    fun certificateFingerprint(certificate: X509Certificate): String =
        MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
            .joinToString("") { "%02X".format(it) }

    fun serverContext(): Pair<SSLContext, String> {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(ALIAS)) {
            val spec = KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            ).setAlgorithmParameterSpec(java.security.spec.RSAKeyGenParameterSpec(2048,
                java.security.spec.RSAKeyGenParameterSpec.F4))
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1, KeyProperties.SIGNATURE_PADDING_RSA_PSS)
                .setCertificateSubject(X500Principal("CN=Explorador XP Rede Local"))
                .build()
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
                .apply { initialize(spec) }.generateKeyPair()
        }
        // Nunca deixar o seletor TLS escolher outra chave do AndroidKeyStore.
        // O SHA-256 divulgado no QR precisa corresponder ao certificado REALMENTE entregue.
        val cert = keyStore.getCertificate(ALIAS) as? X509Certificate
            ?: error("Certificado de rede ausente")
        val key = keyStore.getKey(ALIAS, null) as? PrivateKey
            ?: error("Chave privada de rede ausente")
        check(key.algorithm.equals("RSA", ignoreCase = true) &&
            cert.publicKey.algorithm.equals("RSA", ignoreCase = true)) {
            "Identidade TLS incompatível com RSA"
        }
        val chain = arrayOf(cert)
        val keyManager = object : X509ExtendedKeyManager() {
            private fun identityFor(keyType: String?): String? =
                if (keyType != null && (keyType.equals("RSA", ignoreCase = true) ||
                        keyType.equals("RSASSA-PSS", ignoreCase = true))) ALIAS else null

            override fun getCertificateChain(alias: String?): Array<X509Certificate>? =
                if (alias == ALIAS) chain.clone() else null
            override fun getPrivateKey(alias: String?): PrivateKey? = if (alias == ALIAS) key else null
            override fun getServerAliases(keyType: String?, issuers: Array<Principal>?): Array<String>? =
                identityFor(keyType)?.let { arrayOf(it) }
            override fun chooseServerAlias(keyType: String?, issuers: Array<Principal>?, socket: Socket?): String? =
                identityFor(keyType)
            override fun chooseEngineServerAlias(keyType: String?, issuers: Array<Principal>?, engine: SSLEngine?): String? =
                identityFor(keyType)
            override fun getClientAliases(keyType: String?, issuers: Array<Principal>?): Array<String>? = null
            override fun chooseClientAlias(keyTypes: Array<String>?, issuers: Array<Principal>?, socket: Socket?): String? = null
            override fun chooseEngineClientAlias(keyTypes: Array<String>?, issuers: Array<Principal>?, engine: SSLEngine?): String? = null
        }
        return SSLContext.getInstance("TLS").apply { init(arrayOf(keyManager), null, null) } to certificateFingerprint(cert)
    }

    /** Certificate pin required; hostname verification is replaced by exact DER certificate comparison. */
    fun pinnedClientContext(expectedFingerprint: String): Pair<SSLContext, X509TrustManager> {
        val expected = expectedFingerprint.replace(":", "").trim().uppercase()
        require(expected.matches(Regex("[A-F0-9]{64}"))) { "Impressão SHA-256 inválida (64 caracteres hexadecimais)." }
        val trust = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {
                throw java.security.cert.CertificateException("Cliente não aceito")
            }
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {
                val received = chain?.firstOrNull() ?: throw java.security.cert.CertificateException("Sem certificado")
                val fingerprint = certificateFingerprint(received)
                if (!MessageDigest.isEqual(fingerprint.toByteArray(), expected.toByteArray())) {
                    throw NetworkPinMismatchException()
                }
                received.checkValidity()
            }
        }
        return SSLContext.getInstance("TLS").apply { init(null, arrayOf<TrustManager>(trust), null) } to trust
    }
}
