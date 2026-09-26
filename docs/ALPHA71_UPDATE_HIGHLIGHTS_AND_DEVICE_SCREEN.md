# Explorador XP — alpha.71

## Escopo

A `0.1.0-alpha.71` introduz duas mudanças de navegação/experiência:

1. uma tela de novidades mostrada uma única vez após cada atualização do aplicativo;
2. a tela de informações do dispositivo deixa de ser modal e passa a ocupar toda a área útil do Explorador XP.

## Novidades pós-atualização

- `ReleaseNotes.kt` passa a ser a fonte única das notas da versão atual.
- `UpdateHighlightsScreen.kt` renderiza uma tela completa no estilo visual do Explorador XP, com versão, resumo das mudanças e botão **Continuar**.
- `PreferencesStore` persiste o último `versionCode` cuja tela de atualização já foi apresentada.
- A detecção usa `firstInstallTime` e `lastUpdateTime` do pacote para distinguir atualização de instalação inicial.
- A tela aparece apenas quando o `versionCode` atual é maior que o último já apresentado.
- O `versionCode` é marcado como apresentado assim que a tela entra em composição; por isso ela não volta na segunda abertura mesmo que o usuário feche o app antes de tocar em **Continuar**.
- Fluxos de `ACTION_VIEW` (**Abrir com**) não são interrompidos. A tela fica pendente para a próxima abertura normal.
- A entrada usada por testes de desempenho também não exibe a tela.
- **Sobre > Novidades desta versão** usa a mesma lista de `ReleaseNotes.current.changes`.

## Informações do dispositivo

- `DeviceInfoDialog` foi substituído por `DeviceInfoScreen`.
- A tela não usa mais `Dialog`/`XpModalWindow` nem fundo escurecido.
- O layout usa `fillMaxSize()` dentro da área segura já fornecida pela `MainActivity`.
- Cabeçalho, atualização dos dados, conteúdo rolável, exportação, compartilhamento e rodapé foram preservados.
- `BackHandler` fecha a tela e retorna ao Explorer em vez de encerrar a Activity.
- Os dois acessos existentes — **Ferramentas > Informações do dispositivo** e **Sobre > Informações técnicas** — continuam abrindo o mesmo conteúdo.

## Versão

- `versionName`: `0.1.0-alpha.71`
- `versionCode`: `71`
- `applicationId`: `com.exploradorxp.app`
