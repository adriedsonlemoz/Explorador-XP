# Explorador XP 0.1.0-alpha.75

## Tela de instalação/inspeção de APK

A alpha.75 transforma o visualizador de APK em um painel de decisão antes de abrir o instalador do Android.

### Comparação com o aplicativo instalado
- versão e `versionCode`;
- `targetSdk`;
- `compileSdk`, quando exposto pelo Android;
- tamanho do APK e soma dos APKs base/splits instalados;
- quantidade de permissões;
- assinatura/certificado.

### Permissões
O inspector lê as permissões declaradas do APK e da versão instalada, destaca permissões novas e removidas e chama atenção quando uma permissão nova é classificada pelo Android como perigosa/sensível.

### Integridade e detalhes
- SHA-256 real do arquivo APK;
- fingerprints SHA-256 dos certificados;
- minSdk/targetSdk;
- arquiteturas nativas;
- data e tamanho do arquivo;
- resumo interpretado do AndroidManifest.

### Ações
Além de instalar/atualizar/reinstalar, a tela permite compartilhar o APK, abrir sua localização, copiar o packageName, copiar o SHA-256, extrair o ícone e abrir o resumo do manifesto.

## Novidades após atualização
`UpdateHighlightsScreen` continua sendo exibida somente uma vez após uma atualização normal. O conteúdo vem de `ReleaseNotes.current`, atualizado nesta versão com as mudanças da alpha.75.
