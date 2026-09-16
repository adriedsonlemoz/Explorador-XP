# Alpha 21 — Informações do dispositivo e CI mais rápido

## Informações do dispositivo

A nova opção **Ferramentas → Informações do dispositivo** lê os dados diretamente das APIs do Android no aparelho atual. A interface evita jargão excessivo e mostra apenas o que costuma ser útil: nome/modelo, fabricante, Android/API, atualização de segurança, processador, CPU, RAM, armazenamento, tela, bateria e recursos disponíveis.

A coleta é feita fora da thread principal. Dados variáveis (RAM disponível, armazenamento livre e bateria) podem ser atualizados pelo botão **Atualizar**.

## Exportação para IA

O botão **Exportar relatório para IA** abre o seletor de arquivos do Android e salva um TXT estruturado. O arquivo contém uma camada humana e campos técnicos/brutos úteis para diagnóstico: ABI, kernel/build, valores em bytes, densidade/taxa da tela, bateria e flags de recursos.

Por privacidade, o relatório não coleta IMEI, serial, Android ID, MAC, localização, contas, nomes de arquivos ou conteúdo de arquivos. O próprio relatório registra essas exclusões na seção `[privacy]` para evitar interpretações erradas por ferramentas de IA.

## Build e Baseline Profile

O build 20 confirmou testes, lint, geração do Baseline Profile e `assemblePerformance`. A geração do perfil sozinha consumiu 8m38s. Na alpha.21, o workflow **Gerar APK** reutiliza o perfil embarcado e apenas verifica que ele existe; o workflow **Desempenho e Baseline Profile** continua sendo o local para regenerar o perfil e executar Macrobenchmark.

Resultado esperado: o APK comum mantém R8, `shrinkResources` e Baseline Profile, mas deixa de pagar o custo do emulador em toda compilação.
