# Explorador XP 0.1.0-alpha.101

## Correção de compilação R8, build 94

- Corrigidos 13 avisos de classes ausentes que impediam `:app:minifyPerformanceWithR8`.
- Regras restritas em `app/proguard-rules.pro` para referências Java EL (MBassador) e GSS/Kerberos (SMBJ) indisponíveis no Android.
- R8/minificação e assinatura do APK permanecem habilitados; não são adicionadas bibliotecas Java SE incompatíveis.
- Permanecem disponíveis cliente SMB2/SMB3 com autenticação por usuário/senha, WebDAV HTTPS, cópia seletiva, Quick Share, atalhos inferiores e central de rede.
- Sem alterações nas telas de Aplicativos Instalados e Galeria. Essas melhorias estão registradas em `docs/BACKLOG_UI_ALPHA102.md`.

**Validação:** no workflow 94 da alpha.100, `testDebugUnitTest` e `lintDebug` foram bem-sucedidos, porém `minifyPerformanceWithR8` falhou. A alpha.101 ainda exige execução de `gradle :app:assemblePerformance` e teste de conexão SMB em dispositivo.

Versão: `0.1.0-alpha.101`  
Version code: `101`
