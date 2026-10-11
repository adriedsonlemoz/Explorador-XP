# Explorador XP alpha.97: correção de compilação do Gerar APK 91

## Diagnóstico reproduzido a partir dos logs

O workflow falhou em `:app:compileDebugKotlin`, após checkout/configuração do Gradle, com quatro mensagens do compilador em `NetworkDavClient.kt`:

- Linha 58, `Argument type mismatch` e `Cannot infer type for this parameter`: a expressão lambda passada a `.dns(...)` não correspondeu à interface `okhttp3.Dns` esperada no build.
- Linhas 119/120, referências inexistentes a `javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD` e `ACCESS_EXTERNAL_SCHEMA` no ambiente Android.

## Alterações

- `NetworkDavClient.kt`: `.dns(object : Dns { override fun lookup(hostname: String): List<InetAddress> ... })`, validação da identidade do host e preservação dos endereços locais previamente resolvidos para reduzir risco de DNS rebinding.
- `NetworkDavXmlParser.kt`: parsing seguro com recursos anti-XXE. As URIs de propriedades JAXP são strings literais, para não depender de constantes ausentes. São opcionais e têm `runCatching` por diferenças entre providers, mas DOCTYPE/entidades externas são bloqueados por recursos obrigatórios e `EntityResolver`.
- `NetworkDavXmlParserTest.kt`: três testes unitários do parser.

## Critérios de validação

1. Rodar `gradle :app:testDebugUnitTest --stacktrace` no GitHub Actions (inclui compilação Kotlin do app).
2. Rodar `gradle :app:lintDebug --stacktrace` e `gradle :app:assemblePerformance --stacktrace` usando Secrets de assinatura já configurados.
3. Instalar somente após build aprovado; testar conexão HTTPS WebDAV, pin SHA-256, listagem, download, upload desativado por padrão, anúncio mDNS e cancelamento.

A compilação Android completa não foi executada no ambiente local. Não considerar a alpha.97 aprovada até que o CI e os testes em aparelhos confirmem.
