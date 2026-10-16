# alpha.132: salvar ZIP em Downloads/XP e encerrar somente após sucesso

- O botão **Salvar** da tela ZIP é o único evento que aciona o callback `onSavedZip`.
- Caminho de destino: `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)/XP`. Em Android típico, corresponde a `/storage/emulated/0/Download/XP` (nome exibido ao usuário: Downloads).
- Usa permissão de gerenciar arquivos existente no projeto; não solicita a pasta, não recomprime, preserva nome e gera sufixos para duplicados.
- Mensagem breve de sucesso em Toast antes de executar o callback e encerrar a `MainActivity` via `finish()`, retornando ao app/launcher anterior.
- Nada de `exitProcess`, `killProcess`, `finishAffinity` ou fechamento de outros apps. Se faltar permissão, ocorrer falha, não houver espaço ou a cópia for cancelada, o app fica aberto.
- Se ZIP já estiver em Downloads/XP, considera salvamento concluído sem criar duplicata e encerra normalmente quando o usuário toca Salvar.
- Validações locais não equivalem a teste real com Android/Scoped Storage.
