# Explorador XP — alpha.32 — correções e refinamento

Esta entrega implementa as 14 melhorias definidas após o vídeo da alpha.31, preservando integralmente o pacote visual existente e sem criar imagens ou mockups.

## Principais mudanças

1. artefatos internos/legados de Lixeira ocultos da navegação e da análise;
2. esvaziamento físico reforçado da Lixeira gerenciada;
3. estado específico para `Android/data` e `Android/obb` restritos;
4. classificação correta de arquivo sem extensão;
5. pluralização correta nos fluxos de seleção/operação;
6. seleção simplificada com menu **Mais**;
7. Lixeira mais compacta;
8. Favoritos sempre em lista compacta;
9. percentuais de armazenamento explicitamente baseados nos arquivos acessíveis analisados;
10. Lixeira separada dos rankings normais de armazenamento;
11. progresso compacto e consistente;
12. Propriedades com copiar nome/caminho;
13. grade e barra inferior refinadas;
14. card de armazenamento reduzido.

## Segurança da Lixeira

O Explorador XP remove fisicamente apenas a pasta de Lixeira que ele próprio gerencia (`.ExploradorXP_Lixeira`). Nomes internos expostos por Android/MediaStore, como `.trashed-*` e diretórios de reciclagem do sistema, são ocultados da interface e da análise, mas não são apagados automaticamente.

## Desempenho

A análise continua sob demanda e fora da thread principal. A filtragem de artefatos ocorre durante a leitura já existente, sem criar uma segunda varredura da navegação normal.
