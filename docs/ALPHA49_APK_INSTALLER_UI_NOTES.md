# Explorador XP — alpha.49

Versão: `0.1.0-alpha.49` (`versionCode 49`)

## Visualizador APK

- cabeçalho em card com ícone, nome, arquivo e estado;
- comparação entre versão do APK e versão instalada;
- detalhes reorganizados com divisores;
- ações principais lado a lado;
- botões com ícones e distinção visual entre ação primária e secundária.

## Permissão para instalar APKs

O Android não fornece um callback no instante em que o usuário ativa o switch **Permitir desta fonte**, nem permite ao app fechar à força essa tela do sistema. A alpha.49 mantém a instalação pendente e, assim que o usuário usa **Voltar**, detecta a permissão e abre automaticamente o instalador sem exigir outro toque.
