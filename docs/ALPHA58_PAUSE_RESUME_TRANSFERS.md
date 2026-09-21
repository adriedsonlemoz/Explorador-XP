# Explorador XP — alpha.58

## Pausar e continuar operações

A alpha.58 adiciona pausa cooperativa às operações longas de arquivos sem descartar o trabalho já executado. A janela de progresso passa a alternar entre **Pausar** e **Continuar**, mantendo **Cancelar** sempre disponível.

O `ExplorerViewModel` mantém um gate de pausa separado do `ExplorerUiState`. O `FileRepository` recebe um checkpoint suspensível e o repassa ao `FileOperationPlan`. Durante cópias, o checkpoint é consultado entre blocos de 256 KiB; durante exclusões, antes de cada entrada; durante a enumeração inicial, em lotes. Assim, pausar não recria o plano nem reabre a operação desde o início.

As operações cobertas são copiar/colar, mover, exclusão permanente, mover para a Lixeira, restaurar, apagar da Lixeira e esvaziar a Lixeira. Movimentações instantâneas feitas pelo próprio sistema de arquivos com `renameTo()` podem terminar antes de haver tempo de pausar, o que é esperado para uma operação atômica curta.

## Métricas

O tempo acumulado em pausa é subtraído do cálculo de velocidade média e ETA. Dessa forma, uma pausa longa não reduz artificialmente a taxa exibida quando a operação continua.

## Cancelamento

Cancelar continua sendo uma operação distinta de pausar. Uma coroutine suspensa aguardando **Continuar** é cancelável, portanto o botão **Cancelar** encerra a tarefa mesmo quando ela está pausada.
