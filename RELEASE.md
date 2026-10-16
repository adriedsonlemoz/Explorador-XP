# Explorador XP 0.1.0-alpha.131

**Versão:** `0.1.0-alpha.131`  
**versionCode:** `131`

## Correção do workflow Gerar APK 122

- O compilador Kotlin parava em `ArchiveViewer.kt:656-657` porque as chamadas `rememberLauncherForActivityResult` e `ActivityResultContracts.RequestPermission` estavam sem importação.
- Adicionados os imports `androidx.activity.compose.rememberLauncherForActivityResult` e `androidx.activity.result.contract.ActivityResultContracts`.
- `tools/validate_compose_imports.py` agora também impede o uso desses símbolos sem importação adequada, antes da etapa Gradle.

## Recursos preservados

- Visualizador ZIP: faixa **Extrair / Verificar / Salvar**; Salvar copia automaticamente para **Armazenamento interno/XP/** sem seletor e sem sobrescrever arquivos de mesmo nome.
- APK: versões visíveis e quatro ações compactas em linha; editor: comandos rápidos no rodapé.

**Validação:** importações e estrutura verificadas localmente. Build Android completo e teste em aparelho ainda pendentes.
