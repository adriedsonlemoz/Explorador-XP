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
- Próximo: validar a alpha.25 no aparelho e depois executar Macrobenchmark para comparar `FrameTimingMetric`/jank após a otimização de ícones.
- Depois da medição dos ícones: atualizar Compose/AndroidX de forma controlada, comparando métricas antes/depois.

## Depois
- Suporte a SMB/rede local como módulo independente.
- Temas XP adicionais sem descaracterizar o layout aprovado.

### Alpha 24 — Informações do dispositivo

- [x] Redesenhar o painel em Compose usando os dados reais já coletados.
- [x] Melhorar hierarquia visual sem inserir foto/imagem falsa do modelo.
- [x] Integrar atualização ao cabeçalho e manter exportação para IA.
- [x] Exibir recursos com estado legível Disponível/Não disponível.

### Alpha 25 — Sensores e compartilhamento

- [x] Respeitar barra de navegação no rodapé e impedir corte das ações finais.
- [x] Detectar sensores adicionais usando `SensorManager`.
- [x] Separar Recursos de Sensores na interface.
- [x] Adicionar resumo copiável.
- [x] Gerar PNG local com ficha do dispositivo.
- [x] Compartilhar PNG via `FileProvider`.
- [x] Expandir relatório técnico para IA com seção `[sensors]`.

### Alpha 26 — Conectividade e CPU

- [x] Adicionar painel de conectividade real (Wi‑Fi/rede móvel/SIM/eSIM/Bluetooth/VPN).
- [x] Exibir banda e padrão do Wi‑Fi quando o Android disponibilizar.
- [x] Ampliar informações de CPU com ABI, clock máximo por grupo e hardware.
- [x] Transformar setas de seção em controles reais de expandir/recolher.
- [x] Compactar sensores em três colunas com nomes de até duas linhas.
- [x] Expandir relatório para IA sem coletar identificadores pessoais.
