# Explorador XP 0.1.0-alpha.91

## Correção

- Corrigido o erro de compilação identificado no workflow **Gerar APK 86**.
- `DeviceInfoScreen` construía `DeviceImageResult.Unavailable` sem os parâmetros obrigatórios `checkedAtEpochMs` e `fromCache`; a chamada agora usa a assinatura completa.
- A correção mantém o comportamento de segurança da tela **Informações do dispositivo**: em falha de coleta, o app mostra fallback amigável em vez de encerrar.

## Preservado

- A tela **Armazenamento** redesenhada na alpha.90 permanece intacta.
- Aplicativos instalados, instalador/visualizador de APK, Lixeira, análise de armazenamento e demais funções anteriores não foram removidos.

Versão: `0.1.0-alpha.91`  
Version code: `91`
