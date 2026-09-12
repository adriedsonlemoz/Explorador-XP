# Changelog

## 0.1.0-alpha.2

- Corrigida a compilação no GitHub Actions: adicionado o import de `androidx.activity.compose.setContent` em `MainActivity.kt`.
- Removido código não utilizado da Activity.
- Workflow atualizado para obter o `versionName` diretamente de `app/build.gradle.kts` ao nomear e publicar o APK.
- Versão sincronizada para `versionCode 2`.

## 0.1.0-alpha.1

- Início do projeto Android nativo em Kotlin + Jetpack Compose.
- Implementação da tela principal baseada no mockup móvel aprovado.
- Integração dos 147 ícones XP gerados para o projeto.
- Navegação real por diretórios do armazenamento compartilhado.
- Lista/grade, busca, ordenação e breadcrumb.
- Histórico Voltar/Avançar e navegação Subir.
- Seleção múltipla e ações copiar, mover, colar, excluir e compartilhar.
- Criar pasta e renomear.
- Favoritos e arquivos recentes persistentes.
- Abertura de arquivos via FileProvider.
- Tela de propriedades básica.
- Fluxo de permissão para acesso amplo ao armazenamento.
- Ícone adaptativo inicial do aplicativo.
