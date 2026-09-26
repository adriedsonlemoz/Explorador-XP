# Explorador XP 0.1.0-alpha.77

## Padronização do fechamento
O X vermelho foi removido dos cabeçalhos das telas e janelas. O padrão passa a ser:
- telas completas e informativas: botão **Fechar** na barra inferior;
- diálogos de ação: **Cancelar** ou **Fechar** na parte inferior;
- abas e painéis internos: seus controles pequenos continuam locais.

Foram ajustados Informações do dispositivo, Lixeira, cabeçalhos compartilhados, visualizador interno e o estado de preparação de ZIP.

## Correção do workflow
O workflow falhava nos testes com:
`Cannot access 'fun sha256File(file: File): String': it is private in file.`

O teste `ApkInspectorSupportTest` agora referencia `sha256ApkFile`, helper atual do inspetor de APK.

Versão: `0.1.0-alpha.77` / `versionCode 77`.
