# Alpha.119: mini-player e serviço Media3

## Arquitetura

- `MusicPlaybackService` contém o único `ExoPlayer` e o `MediaSession` do aplicativo. Não usa serviço de monitoramento genérico nem recursos de rede.
- `rememberMusicController(enabled)` conecta/desconecta um `MediaController` durante o ciclo da UI. A sessão continua no foreground de mídia após a UI se desconectar enquanto reproduz.
- `AudioPlayerViewer` apenas controla a sessão e carrega a playlist local de até 1000 músicas; reabrir a mesma fila não reinicia o áudio.
- `MusicMiniPlayer` aparece na tela principal se houver uma música em sessão e permite avançar/voltar, pausar, abrir o player ou encerrar.
- Media3 fornece controle de mídia ao Android para a notificação, hardware de mídia e tela bloqueada. O toque na notificação abre a activity e reconecta ao player se ainda estiver ativo.
- Para arquivos externos copiados para o cache temporário, a sessão é parada antes de excluir o cache. Assim o serviço não mantém um caminho inválido.

## Permissões e ciclo de vida

- `FOREGROUND_SERVICE` já existia; a nova `FOREGROUND_SERVICE_MEDIA_PLAYBACK` é exigida para Android 14+.
- `media3-session` mesma versão 1.9.4 das outras dependências Media3.
- O MediaSessionService é declarado com `foregroundServiceType=mediaPlayback` e a intent action oficial.
- Reprodução segundo plano não significa reprodução permanente: usuário pode parar, o Android pode interromper a sessão, e um force-stop encerra o app.
- Nada é iniciado continuamente em background sem o usuário abrir uma música.

## Testes necessários no celular

1. Reproduzir MP3 e voltar: confirmar mini-player e continuidade.
2. Bloquear o aparelho: confirmar notificação, próxima/anterior, pausa e retomada.
3. Voltar pelo mini-player e notificação sem reiniciar a posição.
4. Trocar faixas, embaralhar, repetir, desconectar fones, atender chamada e verificar foco de áudio.
5. Abrir áudio de outro aplicativo, fechar e conferir que o arquivo temporário pode ser apagado.
6. Verificar modo economia de bateria e a liberação correta do serviço após encerrar.
