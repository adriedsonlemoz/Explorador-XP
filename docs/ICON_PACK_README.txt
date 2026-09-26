EXPLORADOR XP — SISTEMA DE ÍCONES VETORIAIS

A partir da v0.1.0-alpha.83 o aplicativo não usa mais o antigo pacote de PNGs para ícones da interface.

Padrão atual:
- VectorDrawable XML para navegação, ações, estados, arquivos, pastas e armazenamento.
- Um conjunto pequeno e consistente de ícones por categoria de arquivo, em vez de um bitmap diferente para cada extensão.
- Adaptive Icon do launcher com foreground/monochrome vetoriais.
- O mesmo vetor escala para qualquer densidade sem perda de nitidez.
- Não há cache de Bitmap para ícones; o Compose renderiza os vetores diretamente.

Categorias de arquivo:
- texto; documentos; planilhas; apresentações; PDF; imagens; áudio; vídeo; compactados; código; bancos; apps/pacotes; imagens de disco; fontes; ebooks; desconhecidos.

Exceção intencional:
- conteúdo real aberto pelo usuário (fotos/miniaturas) e a imagem opcional do modelo do aparelho continuam sendo conteúdo, não ícones decorativos. Essas imagens não fazem parte do pacote de ícones do APK.
