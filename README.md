# Explorador XP

Gerenciador de arquivos Android nativo em **Kotlin + Jetpack Compose**, inspirado no Windows XP e redesenhado para uso confortável em telas de celular.

**Versão atual:** `0.1.0-alpha.2` (`versionCode 2`)  
**Pacote:** `com.exploradorxp.app`  
**Min SDK:** 26  
**Target/Compile SDK:** 35

## O que já está implementado

- Interface principal baseada no mockup aprovado: cabeçalho azul XP, barra de navegação, breadcrumb, cartão de armazenamento, lista/grade e navegação inferior.
- 147 ícones XP gerados para o projeto integrados diretamente em `res/drawable-nodpi`.
- Reconhecimento visual de dezenas de tipos de arquivo: PDF, Word, Excel, PowerPoint, HTML, CSS, JS, JSON, XML, APK, ZIP, RAR, 7Z, imagens, áudio, vídeo, código e outros.
- Navegação real pelo armazenamento compartilhado primário.
- Histórico Voltar / Avançar e ação Subir.
- Busca no diretório atual.
- Modos Lista e Grade.
- Ordenação por nome, data, tamanho e tipo.
- Seleção por toque longo.
- Copiar, recortar/mover e colar, incluindo pastas recursivas.
- Criar pasta, renomear e excluir.
- Abrir arquivos pelo app compatível via `FileProvider`.
- Compartilhar arquivos.
- Favoritos persistentes.
- Recentes persistentes.
- Propriedades básicas de arquivos/pastas.
- Fluxo para conceder `MANAGE_EXTERNAL_STORAGE` em Android 11+.
- Launcher adaptativo baseado na pasta XP, evitando o ícone quadrado.

## Estrutura

```text
app/src/main/java/com/exploradorxp/app/
  MainActivity.kt
  ExplorerScreen.kt
  ExplorerViewModel.kt
  ExplorerModels.kt
  FileRepository.kt
  FileIconMapper.kt
  PreferencesStore.kt

app/src/main/res/drawable-nodpi/
  147 PNGs do pacote visual XP

docs/
  mockup_explorador_android_xp.png
  catalogo_icones.png
  ICON_FILES.txt
```

## Acesso aos arquivos

O app usa acesso amplo ao armazenamento compartilhado porque sua função principal é gerenciamento de arquivos. Em Android 11+, o usuário precisa conceder manualmente **Acesso a todos os arquivos**. Em versões anteriores, o app solicita as permissões legadas necessárias.

A primeira alpha prioriza o armazenamento compartilhado primário. O suporte dedicado a SD/USB por SAF (`ACTION_OPEN_DOCUMENT_TREE`) está planejado para a próxima etapa, para cobrir volumes que não podem ser tratados diretamente por `java.io.File`.

## Build

Abra o projeto no Android Studio e sincronize o Gradle. O workflow `.github/workflows/gerar-apk.yml` também pode gerar um APK debug e publicá-lo diretamente como asset da prerelease `explorador-xp-dev` — sem empacotar o APK em ZIP de artifact.

## Observação

A interface não tenta copiar o Explorer desktop literalmente. O visual XP foi mantido, mas a navegação, seleção e ações foram reorganizadas para toque e telas verticais.
