# Alpha.112: ícones de APK e navegação compacta

## Mudanças

1. **Ícone de APK:** `FileThumbnail.kt` usa `PackageManager.getPackageArchiveInfo` fora da thread principal, com `sourceDir/publicSourceDir` do arquivo, LRU 8 MiB e no máximo duas cargas simultâneas. Fallback vetorial para APK ilegível. O APK não é instalado nem executado.
2. **Lista compacta:** em `Exibir > Lista compacta (ícones menores)`, altura aproximada de metade da linha normal, ícone 24 dp e informações compactadas. Persistida no `PreferencesStore`.
3. **Ordenação:** `Exibir > Classificar por nome/data/tamanho/tipo` e `Pastas primeiro` usam o processamento já existente do `ExplorerViewModel`, sem reler o sistema de arquivos; comando Organizar retirado de Ferramentas.
4. **Aplicativos instalados:** botão discreto no cabeçalho com ordenação e filtros de tamanho/estado. `AppManagerLogic.filterAndSort` faz filtro combinado com busca e tipo de aplicativo. Quando há permissão de estatística de uso, usa total app + dados (que já inclui cache); sem permissão usa tamanho da instalação APK/splits. O valor é uma aproximação de consumo conforme a disponibilidade do Android.

## Testes pendentes

- Compilação Android completa via workflow e instalação da alpha.112.
- Ícones de APKs válidos e inválidos em Android 8–15; comportamento de APKs muito grandes, scrolling rápido e baixa RAM.
- Lista compacta sob fontes ampliadas/seleção/context menu, persistência após reinício.
- Verificação dos filtros com e sem Acesso ao uso e atualização assíncrona das estatísticas.
