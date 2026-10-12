# Alpha.109: Ferramentas, Lixeira e gesto Voltar

## Implementação

- `ExplorerQuickAccess.kt`: sexto destino `TOOLS`, 72 dp de altura e contagem/tamanho da Lixeira abaixo do atalho. Os valores só são mostrados após carregamento bem-sucedido (`loaded=true`).
- `ExplorerModels.kt` e `ExplorerViewModel.kt`: cálculo do total e estado `loaded`; falha de leitura preserva último snapshot e não produz zero indevido. Trabalho de I/O permanece em `Dispatchers.IO` no repositório.
- `ToolsPanelScreen.kt`: módulo de interface somente para funções existentes. Ao abrir uma ferramenta no painel, Fechar/Voltar retorna ao painel.
- `ExplorerScreen.kt`: `BackHandler` vinculado ao histórico; prioriza seleção, busca avançada e pesquisa. Quando não há histórico nem outro estado transitório, entrega Voltar ao Android.
- Sem mudança de permissões, alteração no servidor WebDAV, SMB, armazenamento da Lixeira ou exclusão automática.

## Verificações manuais recomendadas

1. Abrir pasta A/B/C, gesto Voltar duas vezes, observar C→B→A; testar avançar no botão do app e Voltar com três botões.
2. Selecionar arquivos e usar Voltar: limpar seleção primeiro; testar busca avançada e busca comum.
3. Abrir/excluir/restaurar arquivos na Lixeira e acompanhar contagem e bytes na barra inferior; teste especial com pasta excluída.
4. Abrir cada ferramenta pelo novo painel, usar gesto/Fechar e conferir retorno ao Painel.
5. Verificar seis atalhos com largura estreita, fontes maiores, rolagem do painel e teclado ativo.
6. Validar Galeria, SMB/WebDAV e tela Sobre após atualização.

## Limites

- O snapshot da Lixeira é atualizado pelo ViewModel após operações feitas pelo Explorador XP ou ao reabrir a Lixeira; exclusões por programas externos podem requerer novo carregamento.
- Somente arquivos mantidos pela Lixeira gerenciada pelo Explorador XP entram na contagem, não lixeiras de outros aplicativos.
- Itens não expiram; exclusão por prazo e novos testes/diagnósticos não foram implementados.
- Ainda requer `Gerar APK`, testes JUnit via Gradle, Android Lint e testes reais.
