# Alpha 88 — Armazenamento e acesso aos aplicativos

## Armazenamento

A tela foi recriada para separar visualmente três níveis de informação:

1. **Capacidade do volume**: usado, livre, total e percentual vindos do Android.
2. **Ações**: Aplicativos instalados, Lixeira e análise/atualização.
3. **Análise de arquivos**: categorias, maiores pastas e arquivos grandes calculados somente sobre itens que puderam ser enumerados.

A interface nunca transforma a soma analisada em capacidade total do aparelho e não estima áreas protegidas.

## Aplicativos instalados

O módulo continua funcionando sem `PACKAGE_USAGE_STATS`: nesse caso mostra o tamanho dos APKs base/splits realmente encontrados. Quando **Acesso ao uso** é concedido, `StorageStatsManager` pode fornecer código, dados e cache.

Em versões do Android que restringem configurações especiais para apps instalados fora de uma loja confiável, o usuário pode precisar abrir **Informações do Explorador XP**, tocar nos 3 pontos e escolher **Permitir configurações restritas**. A presença e o nome dessa ação dependem do Android/fabricante; por isso a interface apresenta o passo como condicional.

A alpha.88 adiciona uma explicação pós-atualização depois de Novidades, exibida uma única vez quando aplicável. O usuário pode continuar sem liberar a autorização.
