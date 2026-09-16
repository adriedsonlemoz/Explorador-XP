# Explorador XP — alpha.14

## Feedback de progresso em cópia, mover e exclusão

- Nova `TransferProgressDialog`, no mesmo estilo de janela usado em Sobre/Doação (barra de título azul com botão de fechar).
- Mostra: ícone da ação (copiar/recortar/excluir), nome do item sendo processado no momento, barra de progresso animada e o texto "X de Y item(ns) • Z%".
- Botão **Cancelar** interrompe a operação a qualquer momento; a listagem é atualizada imediatamente após o cancelamento.
- A janela aparece assim que a operação começa, mostrando "Preparando…" enquanto o total de itens ainda está sendo contado.

## FileRepository

- `paste()` e `delete()` agora recebem um callback `onProgress` e contam o total de itens (arquivos + pastas) antes de iniciar.
- O caminho rápido de mover no mesmo volume (`File.renameTo`) também reporta progresso, computando de uma vez o total de itens movidos.
- Verificação de cancelamento (`ensureActive()`) a cada item processado, permitindo interromper operações longas sem esperar o fim de uma pasta inteira.
- Progresso agrupado (`ProgressTicker`) a cada ~80 ms para não gerar uma atualização de estado por arquivo em transferências grandes.

## ViewModel

- `pasteClipboard()`, `deleteFile()` e `deleteSelected()` passam a usar uma função interna comum (`runTransfer`) que gerencia o job cancelável, o estado de progresso e as mensagens de sucesso/cancelamento/erro.
- Iniciar uma nova transferência cancela automaticamente qualquer uma anterior ainda em andamento.
