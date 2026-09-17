# Explorador XP — alpha.30 — Lixeira e armazenamento

## Objetivo

Integrar Lixeira, análise detalhada de armazenamento e refinamentos de identificação/organização sem criar fluxos paralelos e sem adicionar imagens ou mockups.

## Lixeira

- A exclusão existente passa a oferecer **Mover para a Lixeira** ou **Apagar permanentemente**.
- Cada volume utiliza uma pasta interna gerenciada `.ExploradorXP_Lixeira`, ocultada da navegação normal mesmo quando arquivos ocultos estão visíveis.
- Quando possível, o item é movido dentro do mesmo volume; cópia + exclusão fica como fallback.
- Metadados mínimos preservam caminho original, data, tamanho e tipo.
- Restauração recria o diretório de origem quando necessário e evita sobrescrita com sufixo `(restaurado)`.
- A tela da Lixeira permite restaurar, apagar definitivamente, atualizar e esvaziar.

## Armazenamento

- O card de armazenamento virou ponto de entrada para a análise detalhada.
- A varredura só começa quando solicitada e roda em `Dispatchers.IO`.
- O usuário vê total, usado, livre, distribuição por categorias, maiores pastas e arquivos grandes.
- A análise mostra quantidade de arquivos verificados e pode ser cancelada.
- Resultados reutilizam o próprio Explorer para abrir pastas e arquivos.
- A pasta gerenciada da Lixeira é ignorada na análise para não duplicar consumo aparente por categoria.

## Identificação e organização

- `FileTypeClassifier` centraliza os rótulos de tipo usados em lista, grade, status, Lixeira e visualizadores.
- A ação duplicada **Exibir** saiu da toolbar; **Lixeira** ocupa esse espaço e Lista/Grade continuam no menu Exibir.
- A barra de status mostra contexto de seleção, tipo e tamanho de forma mais útil.
- Ajuda foi dividida em tópicos expansíveis.
- Sobre reúne versão, desenvolvedor, PIX copiável e novidades da versão.

## Recursos visuais

Nenhuma imagem ou mockup novo foi criado/adicionado nesta atualização. Os recursos XP existentes foram preservados e reutilizados.
