# Alpha 52 — Padronização das janelas modais

## Problema observado em aparelho real

As telas **Lixeira**, **Ajuda**, **Sobre**, **Armazenamento** e **Informações do dispositivo** usavam estruturas de `Dialog` diferentes. Algumas preenchiam praticamente toda a área segura com `fillMaxSize()`/`fillMaxHeight()`, enquanto outras apenas limitavam a altura máxima. Visualmente isso fazia o conteúdo parecer um painel solto, sem um limite inferior claro, e a tela Sobre podia aparentar estar deslocada para baixo.

## Correção aplicada

Foi criado o componente reutilizável `XpModalWindow`, responsável por:

- centralizar a janela dentro de `safeDrawingPadding()`;
- manter margem externa superior/inferior e lateral;
- limitar a largura em telas grandes;
- usar altura proporcional à área segura (84–88% nas telas migradas);
- manter cabeçalho e rodapé fixos;
- reservar somente a área central para rolagem;
- desenhar divisores entre cabeçalho, conteúdo e rodapé;
- manter borda e cantos consistentes com a identidade visual do Explorador XP.

## Telas migradas

- Ajuda;
- Sobre o Explorador XP;
- Lixeira;
- Armazenamento;
- Informações do dispositivo.

A Lixeira continua com sua `LazyColumn`, e as demais telas mantêm suas áreas roláveis internas. Nenhuma ação foi removida.

## Ajustes específicos

- **Sobre:** atalhos de Ajuda e Informações técnicas foram movidos para o rodapé fixo junto de Fechar.
- **Lixeira:** resumo de itens/tamanho permanece no topo e também aparece no rodapé, que agora deixa o limite inferior explícito.
- **Armazenamento:** análise continua rolável e o rodapé mostra progresso ou percentual usado/livre.
- **Informações do dispositivo:** cabeçalho personalizado foi preservado; o corpo agora fica contido e o rodapé permanece visível durante a rolagem.
- **Ajuda:** tópicos continuam expansíveis dentro da área central, com rodapé fixo.

## Validação local

O projeto não inclui `gradlew` e o ambiente não possui a toolchain Android/Gradle completa. Foi executado smoke test com `kotlinc` nos arquivos Kotlin alterados; as referências Android/Compose ficam naturalmente sem resolução sem o classpath, mas não foram encontrados diagnósticos de parser como `expecting`, `unclosed` ou `unexpected tokens`.
