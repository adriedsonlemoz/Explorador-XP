# Alpha.114 • Galerias por pasta, interface e correção CI

## Navegação
- Ao entrar em Fotos/Vídeos, o índice MediaStore agrupa por pasta física completa e não carrega thumbnails para gerar o índice. Não é possível acessar dados de apps fora dos acessos concedidos.
- Voltar do Android retorna da pasta ao índice; na raiz fecha a galeria. Voltar e Subir da barra retornam ao índice, mantendo a possibilidade de Avançar para a última pasta; Início volta ao índice.
- Menus Arquivo, Editar, Exibir, Favoritos, Ferramentas e Ajuda usam os mesmos componentes do Explorador; opções sem suporte (ex.: criar pasta na coleção MediaStore) não aparecem.
- Selecionar tudo e inverter percorrem metadados somente da pasta selecionada. Cópia SAF não sobrescreve nomes existentes; compartilhamento limita a 100 arquivos por intent.
- A visualização dentro da pasta conserva miniaturas e lista/grade. Paginação limitada a 96 por consulta, como na versão anterior.

## Interface
- Listagem de Aplicativos: ícones 32dp; informações preservadas nas três linhas reduzidas.
- Ferramentas: grade adaptativa com cartões de 112dp de altura, bordas leves, ícones 30dp e descrição.

## CI
- Gerar APK 107: Kotlin de app e testes compilou. A falha foi de um único teste (`ReleaseNotesScopeTest`) que esperava a lista da alpha.112 na alpha.113.
- Atualizadas as asserções para as seis mudanças da alpha.114; nenhuma tarefa de teste foi suprimida.
- Não foi possível rodar Gradle/Android completo localmente. Recomenda-se executar GitHub Actions e testar navegação no aparelho.
