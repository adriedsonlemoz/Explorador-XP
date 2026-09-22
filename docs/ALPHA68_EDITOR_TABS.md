# Explorador XP — alpha.68

## Objetivo

Evoluir o editor interno sem substituir o componente leve existente, adicionando trabalho com vários arquivos e melhorando as assistências de indentação já presentes desde a alpha.61.

## Abas do editor

- Arquivos locais de texto/código da pasta atual podem ser abertos em até 10 abas.
- A aba ativa é destacada e alterações não salvas recebem `*`.
- Ao trocar de aba, texto não salvo, seleção/cursor e um histórico recente e contínuo de desfazer/refazer de arquivos comuns ficam preservados em memória, com limites para evitar crescimento excessivo de RAM.
- Arquivos grandes mantêm a estratégia paginada: se o trecho atual estiver alterado, é necessário salvá-lo antes de trocar de aba.
- Fechar uma aba alterada permite salvar, descartar ou cancelar.
- Sair do editor com várias abas alteradas oferece salvar todas.
- Abas sujas registram tamanho/data da versão em disco. Se essa versão mudar enquanto a aba estiver em segundo plano, o editor bloqueia sobrescrita direta e orienta **Salvar como**.
- HTML, HTM, TXT, código e nomes especiais como `README`, `Makefile` e `.gitignore` passam pelo mesmo ponto de composição do editor para a sessão não ser perdida ao alternar formatos.
- Arquivos recebidos por **Abrir com** e prévias temporárias do ZIP continuam fora do modo de múltiplas abas para preservar as regras de origem/permissão e somente leitura.

## Indentação e pares

- Botões **← Recuar** e **Indentar →** atuam na linha atual ou em todas as linhas selecionadas.
- TAB indenta e Shift+TAB recua quando há teclado físico.
- Autoindentação continua reconhecendo blocos `{`, `[`, `(`, `:` em Python e chaves YAML.
- Fechamento de `()`, `[]`, `{}`, aspas simples e duplas continua ativo por padrão.
- Autoindentação e fechamento de pares agora têm controles persistentes independentes nas configurações.
- `EditorIndentationEngine` concentra a transformação pura de blocos e pode ser testado sem Android/Compose.

## Compatibilidade preservada

- `applicationId`: `com.exploradorxp.app`
- editor de arquivos grandes, realce de sintaxe, localizar/substituir, undo/redo, quebra de linha e preview web
- edição externa segura introduzida nas versões anteriores
- busca avançada e fila de operações da alpha.67
- detecção por conteúdo, compactados, dispositivo, APK, galeria, Lixeira e demais funções existentes
- nenhuma imagem ou mockup criada/modificada nesta etapa

## Versão

- `versionName`: `0.1.0-alpha.68`
- `versionCode`: `68`
