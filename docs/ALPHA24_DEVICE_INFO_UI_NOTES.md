# Alpha 24 — Informações do dispositivo

A tela foi redesenhada diretamente em Jetpack Compose a partir da referência visual aprovada.

## Decisões

- Não usar foto do aparelho: o projeto não possui uma fonte confiável de imagem por modelo e não deve inventar recursos visuais.
- Manter somente dados reais retornados pelo Android.
- Usar Material Icons vetoriais para reduzir custo e evitar novos PNGs.
- Integrar Atualizar ao cabeçalho para reduzir altura e deixar o painel mais coerente.
- Reforçar o relatório para IA como ação principal sem alterar os campos ou o contrato de privacidade.

## Estrutura

1. Cabeçalho
2. Resumo do dispositivo
3. Memória / armazenamento / bateria
4. Sistema
5. Bateria
6. Recursos
7. Versão do Explorador XP
8. Relatório para IA
