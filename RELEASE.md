# Explorador XP 0.1.0-alpha.119

**Versão:** `0.1.0-alpha.119`  
**Version code:** `119`

## Novidades

- Mini-player na navegação com nome da faixa e controles anterior/pausar/retomar/próxima/encerrar.
- Reprodução de música continua ao sair do player ou bloquear a tela com Media3 `MediaSessionService` e sessão de mídia do Android.
- Controles do sistema na notificação e tela bloqueada; tocar a notificação retorna ao player.
- A fila da pasta e a posição são preservadas ao reabrir, sem criar um segundo ExoPlayer.
- Limpeza conservadora dos arquivos temporários compartilhados por outras aplicações.

## Testes pendentes

- Build completo do Gradle/Android, testes com MP3/FLAC/OPUS, fila, chamadas telefônicas, Bluetooth/fones, tela bloqueada, consumo de bateria e restrições em segundo plano do sistema.
- A notificação de mídia fica sob responsabilidade do Media3; visibilidade depende das configurações e versão do Android.
