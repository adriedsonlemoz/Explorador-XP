# Explorador XP 0.1.0-alpha.80

## Ajuda em tela completa

A Ajuda não usa mais `XpModalWindow`. Ela é renderizada como uma tela completa dentro do fluxo do Explorer, com cabeçalho XP, tópicos expansíveis preservados, conteúdo rolável, `BackHandler` e rodapé fixo contendo a quantidade de tópicos e o botão **Fechar**.

## Armazenamento em tela completa

A tela de Armazenamento também deixou de usar `Dialog`. O cabeçalho, o acionamento de análise, o cancelamento, a abertura de pastas/arquivos e a Lixeira foram preservados, mas agora todo o conteúdo ocupa a área útil do aplicativo e o rodapé fica fixo.

## Origem dos números

Os valores de capacidade geral (`totalBytes`, `freeBytes` e `usedBytes`) são obtidos via `StatFs` e representam o volume informado pelo Android. Eles não equivalem ao conjunto que o Explorador XP consegue percorrer.

A análise sob demanda percorre somente entradas enumeradas por `File.listFiles()` e conta apenas entradas reconhecidas como arquivos (`isFile`). `scannedFiles` representa a quantidade de arquivos efetivamente analisados e `scannedBytes` é a soma dos tamanhos desses arquivos. Diretórios não entram na contagem de arquivos.

As categorias e o ranking de pastas são alimentados durante essa mesma travessia. Por isso, percentuais por tipo usam `scannedBytes` como denominador e as contagens de pastas foram explicitamente rotuladas como arquivos analisados.

## Limitações de acesso

Quando `listFiles()` retorna `null` para um diretório, a análise incrementa `inaccessibleDirectories`. A interface só declara a varredura como parcial quando essa condição é realmente observada. Nenhum tamanho ou número de arquivo inacessível é estimado.

## Categorias compactas

A interface mostra no máximo cinco cartões: até quatro categorias específicas de maior tamanho e, quando necessário, um cartão **Outros** que agrega todas as categorias restantes, inclusive a categoria interna `other`, evitando duplicação visual.

## Atualização segura

Ao executar uma nova análise, o último resultado válido é mantido na tela e marcado como resultado anterior. Ele só é substituído após uma nova análise concluída com sucesso. Cancelamento ou falha não transforma dados antigos em resultados novos.
