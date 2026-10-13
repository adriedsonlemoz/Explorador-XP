# Alpha.118: player de música local

## Alterações

- O áudio não usa mais `VideoView`. O novo `AudioPlayerViewer` foi construído com Compose + `Media3 ExoPlayer`, versão de dependência já existente no projeto.
- As capas são extraídas **apenas do arquivo atual** usando `MediaMetadataRetriever` em I/O. Dados das tags podem não existir; não são inferidos.
- A playlist reúne músicas com extensões MP3, WAV, M4A, AAC, OGG, FLAC e OPUS da **mesma pasta**, sem percorrer subpastas. Ordenação pelo nome e limite de 1000 para evitar listas excessivas.
- O player responde a interrupções de foco e a desconexão de fones pelos recursos do Media3 e pausa no evento ON_STOP. Ele não implementa ainda um serviço de música em segundo plano.
- A mídia é liberada no descarte da tela; o item atual é refletido no título/rodapé do visualizador, sem recriar o reprodutor a cada faixa.
- JPEG/PNG embutidos acima de 3 MB não são decodificados; a arte é reduzida ao carregar para evitar explosões de memória.

## Limitações e validação

- A compatibilidade real do codec depende do Android e dos decodificadores do aparelho.
- Não há acesso a música online, permissões novas, varredura recursiva, download de capas ou serviços em background.
- Gradle Android, reprodução física e alterações de configuração precisam ser testadas no CI/dispositivo.
- Mini-player/MediaSessionService foram implementados posteriormente na alpha.119. Veja `ALPHA119_BACKGROUND_AUDIO.md`.
