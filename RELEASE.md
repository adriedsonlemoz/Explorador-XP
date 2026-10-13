# Explorador XP 0.1.0-alpha.116

**Versão:** `0.1.0-alpha.116`  
**Version code:** `116`

## Melhorias de interface

- ZIP: removida a barra genérica redundante de "Abrir com"; cabeçalho agora mostra apenas conteúdo/tamanho com ações Extrair/Verificar/Info na mesma linha. Busca e seleção tornadas opcionais, navegação por caminho preservada. Info do ZIP compacta com seção de detalhes técnicos expansível e caminho copiável.
- Lixeira: lista compacta com restauração de um toque, menu para exclusão definitiva com confirmação, detalhes sob demanda, filtros Arquivos/Pastas e agrupamento por data. Seleção múltipla com restauração e exclusão em lote; operações enviadas de uma vez ao repositório para evitar disputas de transferências.
- Dispositivo: toques em valores técnicos públicos copiam para a área de transferência com confirmação. Mantidos dados de rede/SIM sem cópia automática. Catálogo offline reconhece o modelo 21091116UG como Redmi Note 11 Pro+ 5G e alias exato MT6877V/TZA como MediaTek Dimensity 920. Dados de catálogo continuam diferenciados das leituras Android.
- Notas de atualização e testes de escopo sincronizados com a alpha.116.

## Validação

- Validação local de integridade de arquivos, recursos e testes unitários isolados quando disponíveis.
- **Pendente:** build Android completo, testes instrumentados e teste físico das interfaces ZIP, Lixeira e Dispositivo.
