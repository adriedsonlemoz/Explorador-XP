# Explorador XP — alpha.69

## Visualizador de imagens

A alpha.69 transforma o visualizador existente em uma galeria mais completa sem mudar a regra de origem: somente imagens da pasta que estava aberta no Explorer entram na sequência.

### Gestos e visualização

- pinça com dois dedos para zoom de 100% a 600%;
- pan quando a imagem está ampliada;
- duplo toque alterna Ajustar / 250%;
- gesto horizontal anterior/próxima permanece disponível quando a imagem está ajustada;
- rotação visual em passos de 90°;
- tela cheia com controles que se ocultam automaticamente e reaparecem ao tocar;
- indicador de zoom e rotação.

### Ações

- Compartilhar usa o fluxo já existente do Explorador XP;
- Lixeira reutiliza a lixeira restaurável do app e exige confirmação;
- após mover a imagem atual, a galeria escolhe a próxima imagem disponível, depois a anterior;
- arquivos recebidos externamente e previews temporários somente leitura não podem ser enviados à Lixeira pelo visualizador.

### Informações

A nova janela Info lê em segundo plano dimensões e EXIF sem modificar o arquivo. Quando presentes, mostra fabricante/modelo da câmera, data da captura, orientação, ISO, tempo de exposição, abertura, distância focal e software.

### Compatibilidade

HEIC com extensão conhecida é encaminhado ao visualizador interno; a decodificação depende do codec disponível no Android/ROM. Em caso de incompatibilidade, o visualizador mantém a opção Abrir com outro aplicativo.

### Versão

- `versionName`: `0.1.0-alpha.69`
- `versionCode`: `69`
- `applicationId`: `com.exploradorxp.app`
