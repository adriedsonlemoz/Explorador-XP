# Explorador XP — alpha.40

Versão: `0.1.0-alpha.40` (`versionCode 40`)

## Problema observado

No aparelho real, a Lixeira ficou praticamente inutilizável: os nomes dos arquivos não apareciam, os botões ocupavam largura excessiva, as linhas ficavam com grandes áreas vazias e o diálogo **Esvaziar Lixeira?** mostrava apenas **Cancelar**.

## Causa principal

`XpDialogButton` possuía um `Row` interno com `fillMaxWidth()`. Quando o botão era usado dentro de outra `Row` sem largura explícita, ele podia consumir a largura disponível durante a medição do Compose. Na Lixeira isso comprimía a coluna de nome/metadados e também empurrava ações irmãs para fora da área visível.

## Alterações

- Removido o `fillMaxWidth()` interno de `XpDialogButton`; a largura passa a ser controlada pelo modificador recebido pelo botão.
- Reconstruída a lista da Lixeira em cards compactos e legíveis.
- Nome do arquivo em destaque, com até duas linhas.
- Tipo/tamanho, data de exclusão e origem separados para evitar truncamento de toda a informação em uma única linha.
- **Restaurar** e **Apagar** visíveis lado a lado em cada item, sem menu escondido.
- **Atualizar** e **Esvaziar Lixeira** lado a lado no cabeçalho em celulares comuns.
- Confirmação de esvaziamento informa quantidade de itens e tamanho total.
- Metadado da Lixeira passa a gravar `originalName`; entradas antigas continuam compatíveis por fallback para caminho original e nome físico.
- Adicionado `TrashItemTest` para validar a estratégia de nome/fallback.

## Validação local

O ambiente desta sessão não possui Gradle nem Android SDK, e o projeto não contém `gradlew`; portanto não foi possível executar um build Android completo localmente. Foram feitas validações estruturais dos arquivos alterados, referências e JSON, mantendo os workflows do GitHub inalterados para o build real.

Nenhuma imagem/mockup ou recurso gráfico novo foi criado.
