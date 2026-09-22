# Explorador XP — alpha.65

Versão: `0.1.0-alpha.65` (`versionCode 65`)

## Arquivos sem extensão

A alpha.65 adiciona um fallback de identificação pelo conteúdo quando o nome não fornece uma extensão útil. A extensão continua sendo a fonte primária para evitar custo de I/O e classificações indevidas.

São reconhecidos por assinatura/conteúdo:

- ZIP;
- APK sem extensão (ZIP com estrutura Android);
- PDF;
- PNG, JPEG, GIF, BMP e WebP;
- texto provável, usando heurística conservadora.

O arquivo usado como caso real, `EditaAi-0.1.0-alpha.3`, começa com assinatura ZIP e contém um único item `EditaAi-0.1.0-alpha.3.apk`. Por isso passa a abrir diretamente no navegador ZIP interno, de onde o APK pode ser aberto no inspector já existente.

## Proteções de desempenho e compatibilidade

- a inspeção por conteúdo só ocorre para nome sem extensão, extensão desconhecida ou extensões genéricas;
- formatos conhecidos como DOCX/XLSX/PPTX não são reclassificados como ZIP;
- o fluxo normal de abertura faz a inspeção em `Dispatchers.IO`;
- resultados são guardados em cache curto por caminho, tamanho e `lastModified`;
- nenhum recurso visual/imagem foi criado ou alterado.
