# Explorador XP — alpha.63 — Editor de arquivos grandes

Versão: `0.1.0-alpha.63` (`versionCode 63`)

## Objetivo

Remover o antigo comportamento que truncava arquivos de texto/código acima de 750 KB e os forçava para somente leitura, sem trocar o editor leve existente nem voltar a carregar documentos de vários MB inteiros na memória.

## Modo arquivo grande

- O arquivo completo continua armazenado no disco.
- O editor decodifica uma janela-alvo de aproximadamente 384 KB por vez.
- Próximo/trecho anterior carregam novas janelas sob demanda.
- A janela tenta terminar/principiar em quebra de linha quando há uma próxima em até 32 KB; arquivos minificados sem quebras continuam limitados por bytes e por fronteira válida de caractere.
- UTF-8, Windows-1252 e UTF-16 LE/BE com BOM continuam reconhecidos pelo mesmo detector do editor.
- Linha e coluna são globais; o gutter começa na linha correspondente ao trecho carregado.

## Edição e gravação

- O trecho carregado continua usando o `CodeEditText`, portanto preserva desfazer/refazer, autoindentação, pares automáticos, TAB/espaços, localizar/substituir do trecho, quebra automática de linha e aviso de alterações não salvas.
- A gravação não monta o arquivo inteiro em uma `String`: prefixo e sufixo são copiados em streaming para um temporário e somente o trecho editado é recodificado.
- O temporário substitui o original com tentativa de `ATOMIC_MOVE` e fallback com backup, mantendo a estratégia segura usada no restante do editor.
- `Salvar como` para arquivo local cria uma cópia completa aplicando somente o patch da janela.
- Para `Abrir com`, a cópia de trabalho grande é atualizada por janela e o envio ao `content://` é feito por streaming. A recuperação do conteúdo externo usa arquivo temporário no cache em vez de `ByteArray` completo.

## Busca e navegação

- **Ir linha** encontra o deslocamento da linha por varredura de bytes em background e carrega o trecho correspondente.
- **Localizar anterior/próxima** percorre o documento completo por streaming com buffer fixo, conta resultados sem armazená-los todos e volta circularmente no início/fim.
- Busca e navegação exibem progresso e possuem cancelamento cooperativo.
- `Substituir todos` em modo arquivo grande atua no trecho atual, mantendo desfazer/refazer previsível e evitando uma operação global impossível de representar no histórico leve.

## Desempenho

- O realce de sintaxe deixa de depender do tamanho total do trecho: em modo grande, regex e spans são aplicados somente à área visível com margem de 40 linhas.
- O índice incremental de números de linha continua restrito à janela carregada, enquanto a base global é mantida separadamente.
- O orçamento do histórico cai de 4.000.000 para 1.200.000 caracteres no modo grande, sem eliminar a operação mais recente.
- Arquivos gigantes com uma única linha continuam com janela limitada; não há busca baseada em `readLine()` que possa materializar uma linha de vários MB.

## Validação adicionada

O motor `LargeTextFileEngine` foi exercitado isoladamente com arquivos UTF-8 de vários MB, UTF-16 LE com BOM, documento de linha única com mais de 12 MB, navegação por janelas, busca global, ir para linha e patch no início do arquivo preservando conteúdo no final.

Nenhuma imagem, recurso gráfico ou mockup foi criado/modificado nesta etapa.
