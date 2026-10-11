# Explorador XP 0.1.0-alpha.94

## Correção de compilação do workflow Gerar APK 89

- Resolvidos 13 erros `Unresolved reference` em `ArchiveManager.kt` e `MainActivity.kt`.
- Definidos em `ArchiveResourceLimits.kt` os limites de **20.000 entradas ZIP**, **8 GiB por extração**, **512 MiB por pré-visualização** e **4 GiB por arquivo externo temporário**.
- Implementada `trimArchivePreviewCache`: limpeza de previews com mais de sete dias e política global de até **1 GiB** em cache privado. A pré-visualização atual e os temporários recentes em andamento são preservados.
- Cache inspecionado antes de visualizar e após finalizar a extração do item; acesso a arquivo já em cache renova sua recência.
- Adicionados testes unitários para limites, preservação de arquivo ativo, expiração e limite global de cache.
- Sem mudanças no acesso a arquivos pela rede. O recurso fica planejado para `0.1.0-alpha.95`, após validação da correção atual.

## Mantido da versão alpha.93

- Substituição de arquivos com staging/backup, extração ZIP protegida, WebView com preview restrito, limites de arquivos externos e consultas remotas opcionais.

Versão: `0.1.0-alpha.94`  
Version code: `94`

**Validação:** erro de compilação diagnosticado a partir dos logs do GitHub Actions; testes locais de Kotlin foram executados para a política de cache. A compilação completa com Gradle/Android SDK e testes Android ainda precisam ser executados pelo workflow antes de distribuir o APK.
