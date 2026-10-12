# Explorador XP 0.1.0-alpha.107

## Diagnóstico e robustez TLS em Rede local

- Servidor HTTPS apresenta somente a chave e o certificado usados no SHA-256 gerado para o QR Code.
- Cliente conversa exclusivamente HTTP/1.1 com o servidor WebDAV interno, sem negociação HTTP/2 desnecessária.
- Compartilhar mostra a última falha de negociação TLS recebida no servidor e permite copiar o diagnóstico sem IP ou credenciais.
- Conectar por IP orienta comparar diagnóstico nos dois aparelhos.
- Não desativa o TLS nem a verificação do certificado. Não é possível comprovar a causa específica do erro observado apenas pelo print.

**Versão:** `0.1.0-alpha.107`  
**Version code:** `107`

**Validação:** testes Kotlin locais e sintaxe verificados; build Android completo e autenticação entre dois aparelhos reais pendentes.
