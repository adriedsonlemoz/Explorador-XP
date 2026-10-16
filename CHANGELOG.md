## 0.1.0-alpha.130 (2026-10-16)

- ZIP: botões na ordem **Extrair, Verificar, Salvar**.
- Salvar copia o ZIP original diretamente para `Armazenamento interno/XP/`, criando a pasta se faltar e sem abrir seletor SAF.
- Arquivos de mesmo nome ganham sufixo `(1)`, `(2)` etc. sem sobrescrita; cópia incompleta é removida em caso de falha.
- Se o ZIP já estiver na pasta XP, não duplica. Falta de autorização de armazenamento é informada sem abrir seletor de destino.
- Demais ferramentas e interfaces da alpha.129 preservadas. Compilação Gradle/Android e teste físico pendentes.

## 0.1.0-alpha.129 (2026-10-16)

- ZIP: botão Info removido somente da faixa superior; detalhes do arquivo permanecem disponíveis no menu ⋮.
- A faixa superior ZIP usa três ações uniformes: Extrair, Salvar e Verificar.
- APK: versão instalada (quando disponível) e versão do APK agora aparecem no cartão de identificação, antes do nome de pacote/certificado; comparação detalhada segue expansível.
- APK: ações de Instalar/Reinstalar/Atualizar, Abrir, Desinstalar e Gerenciar em linha única, com ícones acima dos títulos. O fluxo de downgrade e confirmações foi preservado.
- Versões sem `versionName` usam `versionCode`, evitando ocultar ações quando o aplicativo está instalado.
- Testes adicionados para rotulagem de versões com e sem `versionName`. Build Android e testes visuais pendentes.

## 0.1.0-alpha.128 (2026-10-16)

- Corrige o build 119: o teste `ReleaseNotesScopeTest` não exige mais as frases antigas da alpha.126, que causaram falha na tarefa `:app:testDebugUnitTest` mesmo após compilação Kotlin bem-sucedida (168 testes, uma falha).
- O teste agora verifica metadados da versão, conteúdo não vazio, limite de entradas e ausência de itens duplicados ou histórico de versões na tela de novidades.
- O workflow passa a usar `tools/validate_source.py`, com versão obtida de `VERSION` e validação cruzada com Gradle, JSON, README, CHANGELOG e RELEASE, sem valor fixo de alpha.127.
- Preservados quatro botões do ZIP na mesma linha e cinco ações rápidas de edição de código, sem alterações de comportamento.
- Build Android completo, lint e teste físico continuam pendentes nesta entrega; não foi gerado APK localmente.

## 0.1.0-alpha.127 (2026-10-16)

- Visualizador ZIP: **Extrair, Salvar, Verificar, Info** agora dividem uma única faixa horizontal, com ícones e textos compactos e 49 dp de altura.
- Editor de texto/código: nova faixa inferior acima das informações de linha/coluna para **Selecionar palavra, Copiar, Colar, Recortar, Selecionar tudo**.
- Recortar e Copiar dependem de seleção; Recortar e Colar são desabilitados em modo somente leitura. A seleção preserva o cursor e funciona sobre identificadores de código.
- Novos testes de seleção de palavra, testes estruturais e metadados sincronizados. Testes Android físicos pendentes.

## 0.1.0-alpha.126 (2026-10-16)

- Adicionado **Salvar ZIP** na visualização de arquivos ZIP, usando o seletor de documentos do Android.
- Copia o ZIP original sem recompressão ou extração, inclusive quando recebido por abertura externa.
- Verifica bytes transferidos e tenta limpar cópias incompletas após erro; a interface exibe sucesso ou falha.
- Organiza ações em duas linhas para facilitar uso em telas estreitas.
- Corrige o versionCode incorreto do cabeçalho do README e sincroniza os metadados da nova versão.

## 0.1.0-alpha.125 (2026-10-14)

- Corrigido teste unitário desatualizado da barra inferior, que esperava seis atalhos apesar da inclusão de Início.
- Validada a ordem de sete destinos com Início na quarta posição.
- Mantidos os chips compactos de aplicativos e demais recursos da alpha.124.

## 0.1.0-alpha.125 (2026-10-14)

- Redesenha os filtros rápidos de Aplicativos instalados como chips compactos com ícones, todos em uma linha rolável.
- Diferencia visualmente Todos, Usuário, Sistema, Nome, Maiores, Menores, Instalados e Atualizados; conserva filtro de tipo e ordenação simultâneos.
- Mantém realce dos filtros ativos e descrição de acessibilidade para estado selecionado.
- Preserva o botão Início central da barra inferior com sete atalhos e as demais funcionalidades da alpha.123.

## 0.1.0-alpha.125 (2026-10-13)

- Unifica os filtros de Aplicativos instalados em uma única linha horizontal com Todos, Usuário, Sistema, Nome, Maiores, Menores, Instalados e Atualizados.
- Adiciona o botão Início na barra inferior fixa do Explorador XP, posicionado entre Downloads e Rede.
- O atalho Início retorna à pasta principal do armazenamento e passa a ficar selecionado quando a tela principal está ativa.

## 0.1.0-alpha.122 (2026-10-13)

- Armazenamento em abas com Categorias, Pastas e Arquivos.
- Resumo do espaço de aplicativos e filtros rápidos na lista de apps.
- Reconhecimento de ZIP sem extensão para arquivos vindos do GitHub Manager.
