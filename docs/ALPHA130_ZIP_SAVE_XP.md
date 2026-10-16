# Explorador XP alpha.130: Salvar ZIP direto na pasta XP

- Faixa do visualizador ZIP: **Extrair / Verificar / Salvar**.
- Ao tocar em **Salvar**, o app copia o ZIP integral para **Armazenamento interno/XP/** (diretório `XP` na raiz do armazenamento compartilhado primário).
- Não abre seletor SAF, não extrai, não recomprime, não sobrescreve um ZIP existente. Caso o nome exista, cria `nome (1).zip`, etc. A pasta XP é criada se necessário.
- Se o arquivo de origem já estiver dentro da XP, informa que já está salvo.
- Sem a autorização de gerenciamento de arquivos do app, a interface informa a necessidade da permissão. No Android 10 ou anterior também respeita a permissão de gravação. Não cria arquivos em pastas privadas como fallback silencioso.
- A cópia ocorre em dispatcher de I/O, verifica quantidade de bytes e apaga a cópia parcial em caso de erro/cancelamento.
- Testes unitários: criação da pasta, cópia idêntica, nomes duplicados, arquivo já no destino, erro de cópia, fonte inexistente.
- Pendente: execução do Gradle/Android, APK e testes físicos de permissão/fluxo.
