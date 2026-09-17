# Alpha 27 — pipeline de ícones vetoriais

- Removidos do módulo `app` os 150 PNGs `drawable-xxxhdpi` de tipos de arquivo e ações.
- `FileIconMapper` agora retorna categoria visual + etiqueta curta de extensão.
- Lista, grade, propriedades, menus de contexto, toolbar, armazenamento e estados vazios usam `ImageVector`.
- APK local tenta usar o ícone real do pacote via `PackageManager`, em `Dispatchers.IO`, com cache LRU de 4 MiB e uma leitura simultânea.
- Removido `CachedResourceIcon.kt`, pois os bitmaps de recursos deixaram de ser necessários no caminho crítico.
- Recursos brutos do módulo app caíram de ~3,97 MB para ~0,85 MB antes do R8.
- Launcher não foi alterado.
