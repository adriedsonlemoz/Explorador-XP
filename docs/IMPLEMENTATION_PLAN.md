# Próximas etapas

## Próxima etapa técnica
- Integrar SD e USB OTG por Storage Access Framework com permissões persistentes.
- Tela de locais/dispositivos acessível pelo menu, sem retornar à barra lateral de desktop.
- Cálculo opcional de tamanho total de pastas em segundo plano.
- Melhorar estados de erro e permissões por volume.

## Etapas seguintes
- Compactar e extrair ZIP.
- Visualizador interno básico para imagens e texto.
- Ações em lote com progresso e cancelamento.
- Lixeira opcional do próprio app antes da exclusão permanente.

## Qualidade e desempenho em andamento
- Testes JVM iniciais adicionados na alpha.15 para projeção da listagem em memória; o workflow da alpha.17 mantém testes e lint obrigatórios antes do APK e imprime o relatório completo quando o lint falha.
- Próximo: executar o build da alpha.17 no GitHub. Se testes, lint e `assemblePerformance` passarem, validar o APK no aparelho e então adicionar testes instrumentados de navegação/operações de arquivo + Macrobenchmark/Baseline Profile para medir scroll, abertura de pasta e inicialização.

## Depois
- Suporte a SMB/rede local como módulo independente.
- Temas XP adicionais sem descaracterizar o layout aprovado.
