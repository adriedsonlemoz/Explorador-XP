# Explorador XP — alpha.35 — editor interno de texto e código

## Escopo

Esta etapa altera somente o fluxo de arquivos baseados em texto. O player Media3, áudio, imagens, PDF, ZIP, APK, Lixeira, armazenamento e navegação principal permanecem com o comportamento anterior.

## Situação anterior

O visualizador antigo usava um `EditText` monoespaçado com alternância simples entre leitura e edição. Havia linha/coluna e limite de tamanho, porém o salvamento era feito diretamente por `File.writeText()`, o rótulo de codificação era fixo em UTF-8 e não existiam Salvar como, histórico próprio, busca/substituição, ir para linha ou proteção ao fechar com alterações pendentes. HTML usava uma tela separada com `WebView` e JavaScript desativado.

## Editor alpha.35

- Extensões textuais conhecidas são encaminhadas para `TextCodeEditorViewer`.
- A interface mantém visual XP, fonte monoespaçada e controles compactos.
- Barra principal: Salvar, Salvar como, Desfazer, Refazer, Localizar, Visualizar (web) e Mais.
- Menu Mais: selecionar tudo, copiar, recortar, colar, localizar/substituir, ir para linha e preview quando aplicável.
- Barra inferior informa linha/coluna, quantidade de linhas, alterações pendentes e codificação.
- Números de linha são desenhados no próprio `EditText`, sem dependência externa pesada.
- Tab insere quatro espaços; Enter tenta preservar a indentação inicial da linha atual quando o teclado envia a tecla correspondente.
- Realce básico é limitado a arquivos pequenos/médios e aos formatos HTML/XML, CSS, JS e JSON para evitar custo excessivo.

## Codificação e segurança de leitura

O editor detecta:

- UTF-8;
- UTF-8 com BOM;
- UTF-16 LE/BE quando há BOM;
- Windows-1252 como fallback somente quando a amostra não parece binária.

Arquivos com bytes nulos/controles em proporção incompatível com texto são recusados pelo editor e podem ser abertos externamente. Arquivos acima de 750.000 bytes entram em modo parcial/somente leitura; a prévia lê no máximo 400.000 bytes.

## Salvamento seguro

O texto é escrito em arquivo temporário na mesma pasta, seguido de `flush`, `FileDescriptor.sync()` e releitura completa para validar que o conteúdo gravado corresponde ao texto esperado. Depois:

1. tenta-se `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`;
2. se o sistema de arquivos não oferecer troca atômica, o original é movido para um backup temporário;
3. o temporário validado assume o nome final;
4. se essa troca falhar, o backup é restaurado.

O BOM/codificação detectados são preservados. Em codificações incapazes de representar um caractere novo, a validação impede substituir silenciosamente o original por conteúdo corrompido.

## Preview web

HTML/HTM usa `loadDataWithBaseURL()` apontando para a pasta real do arquivo. Assim referências relativas como `style.css`, `script.js`, imagens e outros recursos locais existentes na mesma pasta continuam resolvendo normalmente. JavaScript fica habilitado apenas dentro desse preview web solicitado.

CSS e JavaScript também podem usar um preview simples gerado pelo editor para inspeção rápida. O botão Atualizar usa o conteúdo atual; salvar também renova a fonte de preview. O preview pode ocupar toda a área do visualizador e possui atalhos para voltar ao código.

## Fechamento e atualização da pasta

O botão fechar e o botão Voltar passam pelo mesmo guardião do editor. Se houver alterações não salvas, o usuário escolhe salvar e sair, sair sem salvar ou cancelar. Ao fechar o visualizador, a listagem atual é atualizada para refletir o arquivo modificado e possíveis cópias criadas por Salvar como.

## Recursos visuais

Nenhuma imagem, mockup ou pacote de ícones foi criado ou substituído nesta etapa.
