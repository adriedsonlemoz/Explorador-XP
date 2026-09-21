# Explorador XP — alpha.61 — Editor de texto/código

## Objetivo

Evoluir o editor interno sem substituir o componente nativo leve nem remover recursos existentes. A implementação mantém salvar/salvar como, desfazer/refazer, linha/coluna, aviso de alterações não salvas, preview web, codificações já reconhecidas e proteção de arquivos grandes.

## Alterações

- Destaque de sintaxe ampliado para os formatos de código/configuração reconhecidos pelo Explorador XP.
- Autoindentação no Enter (inclusive teclado virtual) e aumento de nível após blocos; Python/YAML recebem tratamento de `:`.
- Fechamento automático de `()`, `[]`, `{}`, aspas simples e duplas, com proteção para aspas escapadas e avanço sobre fechamento já existente.
- Preferências persistentes: TAB ou espaços; 2, 4 ou 8 espaços por nível; quebra automática de linha.
- Índice incremental de início de linhas para gutter, Ir para linha e linha/coluna, evitando recontar o documento inteiro em cada edição.
- Localizar/substituir com quantidade total, ocorrência atual, anterior/próxima e retorno circular.
- Estado de documento salvo separado da profundidade do histórico de desfazer/refazer.
- Preview mantém a instância do editor composta, evitando perda de histórico/estado ao alternar visualização.
- Realce é desativado/reduzido acima do limite definido para manter responsividade; o limite existente de edição para arquivos grandes continua preservado.

## Abrir com

Arquivos recebidos via `ACTION_VIEW` continuam copiados para `cache/external-open` e abertos em somente leitura nesta versão. A alpha.61 adiciona `ExternalOpenOrigin`, que conserva URI original, MIME, nome de exibição e flags de leitura/escrita/persistência separadamente da cópia temporária. Nenhuma gravação no URI original foi habilitada; essa estrutura serve de base para uma próxima etapa de “Salvar de volta”.

## Desempenho

O editor não envia mais uma cópia completa do conteúdo para o estado Compose a cada tecla. O gutter usa índice incremental de linhas; os padrões de sintaxe são reutilizados em cache e o realce permanece debounced e limitado por tamanho. O histórico de desfazer/refazer ganhou orçamento aproximado de 4 milhões de caracteres, preservando a operação mais recente e descartando primeiro entradas antigas quando necessário. A busca só recalcula contagens durante edição quando o painel Localizar está aberto.

## Validação da entrega

- `applicationId` preservado: `com.exploradorxp.app`.
- Versão: `0.1.0-alpha.61` / `versionCode 61`.
- Nenhuma imagem ou mockup criada/modificada.
- O pacote-fonte enviado não inclui Gradle Wrapper nem Android SDK local; por isso a validação desta entrega combina verificações estruturais, parser Kotlin disponível no ambiente, testes isolados dos algoritmos/regex e consistência de recursos/metadados. A compilação Android completa continua a cargo do workflow Gradle do projeto.

## Correção de build incorporada

- Corrigida a chamada de `PackageManager.getPermissionInfo()` no inspetor de APK: a API usa o overload de flags `Int`; a referência inexistente a `PackageManager.PermissionInfoFlags` foi removida.
