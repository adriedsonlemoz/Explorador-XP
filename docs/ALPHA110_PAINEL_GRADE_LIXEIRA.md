# Alpha.110: organização do Painel e Lixeira compacta

## Alterações

- `ToolsPanelScreen.kt`: grade adaptativa com oito atalhos quadrados para APPS, STORAGE, DEVICE, NETWORK, TRASH, SETTINGS, HELP e ABOUT, sem descrições ou preferências de ordenação.
- `ExplorerScreen.kt`: `AppSettingsScreen` recebe os estados/callbacks de ordenação e Pastas primeiro usados anteriormente no painel, inserindo seção Organização de arquivos nas configurações. Ajuda revisada.
- `ExplorerQuickAccess.kt`: indicador da Lixeira na linha do ícone, com contagem desenhada sobre ele e bytes abreviados ao lado. Todos os seis atalhos preservam a mesma altura e alinhamento. Acessibilidade comunica itens e bytes sem abreviação.
- `TrashBarSummary.kt`: formatação compacta isolada com testes Kotlin/JUnit em `TrashBarSummaryTest.kt`.
- `ReleaseNotes.kt`: somente quatro itens correspondentes à alpha.110; histórico integral no CHANGELOG.

## Plano de validação no Android

1. Tela Ferramentas: testar com fonte normal e ampliada, portrait/landscape, telas estreitas e gesto Voltar; abrir os oito atalhos e verificar retorno ao painel.
2. Configurações > Organização de arquivos: escolher Nome/Data/Tamanho/Tipo e Pastas primeiro, retornar às pastas, verificar ordenação e recarregamento.
3. Lixeira: testar vazia, 1 item, vários itens, mais de 99 itens, arquivos de 0 B, KB, MB e GB; mover, restaurar, apagar e esvaziar. Testar TalkBack e garantir que barra inferior continue alinhada.
4. Novidades da atualização: após instalar alpha.110 por cima da alpha.109, conferir apenas os quatro itens novos.
5. Regressão: gesto Voltar, Fotos/Vídeos, Download, acesso WebDAV/SMB, ajuda e atualização por assinatura.

**Não incluído:** prazo de exclusão da Lixeira, diagnóstico de downloads, velocidade, IP público ou VPN.
**Pendente:** build completo Android Gradle, lint e testes em aparelhos.
