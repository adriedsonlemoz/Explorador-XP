# Alpha.92 — Informações do dispositivo

## Causa identificada no código

Depois que a leitura do Android concluía, o Compose montava o conteúdo de `DeviceInfoScreen`. O componente `DeviceOriginLabel`, usado duas vezes nessa tela, aplicava `Modifier.padding(bottom = (-3).dp)`. Padding negativo viola as restrições de layout do Compose e gera exceção durante a composição/medição. O `runCatching` adicionado na alpha.89 envolve a coleta assíncrona e as buscas; ele não intercepta erros gerados ao desenhar componentes. Por isso a tela ainda podia fechar o aplicativo.

## Correção

- Removido o padding inferior negativo do rótulo, mantendo os espaçamentos positivos e o `Arrangement.spacedBy` do conteúdo.
- Preservada a busca opcional de identidade/imagem externa da alpha.82, com cache, fonte e fallback offline; ela não é necessária para reproduzir a falha de layout.
- `ReleaseNotes.current` atualizado: alimenta tanto as novidades exibidas uma vez após a atualização quanto **Sobre > Novidades desta versão**.

## Validação

- Conferir que `DeviceInfoScreen.kt` não contém mais padding negativo; as duas chamadas de `DeviceOriginLabel` usam a mesma implementação corrigida.
- Abrir **Ferramentas > Informações do dispositivo**, voltar, abrir por **Sobre > Informações técnicas** e repetir com busca externa ligada/desligada em um APK instalado no aparelho.
- Confirmar as novidades da alpha.92 na primeira abertura após atualizar da alpha.91.
- O ambiente de edição não possui Gradle/Android SDK; build e teste no aparelho dependem do workflow **Gerar APK**.

Versão: `0.1.0-alpha.92` / `versionCode 92`.
