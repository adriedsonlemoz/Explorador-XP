# Backlog de interface após a alpha.101 (não implementado)

Revisado em 2026-10-11, a partir da análise do código da alpha.100 e do pedido do usuário. Priorizar após confirmar build e testes básicos de rede/arquivos da alpha.101.

## Ferramentas > Aplicativos Instalados

- [ ] Compactar o cabeçalho dos detalhes: ícone real à esquerda, nome e pacote à direita.
- [ ] Subir os indicadores "Sistema/Usuário" e "Habilitado/Desativado" próximos ao nome. O indicador atual `enabled` **não** prova execução em segundo plano; não usar o termo "Ativo".
- [ ] Colocar uma linha de quatro ações compactas junto ao cabeçalho: **Abrir**, **Limpar**, **Remover**, **Sistema**; ícones menores e rótulos curtos para caber em telas estreitas.
- [ ] Manter a ação copiar pacote disponível por toque no identificador ou no painel Informações, sem criar um quinto botão.
- [ ] **Limpar** abre a tela oficial do Android para gerenciar dados; o aplicativo não pode apagar dados privados de terceiros diretamente. **Remover** usa a confirmação de desinstalação do sistema. Respeitar apps do sistema e ações não disponíveis.
- [ ] Testes de layout em telas pequenas, fontes grandes, acessibilidade e restrições de apps de sistema.

## Atalho Fotos > Galeria

- [ ] Substituir ícones genéricos por miniaturas reais usando `MediaStore`/`ContentResolver.loadThumbnail` e aproveitar cache existente em `FileThumbnail.kt` quando compatível.
- [ ] Exibir tamanho legível (KB/MB) e detalhes de resolução, data e pasta de origem.
- [ ] Permitir alternar **Grade** e **Lista**, com carregamento paginado e miniaturas somente dos itens visíveis, sem limite arbitrário de 600 por abertura.
- [ ] Agrupar por data e permitir seleção múltipla com operações copiar, mover, compartilhar e excluir, sempre com permissões Android.
- [ ] Preservar o visualizador e o zoom existentes e evitar bloquear a interface durante a leitura de imagens.
- [ ] Reaproveitar o mesmo padrão futuramente no atalho Vídeos, mostrando miniatura e duração.
- [ ] Testes de rolagem com milhares de itens, consumo de RAM, permissões parcialmente concedidas e pastas inacessíveis.

## Regra de entrega

Não antecipar essas melhorias na alpha.101; ela é exclusiva da correção do build 94.
