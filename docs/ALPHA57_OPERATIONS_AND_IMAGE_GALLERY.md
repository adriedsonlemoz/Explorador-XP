# Explorador XP — alpha.57

## Operações de arquivos

A alpha.57 continua a evolução iniciada nas alpha.54–56 sem reintroduzir varreduras duplicadas. `FileOperationPlan` continua enumerando a árvore uma vez, mas a fase de cópia agora informa incrementos de bytes enquanto grava cada bloco. O estado de transferência calcula velocidade média e uma estimativa de tempo restante sem executar uma segunda leitura do arquivo.

Conflitos de nome ao colar deixaram de ser resolvidos silenciosamente apenas com renome automático. A interface oferece **Substituir**, **Ignorar** e **Manter ambos**, com uma opção para repetir a mesma decisão nos conflitos seguintes da operação. Copiar para a própria pasta preserva a origem e força o comportamento seguro de manter ambos.

A pausa/continuação não foi incluída nesta etapa; cancelamento permanece disponível e seguro.

## Visualizador de imagens

Ao abrir uma imagem pela pasta atual, o `ExplorerViewModel` entrega ao visualizador uma sequência composta exclusivamente pelas imagens do snapshot daquela pasta. A sequência:

- não pesquisa o armazenamento inteiro;
- não entra em subpastas;
- ignora a busca textual ativa para que a galeria represente a pasta aberta;
- mantém a ordenação escolhida no Explorer;
- respeita a preferência de arquivos ocultos;
- não mistura Favoritos, conteúdo recebido por `content://`, previews de ZIP ou arquivos abertos fora da pasta atual.

O visualizador permite trocar de imagem pelos botões **Anterior/Próxima** ou por gesto horizontal e mostra a posição atual na pasta. Somente a imagem ativa é decodificada em bitmap, evitando carregar a pasta inteira em memória.

## Escopo visual

Nenhuma imagem, ícone ou mockup foi criado ou modificado nesta etapa.
