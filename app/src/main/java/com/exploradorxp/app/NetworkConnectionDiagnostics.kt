package com.exploradorxp.app

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.EOFException
import java.io.InterruptedIOException
import java.io.IOException
import javax.net.ssl.SSLException

/** Descrições por categoria, nunca incluem senha, tokens, URL ou conteúdo bruto das exceções. */
internal data class NetworkFailure(
    val code: String,
    val title: String,
    val explanation: String,
    val guidance: String,
) {
    fun copyable(): String = "Explorador XP - diagnóstico de rede\n" +
        "Código: $code\n$title\n$explanation\nO que verificar: $guidance\n" +
        "Informações de autenticação não incluídas."

    companion object {
        fun from(error: Throwable): NetworkFailure {
            val causes = generateSequence(error) { it.cause }.take(8).toList()
            val message = causes.mapNotNull { it.message }.joinToString(" ").lowercase()
            return when {
                causes.any { it is SSLException } ||
                    listOf("certificate", "certificado", "pin sha", "fingerprint", "handshake").any { it in message } ->
                    NetworkFailure("TLS", "Não foi possível confirmar a segurança",
                        "O certificado do outro aparelho não corresponde ao esperado, ou a conexão TLS falhou.",
                        "Leia novamente o QR mostrado pelo aparelho correto. Não ignore a verificação do certificado.")
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
