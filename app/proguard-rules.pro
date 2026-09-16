# Regras específicas do Explorador XP.
#
# A base usa majoritariamente APIs Android/Compose sem reflexão própria. O R8 pode
# portanto otimizar normalmente usando proguard-android-optimize.txt e as regras
# publicadas pelas dependências. Adicione regras aqui somente quando uma biblioteca
# realmente exigir keep explícito; keeps amplos desnecessários reduzem a otimização.
