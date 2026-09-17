# Explorador XP — alpha.36 — correção do build Media3

## Falha recebida

O workflow `Gerar APK` falhava ainda em `:app:checkDebugAarMetadata`, antes de compilar os fontes Kotlin. Os logs informavam que os artefatos `androidx.media3` 1.11.1 exigem compilação contra Android API 36 ou superior, enquanto o aplicativo está em `compileSdk 35`. O próprio log também registra que o Android Gradle Plugin 8.7.3 recomenda no máximo API 35.

## Correção aplicada

- Mantidos `compileSdk = 35` e `targetSdk = 35`.
- Mantido Android Gradle Plugin 8.7.3.
- Mantido Gradle 8.9 no workflow.
- `media3-exoplayer` alterado de 1.11.1 para **1.9.4**.
- `media3-ui` alterado de 1.11.1 para **1.9.4**.
- Nenhuma função do `VideoPlayerViewer.kt` foi removida ou alterada nesta correção.

A linha Media3 1.9.x usa `compileSdk = 35` segundo as notas oficiais do AndroidX. Assim, a correção elimina a incompatibilidade que interrompia o build sem forçar a adoção de API 36 e de uma nova cadeia AGP/Gradle neste momento.

## Escopo preservado

O editor de texto/código da alpha.35, visualizadores, Lixeira, armazenamento, informações do dispositivo, recursos visuais XP e workflows continuam funcionalmente iguais. Não foram criadas imagens nem mockups.

## Validação

- versões sincronizadas para `0.1.0-alpha.36` / `versionCode 36`;
- arquivos JSON, XML e YAML verificados;
- referências de drawables verificadas;
- recursos de imagem comparados com a alpha.35;
- workflow `Gerar APK` preservado;
- build Android completo não executado localmente por ausência de Android SDK/Gradle no ambiente desta edição. A causa específica registrada no CI anterior foi removida no projeto.
