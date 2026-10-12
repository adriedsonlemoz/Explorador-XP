# Alpha.108 — Galeria Fotos e Vídeos

## Fluxos
- Modo normal: barra com Grade/Lista, Selecionar, Ordenar, Filtrar e Mais.
- Modo seleção: Fechar, Tudo, Limpar, Inverter e Mais; abaixo Copiar e Enviar.
- **Tudo/Inverter** consultam todas as entradas legíveis do filtro vigente no MediaStore em `Dispatchers.IO` sem carregar miniaturas. A atualização leva tempo proporcional ao volume de arquivos. Trocar filtro/ordem limpa a seleção.
- Paginação em grupos de 96 itens após aplicar filtro; consultas executadas em I/O e canceláveis.
- Ordenação por DATE_MODIFIED, SIZE ou DISPLAY_NAME; filtros por segmentos dos diretórios Câmera, Capturas, Download e Outros. Pastas desconhecidas entram em Outros.
- Vídeos: `MediaStore.Video.VideoColumns.DURATION` (milissegundos); dimensão disponível por `MediaStore.MediaColumns.WIDTH/HEIGHT`.
- A cópia por SAF não sobrescreve destino. Envio ao Android tem limite por lote de 100 URIs; volumes maiores podem usar Copiar.

## Limites e validação
- `MediaColumns.DATA` e `File` são herdados: o SAF/content URI é evolução futura para mídias acessíveis somente por Uri.
- Android não indexa necessariamente todas as mídias removíveis/não concedidas; permissões parciais limitam o que aparece.
- Mover/Excluir em lote e agrupamento temporal continuam para versões futuras.
- Validação realizada: testes Kotlin JVM de filtros, análise sintática PSI e integridade do ZIP.
- Pendente: build Android Gradle, lint e testes em aparelho (fonte grande, várias páginas, seleção total, duração).
