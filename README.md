# Explorador XP

Gerenciador de arquivos Android nativo em **Kotlin + Jetpack Compose**, inspirado no Windows XP e redesenhado para uso confortável em telas de celular.

**Versão atual:** `0.1.0-alpha.10` (`versionCode 10`)  
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
- Toque longo entra diretamente no modo de seleção; a barra de ferramentas troca temporariamente para ações de seleção e volta ao normal ao concluir/cancelar.
- Copiar, recortar/mover e colar, incluindo pastas recursivas; quando há conteúdo na área de transferência, **Downloads** vira temporariamente **Colar** na barra de ferramentas.
- Criar pasta, renomear e excluir.
- Visualizador interno para imagens, textos/código editáveis, HTML, PDF, ZIP, áudio/vídeo e informações de APK; formatos não suportados continuam disponíveis via `Abrir com...`.
- Abertura externa por aplicativo compatível via `FileProvider` quando necessário.
- Compartilhar arquivos.
- Favoritos persistentes.
- Atalho fixo para a pasta Downloads do armazenamento.
- Data e horário de criação visíveis nos itens; propriedades exibem criação e modificação.
- Opção persistente para mostrar/ocultar arquivos ocultos.
- Propriedades básicas de arquivos/pastas.
- Fluxo para conceder `MANAGE_EXTERNAL_STORAGE` em Android 11+.
- Novo launcher legado/adaptativo com fundo azul XP, pasta amarela e lupa.
- Barras de status e navegação do Android permanecem visíveis; o modo imersivo/tela inteira foi removido.

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

## Interface alpha.10

A interface segue o Explorer do Windows XP adaptado a telas Android. O cabeçalho possui menu clássico, barra de ferramentas compacta e campo Endereço. O Endereço também permite alternar entre armazenamento interno e cartão SD quando detectado. O cartão de capacidade fica restrito à página inicial, deixando as pastas com mais área útil.

Os ícones principais de pastas, navegação e dispositivos foram atualizados para uma aparência mais próxima do Windows XP, mantendo os recursos já existentes para tipos de arquivo.

Na alpha.7, a barra de ferramentas foi compactada para caber inteira sem rolagem horizontal. O toque longo em arquivo/pasta abre as ações no centro da tela; o toque longo em área vazia oferece Colar, Nova pasta, Selecionar tudo, Atualizar e Propriedades.

Na alpha.9, os menus Arquivo/Editar/Exibir/Favoritos/Ferramentas/Ajuda seguem um fluxo mais próximo do Explorer clássico, com menus suspensos compactos. Ferramentas ganhou `Organizar ›` com nome, data, tamanho, tipo e `Pastas primeiro`. Ajuda ganhou Manual de Ajuda, Sobre e Doação.

O modo de seleção passa a usar a própria barra de ferramentas com sete ações: Copiar, Mover, Excluir, Renomear, Compartilhar, Propriedades e Selecionar tudo. O menu contextual do botão de opções foi remodelado para uma lista compacta no estilo clássico.

Na alpha.10, o pacote de ícones v2 foi aplicado às ações principais da barra e aos botões de opções. Os ícones superiores ficaram maiores e mais encorpados, e o botão lateral de opções deixou de usar a bolinha azul. A tipografia compacta foi normalizada para melhorar a legibilidade sem perder o layout de Explorer clássico. Os controles decorativos de minimizar, maximizar e fechar foram removidos do cabeçalho principal. O aplicativo também deixou o modo imersivo: as barras de sistema do Android voltam a permanecer visíveis.

## Build

Abra o projeto no Android Studio e sincronize o Gradle. O workflow `.github/workflows/gerar-apk.yml` também pode gerar um APK debug e publicá-lo diretamente como asset da prerelease `explorador-xp-dev` — sem empacotar o APK em ZIP de artifact.

## Observação

O cabeçalho reproduz de forma mais fiel a estrutura do Explorer do Windows XP, mas mantém áreas de toque e comportamento adaptados a telas verticais de Android.
