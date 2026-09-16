# Próximas etapas

## Próxima etapa técnica
- Integrar SD e USB OTG por Storage Access Framework com permissões persistentes.
- Tela de locais/dispositivos acessível pelo menu, sem retornar à barra lateral de desktop.
- Cálculo opcional de tamanho total de pastas em segundo plano.
- Melhorar estados de erro e permissões por volume.

## Etapas seguintes
- Compactar e extrair ZIP.
- Visualizador interno básico para imagens e texto.
- Ações em lote com progresso e cancelamento.
- Lixeira opcional do próprio app antes da exclusão permanente.

## Qualidade e desempenho em andamento
- Alpha.18 adiciona `:baselineprofile`, ProfileInstaller, perfil inicial embarcado e geração automática por jornada real.
- Macrobenchmark mede inicialização e rolagem com `StartupTimingMetric` e `FrameTimingMetric`, comparando sem compilação antecipada x Baseline Profile.
- O CI de APK passa a gerar o perfil antes do `assemblePerformance`; workflow separado publica JSON/traces para consulta.
- Alpha.19 corrige o último bloqueio conhecido do `lintDebug` sem suprimir regras, mantendo o I/O de Propriedades em `Dispatchers.IO`.
- Próximo: validar a alpha.19 no GitHub até `generateBaselineProfile` + `assemblePerformance`; depois executar os Macrobenchmarks em aparelho físico para estabelecer a linha de base real de TTID/TTFD e jank.
- Após a linha de base: otimizar decodificação/uso dos PNGs e atualizar Compose/AndroidX de forma controlada, comparando métricas antes/depois.

## Depois
- Suporte a SMB/rede local como módulo independente.
- Temas XP adicionais sem descaracterizar o layout aprovado.
