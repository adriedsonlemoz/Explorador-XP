# Alpha.101: correção do erro R8 do build 94

## Evidência

- Falhou `:app:minifyPerformanceWithR8`, após sucesso em `:app:testDebugUnitTest` e `:app:lintDebug`.
- 13 classes ausentes, listadas na saída do R8: 7 `javax.el` (filtro opcional de MBassador) e 6 `org.ietf.jgss` (caminho GSS/Kerberos de SMBJ).

## Correção

- `app/proguard-rules.pro`: avisos ignorados **somente** para as 13 classes concretas faltantes.
- O cliente SMB do aplicativo utiliza `AuthenticationContext` com usuário/senha e não oferece Kerberos/GSS nem filtros EL.
- Não foram adicionadas dependências de Java SE, modificados protocolos ou desabilitado R8.
- Atenção: uma regra `-dontwarn` não implementa classes ausentes. Caso a autenticação ou os filtros opcionais venham a ser usados, será preciso fornecer implementação Android compatível.

## Verificação requerida

1. Rodar no GitHub Actions `gradle :app:testDebugUnitTest`, `gradle :app:lintDebug` e `gradle :app:assemblePerformance`.
2. Verificar que `minifyPerformanceWithR8` conclui sem classes ausentes e que o APK é assinado.
3. Instalar APK `performance`, conectar a compartilhamento SMB2/3 com usuário/senha e copiar um arquivo pequeno.
4. Verificar que a conexão WebDAV HTTPS e os atalhos continuam funcionais.

O ambiente local de edição não possui Gradle nem Android SDK, então a etapa de R8 e os testes reais permanecem pendentes.
