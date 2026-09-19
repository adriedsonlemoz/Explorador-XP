# Alpha 42 — assinatura permanente

Versão: `0.1.0-alpha.42` (`versionCode 42`)

O Explorador XP deixa de usar a chave debug efêmera no APK `performance`. O GitHub Actions reconstrói um keystore permanente a partir de secrets do repositório e o Gradle usa essa chave nos build types `release` e `performance`.

## Secrets obrigatórios

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

O `github-manager.json` continua com o schema existente para não introduzir campo desconhecido ao GitHub Manager. A integração da assinatura acontece exclusivamente no workflow e nos secrets padrão do GitHub Actions.

A chave privada não é incluída no repositório.
