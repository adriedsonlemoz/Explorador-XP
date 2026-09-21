# Explorador XP — alpha.50

Versão: `0.1.0-alpha.50` (`versionCode 50`)

## Problema reproduzido

Ao abrir um arquivo `.md`, o visualizador carregava o conteúdo e a barra de status, mas o componente de edição caía no fallback com a mensagem `Attempt to invoke interface method ... on a null object reference`. O nome curto da interface é consequência da minificação do APK.

## Causa

`CodeEditText` sobrescreve `onSelectionChanged()`. O construtor de `TextView/EditText` pode disparar esse método antes que a inicialização dos campos da subclasse Kotlin tenha terminado. Nesse instante, o callback `callbackTextState`, embora declarado originalmente com uma lambda padrão, ainda pode estar `null` no nível da JVM. A chamada direta ao callback provocava a exceção durante `CodeEditText(context)`, fazendo `CodeEditorView` entrar no fallback.

## Correção

- `callbackTextState` e `callbackHistoryState` passam a ser anuláveis durante a fase de construção.
- Todas as invocações usam `?.invoke(...)`.
- `configure()` continua instalando os callbacks reais antes de carregar/configurar o documento.
- Nenhum recurso do editor foi removido.

## Escopo esperado

A correção vale para Markdown e para todos os formatos que usam o mesmo `CodeEditText`, incluindo TXT, LOG, HTML, CSS, JS, JSON, XML, YAML, CSV, INI, Kotlin e Java.
