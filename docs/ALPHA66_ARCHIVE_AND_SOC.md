# Explorador XP — alpha.66

Versão: `0.1.0-alpha.66` (`versionCode 66`)

## Arquivos compactados

O navegador ZIP mantém a estrutura e as operações introduzidas nas versões anteriores, mas reorganiza a apresentação:

- cabeçalho com nome, tipo e aviso quando o ZIP foi reconhecido pelo conteúdo;
- métricas separadas para tamanho do arquivo, arquivos, pastas, bytes compactados, bytes descompactados e taxa de compressão;
- **Extrair** e **Abrir com** como ações principais;
- **Verificar** e **Info** como ações secundárias sempre acessíveis;
- ordenação acionada pela própria indicação de classificação;
- barra inferior sem repetir o resumo geral quando o usuário está na raiz;
- pesquisa, breadcrumb, seleção, menu de três pontos, preview, senha e extração preservados.

A janela **Informações do arquivo compactado** foi dividida em **Informações gerais**, **Conteúdo** e **Origem**. Também oferece copiar caminho, compartilhar, abrir pasta e iniciar extração.

## Identificação de SoC

Foi criado `DeviceSoCResolver.kt`, isolando a identificação comercial do processador da camada de interface. O catálogo usa aliases técnicos exatos e só retorna um nome comercial quando existe correspondência conhecida. Caso contrário, o app mantém o identificador informado pelo Android.

Exemplos cobertos nesta etapa:

- `MT6765` → MediaTek Helio P35;
- `SM6225` → Qualcomm Snapdragon 680;
- `SM6225-AD` → Qualcomm Snapdragon 685;
- `SDM660` → Qualcomm Snapdragon 660.

Quando cadastrados para o alias correspondente, GPU e processo de fabricação também são exibidos. CPU real, arquitetura, frequências e `Build.HARDWARE` continuam vindo da detecção já existente.

## Compatibilidade

- `applicationId`: `com.exploradorxp.app` — inalterado;
- detecção por conteúdo da alpha.65 preservada;
- editor, modo de arquivos grandes e otimizações de desempenho preservados;
- nenhum recurso de imagem ou mockup foi criado ou alterado nesta etapa.
