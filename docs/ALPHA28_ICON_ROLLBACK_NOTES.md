# Alpha 28 — rollback dos ícones

A alpha.27 foi um experimento para trocar o conjunto visual XP por vetores genéricos e etiquetas dinâmicas. Após teste real no aparelho, o usuário não percebeu ganho de desempenho e preferiu claramente o visual anterior.

A alpha.28 restaura exatamente o pipeline de ícones da alpha.26/alpha.23:

- 150 PNGs comuns em `drawable-xxxhdpi` com 192×192;
- variantes maiores somente para os poucos usos que precisam;
- `CachedResourceIcon` com decodificação fora da thread principal;
- cache LRU de 6 MiB;
- limite de duas decodificações simultâneas;
- pré-aquecimento dos tipos visíveis no início da pasta.

Todas as funções adicionadas até a alpha.26 foram preservadas.
