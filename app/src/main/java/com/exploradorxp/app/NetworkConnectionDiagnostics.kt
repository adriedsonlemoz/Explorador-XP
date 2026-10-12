package com.exploradorxp.app

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.EOFException
import java.io.InterruptedIOException
import java.io.IOException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException

/** Descrições por categoria, nunca incluem senha, tokens, URL ou conteúdo bruto das exceções. */
internal data class NetworkFailure(
    val code: String,
    val title: String,
    val explanation: String,
    val guidance: String,
    val technical: String = "",
) {
    fun copyable(): String = "Explorador XP - diagnóstico de rede\n" +
        "Código: $code\n$title\n$explanation\nO que verificar: $guidance\n" +
        (if (technical.isNotEmpty()) "Detalhe técnico: $technical\n" else "") +
        "Informações de autenticação não incluídas."

    companion object {
        fun from(error: Throwable): NetworkFailure {
            val causes = generateSequence(error) { it.cause }.take(8).toList()
            val message = causes.mapNotNull { it.message }.joinToString(" ").lowercase()
            // Para TLS, diferenciar pin divergente, datas do certificado e negociação.
            // Nomes de classe são seguros para copiar; mensagens brutas podem conter segredos.
            val technical = causes.joinToString(" → ") { it.javaClass.simpleName }
                .take(180)
            return when {
                causes.any { it is NetworkPinMismatchException } ||
                    "certificado remoto diferente" in message ->
                    NetworkFailure("TLS_PIN", "Certificado diferente do QR",
                        "O aparelho que respondeu apresentou um certificado SHA-256 diferente do informado.",
                        "Confira se o endereço IP pertence ao celular certo. No aparelho que compartilha, " +
                            "desligue e ative o compartilhamento, gere um QR novo e leia novamente. " +
                            "Não ignore a verificação do certificado.", technical)
                causes.any { it is CertificateExpiredException || it is CertificateNotYetValidException } ->
                    NetworkFailure("TLS_DATE", "Certificado fora da validade",
                        "O período de validade do certificado não foi aceito.",
                        "Confira data, hora e fuso dos dois celulares. Reinicie o compartilhamento e leia o QR de novo. " +
                            "Não ignore a verificação do certificado.", technical)
                causes.any { it is SSLHandshakeException || it is SSLException } ||
                    listOf("certificate", "certificado", "pin sha", "fingerprint", "handshake").any { it in message } ->
                    NetworkFailure("TLS_HANDSHAKE", "A conexão HTTPS segura falhou",
                        "Os celulares não concluíram a negociação TLS. Isso não prova que o QR esteja incorreto.",
                        "Mantenha Compartilhar ativo no outro celular e confira o endereço IP da tela do servidor. " +
                            "Gere e leia um QR novo em ambos com a mesma versão. " +
                            "Se persistir, copie o diagnóstico deste aparelho e também o que aparece em " +
                            "Compartilhar no celular servidor. Não ignore a verificação do certificado.", technical)
                "http 401" in message || "http 403" in message || "unauthorized" in message ->
                    NetworkFailure("AUTH", "Acesso não autorizado",
                        "O outro aparelho não aceitou as credenciais ou não permite o acesso.",
                        "Ative o compartilhamento e use a senha e o QR da sessão atual.")
                causes.any { it is UnknownHostException } ->
                    NetworkFailure("ADDRESS", "Endereço não encontrado",
                        "O nome ou endereço do aparelho não pôde ser resolvido.",
                        "Confira o IP exibido no outro aparelho e se ambos usam a mesma rede.")
                causes.any { it is SocketTimeoutException || it is InterruptedIOException } ->
                    NetworkFailure("TIMEOUT", "O aparelho não respondeu a tempo",
                        "A tentativa de conexão excedeu o tempo de espera.",
                        "Verifique o Wi-Fi, o servidor ativo e se o hotspot ou roteador permite comunicação entre aparelhos.")
                causes.any { it is ConnectException || it is NoRouteToHostException } ->
                    NetworkFailure("REACHABILITY", "Não foi possível alcançar o aparelho",
                        "O serviço pode estar desligado, em outra rede ou bloqueado.",
                        "Confira IP, porta, Wi-Fi e se o compartilhamento continua ligado.")
                causes.any { it is EOFException } || "connection closed" in message ||
                    "closed connection" in message || "connection reset" in message ||
                    "unexpected end of stream" in message ->
                    NetworkFailure("CLOSED", "A conexão foi encerrada",
                        "A comunicação terminou antes de o aplicativo receber uma resposta completa. O motivo exato ainda não foi identificado.",
                        "Verifique o servidor no outro aparelho, o IP e a rede. Tente um QR novo e conecte outra vez.")
                "http 404" in message ->
                    NetworkFailure("NOT_FOUND", "Pasta remota não encontrada",
                        "O endereço respondeu, mas não encontrou a pasta solicitada.",
                        "Confira a pasta compartilhada no outro aparelho.")
                error is IllegalArgumentException || error is IllegalStateException ->
                    NetworkFailure("INPUT", "Dados de conexão inválidos",
                        "O endereço ou os dados informados não passaram na validação.",
                        "Leia o QR do outro aparelho ou corrija endereço, usuário e código SHA-256.")
                else -> NetworkFailure("NETWORK", "Não foi possível concluir a operação",
                    "Ocorreu uma falha durante a comunicação. O aplicativo não conseguiu determinar a causa com segurança.",
                    "Verifique se o outro aparelho compartilha a pasta e se a rede está funcionando.")
            }
        }
    }
}

internal object NetworkConnectionData {
    fun format(address: String, username: String, password: String, pin: String): String =
        "Explorador XP - conexão local\n" +
        "Endereço: $address\nUsuário: $username\nSenha temporária: $password\n" +
        "Código SHA-256: $pin\n" +
        "ATENÇÃO: contém uma senha. Compartilhe apenas com pessoa autorizada."
}
