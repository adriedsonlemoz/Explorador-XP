# Changelog

## 0.1.0-alpha.3

- Interface principal compactada para aproveitar melhor telas de celular.
- Botões Voltar, Subir e Exibir movidos para a barra superior, antes do título Explorador.
- Barra de localização redesenhada no estilo de endereço do Windows Explorer e reduzida em altura.
- Nova opção persistente para mostrar ou ocultar arquivos ocultos.
- Arquivos ocultos ficam escondidos por padrão.
- Data e horário de criação passam a aparecer nos itens em lista e grade, com fallback para a data de modificação quando o sistema de arquivos não fornece criação.
- A aba Recentes foi substituída por Downloads e abre diretamente a pasta Download do armazenamento.
- Cartão de armazenamento compactado: usado dentro da área verde, livre dentro da área livre e total fora do marcador, com o ícone alinhado à barra.
- Modo tela inteira imersivo com barras do sistema ocultas e reaparecimento temporário por gesto.
- Propriedades agora exibem data de criação e modificação.
- Versão sincronizada para `versionCode 3`.

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
