# Explorador XP — alpha.51

Versão: `0.1.0-alpha.51` (`versionCode 51`)

## Integração com “Abrir com”

A `MainActivity` agora declara `ACTION_VIEW` apenas para MIME types que possuem tratamento interno no Explorador XP. Não foi usado `*/*` nem `application/octet-stream`, evitando que o aplicativo apareça para formatos binários sem suporte.

Grupos anunciados ao Android:

- texto e código (`text/*` e MIME types de JSON/XML/JavaScript/YAML/TOML/SQL/PHP/Shell);
- ZIP (`application/zip`, `application/x-zip` e `application/x-zip-compressed`);
- PDF;
- APK (`application/vnd.android.package-archive` e variante `application/x-android-package-archive`);
- JPEG, PNG, GIF, BMP e WebP;
- vídeos MP4/M4V/3GP/WebM/MKV/AVI/MOV;
- áudio MP3/WAV/M4A/AAC/OGG/FLAC/Opus.

RAR, 7Z, SVG e outros formatos apenas reconhecidos visualmente, mas sem leitor interno, não são anunciados como abertura externa.

## Entrada por URI

Arquivos fornecidos por outros aplicativos normalmente chegam como `content://`. Como os visualizadores atuais trabalham com `java.io.File`, a alpha.51:

1. lê o nome real por `OpenableColumns.DISPLAY_NAME`;
2. valida extensão/MIME em `ExternalOpenSupport`;
3. copia o conteúdo para `cacheDir/external-open/<requisição>`;
4. abre a cópia no leitor interno;
5. remove o diretório temporário ao fechar ou substituir a abertura.

A abertura externa funciona mesmo sem `MANAGE_EXTERNAL_STORAGE`, porque a permissão temporária da URI concedida pelo aplicativo de origem é suficiente para ler o conteúdo recebido.

## Segurança de edição

Arquivos de texto/código recebidos externamente entram em modo somente leitura. Isso evita salvar alterações apenas na cópia de cache e induzir o usuário a acreditar que o documento original foi modificado. Arquivos abertos normalmente pelo próprio Explorador XP continuam editáveis.

## Ciclo da Activity

`MainActivity` passa a usar `launchMode="singleTop"` e trata `onNewIntent()`, permitindo receber outro arquivo pelo Android quando a atividade já está no topo. O filtro também aceita a categoria `BROWSABLE` sem registrar esquemas web: continua limitado a `content://` e `file://`.

Durante a cópia temporária o app mostra **Abrindo arquivo...** e não exibe a solicitação de acesso global ao armazenamento. Ao fechar uma abertura externa, a Activity termina e retorna ao aplicativo de origem.

## Validação local

- `ExternalOpenSupport.kt` compilado isoladamente com `kotlinc`;
- smoke test JVM: ZIP/Markdown/texto/JSON aceitos e RAR/binário genérico rejeitados;
- `AndroidManifest.xml` validado como XML bem-formado;
- build Android completo depende do CI/GitHub, pois o pacote-fonte continua sem Gradle Wrapper local.
