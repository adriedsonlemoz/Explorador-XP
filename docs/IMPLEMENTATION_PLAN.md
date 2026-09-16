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
- O CI normal de APK reutiliza o Baseline Profile embarcado e não sobe mais um emulador em toda compilação; o workflow separado de desempenho continua responsável por regenerar o perfil e publicar JSON/traces.
- Alpha.19 corrige o último bloqueio conhecido do `lintDebug` sem suprimir regras, mantendo o I/O de Propriedades em `Dispatchers.IO`.
- Alpha.20 corrige a compilação do módulo `:baselineprofile` após o build 19 confirmar testes + lint verdes; `@LargeTest` agora tem runner/rules AndroidX Test declarados explicitamente.
- Build 20 confirmou geração real do Baseline Profile e `assemblePerformance`; como o perfil consumiu 8m38s, a alpha.21 separa definitivamente o build normal da regeneração pesada.
- Alpha.21 adiciona **Ferramentas → Informações do dispositivo**, com painel simples e relatório TXT técnico para IA, sem identificadores únicos ou coleta do conteúdo dos arquivos.
- Alpha.23 otimiza os 150 PNGs comuns para recursos `xxxhdpi`, move a decodificação dos ícones de `FileItem` para background e adiciona cache LRU de 6 MiB.
- Próximo: gerar a alpha.23, executar Macrobenchmark e comparar `FrameTimingMetric`/jank com a linha anterior, além de validar visualmente lista e grade no aparelho.
- Depois da medição dos ícones: atualizar Compose/AndroidX de forma controlada, comparando métricas antes/depois.

## Depois
- Suporte a SMB/rede local como módulo independente.
- Temas XP adicionais sem descaracterizar o layout aprovado.
