# Explorador XP alpha.129: ZIP e APK

## ZIP
- Botão **Info** retirado dos atalhos. Informações permanecem em **⋮ > Informações**.
- Botões remanescentes: **Extrair / Salvar / Verificar** na mesma linha.

## APK
- Mostra versão instalada e do arquivo APK junto ao título do aplicativo, ambas a partir de dados do Android. Ausência de versionName usa versionCode.
- Ações compactas em quatro colunas em telas comuns, incluindo estado de desabilitado; ação de downgrade preserva confirmação.
- Instalar app novo permanece botão inteiro.

## Validação pendente
- Gradle `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assemblePerformance` no GitHub Actions.
- Testes em dispositivos 360 dp, fonte ampliada, APK instalado/não instalado, versões iguais/diferentes, bloqueio de assinatura e downgrade.
