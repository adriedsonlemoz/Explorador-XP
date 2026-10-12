# Backlog de interface: progresso da alpha.102

Revisado em 2026-10-11, a partir da análise do código da alpha.100 e do pedido do usuário. Parte implementada no código-fonte da alpha.102; validar em dispositivo antes de encerrar tarefas restantes.

## Ferramentas > Aplicativos Instalados

- [x] Compactar o cabeçalho dos detalhes: ícone real à esquerda, nome e pacote à direita.
- [x] Subir os indicadores "Sistema/Usuário" e "Habilitado/Desativado" próximos ao nome. O indicador atual `enabled` **não** prova execução em segundo plano; não usar o termo "Ativo".
- [x] Colocar uma linha de quatro ações compactas junto ao cabeçalho: **Abrir**, **Limpar**, **Remover**, **Sistema**; ícones menores e rótulos curtos para caber em telas estreitas.
- [x] Manter a ação copiar pacote disponível por toque no identificador ou no painel Informações, sem criar um quinto botão.
- [x] **Limpar** abre a tela oficial do Android para gerenciar dados; o aplicativo não pode apagar dados privados de terceiros diretamente. **Remover** usa a confirmação de desinstalação do sistema. Respeitar apps do sistema e ações não disponíveis.
- [ ] Testes de layout em telas pequenas, fontes grandes, acessibilidade e restrições de apps de sistema.

## Atalho Fotos > Galeria

- [x] Substituir ícones genéricos por miniaturas reais usando cache e decodificação reduzida já existentes em `FileThumbnail.kt`; investigar `ContentResolver.loadThumbnail` para URIs content em atualização futura.
- [x] Exibir tamanho legível (KB/MB) e nome da pasta de origem em lista.
- [x] Exibir resolução quando disponível na galeria (alpha.108).
- [ ] Agrupar por Hoje, Ontem e mês.
- [x] Permitir alternar **Grade** e **Lista**, com carregamento paginado e miniaturas somente dos itens visíveis, sem limite arbitrário de 600 por abertura.
- [x] Seleção múltipla com ações Copiar (SAF) e Enviar/Compartilhar (Android).
- [ ] Implementar Mover e Excluir em lote com confirmação, Lixeira e segurança de permissões do Android.
- [x] Preservar o visualizador e o zoom existentes e evitar bloquear a interface durante a leitura de imagens.
- [x] Reaproveitar o mesmo padrão de miniaturas e tamanho no atalho Vídeos.
- [x] Exibir duração dos vídeos no MediaStore (alpha.108).
- [ ] Testes de rolagem com milhares de itens, consumo de RAM, permissões parcialmente concedidas e pastas inacessíveis.

## Regra de entrega

Não antecipar essas melhorias na alpha.101; ela é exclusiva da correção do build 94.

## Rede > QR Code (alpha.102)

- [x] Exibir QR com endereço, usuário, senha temporária e pin SHA-256 quando servidor HTTPS estiver ativo.
- [x] Leitura por câmera no cliente; validar conteúdo e preencher campos sem conectar automaticamente.
- [ ] Validar leitura e conexão real entre dois aparelhos e permissões da câmera.
- [ ] Testar rolagem/miniaturas de milhares de imagens em aparelhos com pouca RAM.
- [ ] Completar ações em lote adicionais (mover e excluir) respeitando o Android.
