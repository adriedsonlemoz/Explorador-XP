# Explorador XP 0.1.0-alpha.81

## Ícone de armazenamento

A captura mostrou que o bitmap `drive_storage` estava íntegro, mas era desenhado diretamente em um slot pequeno de `31.dp`. Em algumas densidades a perspectiva do ícone ficava visualmente encostada no limite do slot, dando aparência de recorte mesmo sem `ContentScale.Crop`.

O cabeçalho de Armazenamento agora reserva uma área própria de `37.dp`, centraliza a imagem, limita o bitmap a `33.dp`, adiciona margem interna e declara `ContentScale.Fit`. Assim o recurso permanece totalmente dentro da área disponível e mantém folga visual ao redor.

## Cartões por tipo mais compactos

Os cartões de Vídeos, Imagens, Áudio, Aplicativos/APK e Outros foram reduzidos sem remover informação:

- espaçamento entre linhas/colunas: 8 dp → 6 dp;
- padding dos cartões: 9 dp em todos os lados → 8 dp horizontal / 7 dp vertical;
- ícones: 25 dp → 21 dp;
- espaço abaixo do cabeçalho: 6 dp → 4 dp;
- tamanho ocupado: 14 sp → 13 sp;
- contagem e porcentagem passaram a compartilhar a mesma linha secundária.

A regra de no máximo cinco cartões e o cálculo de porcentagem com base em `scannedBytes` foram preservados.

## Escopo

Esta versão é apenas um polimento visual da tela de Armazenamento. A enumeração de arquivos, os totais do Android, as contagens analisadas, o ranking de pastas, o cancelamento e a atualização segura continuam com a lógica da alpha.80.
