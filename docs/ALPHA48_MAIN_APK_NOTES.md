# Explorador XP — alpha.48

Versão: `0.1.0-alpha.48` (`versionCode 48`)

## Tela principal

- **Atualizar** foi removido da barra de ícones e movido para **Exibir**.
- O indicador de armazenamento na barra inferior ganhou mais largura e usa uma única linha.
- A barra inferior calcula em `Dispatchers.IO` a soma recursiva de arquivos, subpastas e tamanho da pasta atual; a tarefa é cancelada automaticamente ao navegar para outra pasta.
- A pasta interna da Lixeira do Explorador XP não entra nessa contagem rápida.

## APK

- O app declara `REQUEST_INSTALL_PACKAGES` para encaminhar APKs ao instalador do Android.
- `QUERY_ALL_PACKAGES` permite verificar corretamente se o packageName do APK já está instalado neste gerenciador de arquivos distribuído fora da Play Store.
- Caso a instalação por fonte desconhecida ainda não esteja liberada, o usuário é levado à configuração do Explorador XP e o fluxo continua ao retornar.
- O visualizador diferencia instalar, atualizar, reinstalar ou instalar uma versão anterior e pode abrir o app instalado quando houver launcher.
