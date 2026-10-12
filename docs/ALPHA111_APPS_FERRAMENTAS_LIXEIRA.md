# Alpha.111: reorganização visual

- Detalhes do aplicativo: Sistema/Usuário e Habilitado/Desativado sobem para a coluna à direita do nome; ações Abrir, Limpar, Remover e Sistema permanecem operacionais na linha abaixo do pacote.
- Lista de aplicativos: botões Voltar e Atualizar exibidos sem blocos brancos com toque de 44 dp. Espaço após Voltar evita sobreposição ao título.
- Painel de Ferramentas: grade adaptativa de quatro colunas nas larguras usuais (~393 dp), com mínimo por item de 76 dp e três ou duas colunas quando for estreito. Cartões brancos removidos.
- Lixeira na barra inferior: contador sobreposto ao ícone, remove texto lateral de tamanho; dados exatos ainda via TalkBack e tela Lixeira. O índice visual 99+ é deliberado para quantidades maiores.

## Testes pendentes em aparelho

- Conferir aplicativos com nomes longos e etiquetas «Sistema atualizado», estados Habilitado e Desativado.
- Voltar/Atualizar na lista e na ficha; teste com fonte ampliada.
- Ferramentas em 393 dp e larguras menores; abrir todos os oito destinos e voltar.
- Lixeira com 0, 1, 12 e mais de 99 itens, conferir texto sobre o vermelho e sem tamanho lateral.
- Regressão do funcionamento de ações, armazenamento, SMB/WebDAV e histórico do Android.

Build Android e testes de UI reais permanecem pendentes.
