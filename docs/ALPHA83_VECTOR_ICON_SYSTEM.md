# Alpha 83 — sistema vetorial de ícones

## Objetivo

Substituir a biblioteca heterogênea de bitmaps usada como iconografia por um conjunto visual único, sem criar imagens novas e sem alterar a lógica funcional do gerenciador de arquivos.

## Implementação

- Todos os PNGs usados como ícones de interface foram removidos de `app/src/main/res`.
- A interface usa `VectorDrawable` XML e Material Icons já vetoriais onde eles já existiam.
- O `FileIconMapper` mapeia extensões para categorias, evitando dezenas de assets quase equivalentes.
- `drawable_aliases.xml` preserva IDs antigos usados por componentes existentes, reduzindo risco de regressão durante a migração.
- `CachedResourceIcon` continua disponível para não quebrar chamadas antigas, mas deixou de decodificar/cachear `Bitmap`.
- O Adaptive Icon do aplicativo usa apenas vetores no foreground e no monochrome.

## O que não foi removido

Miniaturas e imagens que representam conteúdo real do usuário não são ícones decorativos e continuam funcionando. A imagem opcional do modelo do aparelho, quando obtida com correspondência confiável, também permanece. Nenhuma dessas imagens é incorporada ao APK como parte do pacote de ícones.

## Categorias visuais

Texto, documento, planilha, apresentação, PDF, imagem, áudio, vídeo, arquivo compactado, código, banco de dados, aplicativo/pacote, imagem de disco, fonte, ebook e desconhecido. Pastas especiais seguem a mesma família visual de pasta.
