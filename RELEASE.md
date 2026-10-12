# Explorador XP 0.1.0-alpha.104

## Correção Android Lint: build Gerar APK 97

- Corrigida a falha do `:app:lintDebug` (`PermissionImpliesUnsupportedChromeOsHardware`) causada pela permissão `CAMERA` sem indicação de hardware opcional.
- `android.hardware.camera` e `android.hardware.camera.autofocus` declarados com `required="false"` em `AndroidManifest.xml`.
- A leitura de QR continua disponível em aparelhos com câmera; o aplicativo não exige câmera para instalação.
- Mantidos cliente e servidor WebDAV HTTPS, SMB2/SMB3, Galeria, Aplicativos Instalados e demais recursos da alpha.103.

**Versão:** `0.1.0-alpha.104`  
**Version code:** `104`

**Validação:** corrigida a causa identificada no log do build 97, preservadas as demais funções. O build completo no GitHub Actions e os testes em aparelhos ainda são necessários.
