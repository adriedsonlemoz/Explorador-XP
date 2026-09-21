# Desempenho — alpha.53

Esta etapa inicia a otimização da base antes das próximas evoluções do Explorador XP.

## Alterações aplicadas

- A leitura de diretórios grandes verifica cancelamento a cada lote de itens, reduzindo trabalho obsoleto quando o usuário navega rapidamente.
- Pastas já presentes no cache LRU aparecem imediatamente; a releitura de atualização é adiada brevemente e é cancelada se houver nova navegação.
- A barra inferior continua calculando arquivos, subpastas e tamanho recursivos, porém usa cache LRU de curta duração, um único scanner concorrente e atraso para não competir com a abertura da pasta.
- Busca e ordenação não alteram a chave da varredura recursiva, evitando recalcular toda a árvore apenas por mudar a projeção da lista.
- Miniaturas não iniciam decodificação durante a rolagem. Vídeos são decodificados um por vez e recebem atraso maior que imagens.
- O pré-aquecimento de ícones da listagem/grade foi reduzido para limitar trabalho logo após abrir uma pasta.

## Correções relacionadas

- Ícones PNG XP usam uma área segura interna preservando proporção e alinhamento, especialmente nas pastas Movies e Music.
- Instalação de APK usa `ACTION_INSTALL_PACKAGE`; `ACTION_VIEW` fica reservado ao fluxo de Abrir com. Há fallback explícito para instalador externo quando necessário.

## Próximos gargalos

1. Reduzir passagens recursivas duplicadas em copiar/mover/excluir/lixeira.
2. Separar estados de progresso pesado da árvore principal de UI para diminuir recomposições.
3. Medir diretórios grandes e rolagem com Macrobenchmark/Baseline Profile após esta etapa.
4. Evoluir o cache de metadados e a estratégia de carregamento em lotes para pastas com milhares de itens.
