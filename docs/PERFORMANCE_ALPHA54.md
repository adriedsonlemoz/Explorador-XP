# Desempenho — alpha.54

Esta etapa ataca o próximo gargalo identificado na alpha.53: travessias recursivas repetidas durante operações de arquivo.

## Gargalo anterior

Antes, operações como copiar, mover para a Lixeira e excluir normalmente percorriam uma pasta uma vez para contar entradas e calcular o total do progresso e depois percorriam a mesma árvore novamente para executar o trabalho. Em alguns fluxos de mover/restaurar com fallback de cópia, a mesma árvore podia ser enumerada ainda mais vezes.

## Implementação

- `FileOperationPlan` enumera a árvore iterativamente uma única vez.
- O plano guarda cada entrada, caminho relativo, tipo, tamanho e data de modificação.
- O mesmo plano alimenta a barra de progresso e a execução da cópia/exclusão.
- Exclusão usa a lista em ordem inversa para remover filhos antes dos pais sem nova chamada recursiva a `listFiles()`.
- Cópia usa a ordem planejada para criar diretórios antes dos arquivos e um buffer de 256 KiB.
- `paste`, exclusão permanente, mover para Lixeira, restaurar, apagar item da Lixeira e esvaziar Lixeira usam o novo caminho.
- `renameTo()` continua sendo o caminho rápido para movimentações no mesmo volume; se falhar, o fallback reutiliza o plano já calculado.

## Lixeira

Novas entradas armazenam `treeSize` e `entryCount` no `.trashinfo.json`. Isso evita recalcular o tamanho recursivo ao abrir novamente a Lixeira. Metadados de versões anteriores continuam aceitos e usam o cálculo legado somente quando necessário.

## Validação

Foi adicionado teste JVM para:

- contagem correta de arquivos e diretórios;
- soma dos bytes;
- cópia preservando a hierarquia;
- exclusão em ordem segura;
- pasta vazia representada por uma entrada de progresso.

Não foram criadas, geradas ou alteradas imagens/mockups nesta etapa.

## Próximo foco

O próximo passo recomendado é desacoplar o estado de progresso de operações pesadas do `ExplorerUiState` principal para reduzir recomposições amplas durante transferências e análises.
