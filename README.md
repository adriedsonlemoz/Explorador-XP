# Explorador XP

Gerenciador de arquivos Android nativo em **Kotlin + Jetpack Compose**, inspirado no Windows XP e redesenhado para uso confortável em telas de celular.

**Versão atual:** `0.1.0-alpha.8` (`versionCode 8`)  
**Pacote:** `com.exploradorxp.app`  
**Min SDK:** 26  
**Target/Compile SDK:** 35

## O que já está implementado

- Interface principal baseada no Explorer clássico do Windows XP: barra de título azul, menus Arquivo/Editar/Exibir/Favoritos/Ferramentas/Ajuda, barra de ferramentas compacta, barra de endereço, indicador de armazenamento, lista/grade e barra de status inferior.
- 151 recursos PNG no conjunto visual XP, integrados diretamente em `res/drawable-nodpi`.
- Reconhecimento visual de dezenas de tipos de arquivo: PDF, Word, Excel, PowerPoint, HTML, CSS, JS, JSON, XML, APK, ZIP, RAR, 7Z, imagens, áudio, vídeo, código e outros.
- Navegação real pelo armazenamento compartilhado primário.
- Histórico de navegação com Voltar e Avançar, além da ação Subir.
- Busca no diretório atual.
- Modos Lista e Grade.
- Ordenação por nome, data, tamanho e tipo.
- Toque longo abre menu contextual central; a opção **Selecionar** ativa a seleção múltipla sem substituir o cabeçalho.
- Copiar, recortar/mover e colar, incluindo pastas recursivas; quando há conteúdo na área de transferência, **Downloads** vira temporariamente **Colar** na barra de ferramentas.
- Criar pasta, renomear e excluir.
- Abrir arquivos pelo app compatível via `FileProvider`.
- Compartilhar arquivos.
- Favoritos persistentes.
- Atalho fixo para a pasta Downloads do armazenamento.
- Data e horário de criação visíveis nos itens; propriedades exibem criação e modificação.
- Opção persistente para mostrar/ocultar arquivos ocultos.
- Propriedades básicas de arquivos/pastas.
- Fluxo para conceder `MANAGE_EXTERNAL_STORAGE` em Android 11+.
- Novo launcher legado/adaptativo com fundo azul XP, pasta amarela e lupa.
- Modo tela inteira imersivo com barras do sistema ocultas.

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
  151 PNGs do pacote visual XP

docs/
  mockup_explorador_android_xp.png
  catalogo_icones.png
  ICON_FILES.txt
```

## Acesso aos arquivos

O app usa acesso amplo ao armazenamento compartilhado porque sua função principal é gerenciamento de arquivos. Em Android 11+, o usuário precisa conceder manualmente **Acesso a todos os arquivos**. Em versões anteriores, o app solicita as permissões legadas necessárias.

A primeira alpha prioriza o armazenamento compartilhado primário. O suporte dedicado a SD/USB por SAF (`ACTION_OPEN_DOCUMENT_TREE`) está planejado para a próxima etapa, para cobrir volumes que não podem ser tratados diretamente por `java.io.File`.

## Interface alpha.7

A interface segue o Explorer do Windows XP adaptado a telas Android. O cabeçalho possui menu clássico, barra de ferramentas compacta e campo Endereço. O Endereço também permite alternar entre armazenamento interno e cartão SD quando detectado. O cartão de capacidade fica restrito à página inicial, deixando as pastas com mais área útil.

Os ícones principais de pastas, navegação e dispositivos foram atualizados para uma aparência mais próxima do Windows XP, mantendo os recursos já existentes para tipos de arquivo.

Na alpha.7, a barra de ferramentas foi compactada para caber inteira sem rolagem horizontal. O toque longo em arquivo/pasta abre as ações no centro da tela; o toque longo em área vazia oferece Colar, Nova pasta, Selecionar tudo, Atualizar e Propriedades.

## Build

Abra o projeto no Android Studio e sincronize o Gradle. O workflow `.github/workflows/gerar-apk.yml` também pode gerar um APK debug e publicá-lo diretamente como asset da prerelease `explorador-xp-dev` — sem empacotar o APK em ZIP de artifact.

## Observação

O cabeçalho reproduz de forma mais fiel a estrutura do Explorer do Windows XP, mas mantém áreas de toque e comportamento adaptados a telas verticais de Android.
