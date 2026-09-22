# Explorador XP — alpha.67

## Objetivo

Esta etapa evolui duas áreas de uso diário sem remover funções existentes: pesquisa de arquivos e acompanhamento de operações longas.

## Busca avançada

- Mantém a busca simples da pasta atual.
- Acrescenta modo avançado com pesquisa opcional em subpastas.
- Filtros: arquivos/pastas, categoria, extensão, tamanho mínimo/máximo e data de modificação.
- Ordenação por nome, data, tamanho ou tipo.
- Resultados parciais durante a varredura, contador de itens analisados/encontrados e cancelamento cooperativo.
- Resultados recursivos exibem a pasta relativa de origem.
- Respeita a opção de arquivos ocultos e ignora a Lixeira interna do Explorador XP.
- Usa controle de diretórios canônicos visitados para evitar ciclos durante varreduras recursivas.
- Reexecuta a busca após mudanças relevantes no armazenamento.

## Fila de operações

- Reutiliza o sistema existente de planos iterativos, pausa/continuação, cancelamento e conflitos.
- Adiciona fila visual para cada item de nível superior selecionado.
- Estados: Aguardando, Em andamento, Concluído e Ignorado.
- Mostra progresso do item atual separado do progresso total.
- Mantém velocidade e estimativa de tempo restante quando aplicáveis.
- Mantém Substituir / Ignorar / Manter ambos e “aplicar a todos”.

## Compatibilidade preservada

- `applicationId`: `com.exploradorxp.app`
- detecção por conteúdo da alpha.65
- navegador/Info de compactados e resolvedor de SoC da alpha.66
- editor de texto/código e modo de arquivos grandes
- Lixeira, instalador APK, visualizadores e demais operações existentes
- nenhum recurso de imagem ou mockup foi criado ou modificado nesta etapa

## Versão

- `versionName`: `0.1.0-alpha.67`
- `versionCode`: `67`
