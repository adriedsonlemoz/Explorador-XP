# Explorador XP 0.1.0-alpha.115

## Correção de compilação do workflow Gerar APK 108

- Importação da extensão Compose `horizontalScroll` no arquivo `ExplorerQuickAccess.kt`. A ausência do import impedia `:app:compileDebugKotlin` na alpha.114.
- Validação estática preventiva do import no workflow, antes dos testes Android; nenhuma checagem existente foi desativada.
- Notas de novidades atualizadas para a correção da alpha.115. As funcionalidades da alpha.114 continuam disponíveis: Fotos/Vídeos por pasta com navegação, lista compacta de aplicativos e painel de ferramentas com descrições.

**Versão:** `0.1.0-alpha.115`  
**Version code:** `115`

**Verificação:** logs do workflow 108 analisados; import, versão e estrutura verificados localmente. Compilação Gradle/Android, APK e testes físicos dependem do GitHub Actions/aparelho.
