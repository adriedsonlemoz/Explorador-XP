# Explorador XP 0.1.0-alpha.97

## Correção de compilação: Gerar APK 91

- Corrigida incompatibilidade do `.dns { }` com OkHttp 4.12.0, usando implementação explícita de `okhttp3.Dns`.
- Corrigidas referências XML indisponíveis para o Android, com propriedades JAXP literais opcionais.
- Preservados bloqueio de DOCTYPE, entidades externas, XInclude e DTD externo; adicionada proteção extra no `EntityResolver`.
- Adicionados testes unitários para XML WebDAV válido e rejeição de conteúdo com entidades.
- Permanecem os recursos das alpha.95/96: cliente/servidor WebDAV HTTPS, seleção de pastas, pin SHA-256 do certificado e descoberta mDNS.

**Validação:** compilação Kotlin/JVM isolada e testes locais (consultar `docs/VALIDATION.json`); APK completo e execução em dois Androids ainda pendentes.

Versão: `0.1.0-alpha.97`  
Version code: `97`
