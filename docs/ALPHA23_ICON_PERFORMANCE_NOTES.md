# Alpha 23 — Otimização dos ícones

## Problema

O conjunto visual do Explorer possuía 150 PNGs comuns em 256×256 dentro de `drawable-nodpi`. Mesmo quando um ícone era exibido com 24–48 dp, a primeira decodificação podia trabalhar com o bitmap completo e ocorrer durante a composição/rolagem.

## Alterações

- Recursos comuns convertidos para 192×192 em `drawable-xxxhdpi`; o Android passa a dimensioná-los conforme a densidade real do aparelho.
- `CachedResourceIcon` decodifica os PNGs de `FileItem` em `Dispatchers.IO`.
- Cache LRU limitado a 6 MiB reaproveita ícones já vistos sem permitir crescimento ilimitado de memória.
- Lista e grade aquecem assincronamente os tipos presentes nos primeiros 32 itens.
- Variantes grandes preservam a qualidade de APK, busca e pasta aberta onde a arte é exibida acima de 48 dp.

## Efeito esperado

- Menos trabalho na thread principal durante scroll.
- Menor pico de memória por ícone, especialmente em aparelhos mdpi/xhdpi/xxhdpi.
- Menos decodificações repetidas ao voltar para pastas com os mesmos tipos de arquivo.

O ganho real deve ser confirmado pelo Macrobenchmark e pelo teste da build `performance` em aparelho físico.
