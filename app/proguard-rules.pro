# Regras específicas do Explorador XP.
#
# A base usa majoritariamente APIs Android/Compose sem reflexão própria. O R8 pode
# portanto otimizar normalmente usando proguard-android-optimize.txt e as regras
# publicadas pelas dependências. Adicione regras aqui somente quando uma biblioteca
# realmente exigir keep explícito; keeps amplos desnecessários reduzem a otimização.

# alpha.101: correção do R8 (Gerar APK 94, minifyPerformanceWithR8).
# A SMBJ 0.14.0 traz dependências de APIs opcionais do Java SE não incluídas
# no Android: EL usada em filtros opcionais do MBassador e GSS/Kerberos.
# O cliente do Explorador XP usa AuthenticationContext (usuário/senha NTLM)
# e não expõe autenticação GSS/Kerberos nem filtros de eventos por EL.
# `-dontwarn` permite ao R8 otimizar referências desses caminhos opcionais,
# sem adicionar APIs Java SE artificiais ao APK e sem desligar a minificação.
# Se futuramente esses caminhos forem usados, implementar suporte Android
# compatível antes de remover as restrições. NÃO usar '-dontwarn **'.
-dontwarn javax.el.BeanELResolver
-dontwarn javax.el.ELContext
-dontwarn javax.el.ELResolver
-dontwarn javax.el.ExpressionFactory
-dontwarn javax.el.FunctionMapper
-dontwarn javax.el.ValueExpression
-dontwarn javax.el.VariableMapper
-dontwarn org.ietf.jgss.GSSContext
-dontwarn org.ietf.jgss.GSSCredential
-dontwarn org.ietf.jgss.GSSException
-dontwarn org.ietf.jgss.GSSManager
-dontwarn org.ietf.jgss.GSSName
-dontwarn org.ietf.jgss.Oid
