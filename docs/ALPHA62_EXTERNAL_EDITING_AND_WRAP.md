# Explorador XP — alpha.62 — Abrir com + quebra de linha

## Objetivo

Completar a evolução iniciada na alpha.61 para arquivos de texto/código recebidos por **Abrir com** e tornar a quebra automática de linha fácil de encontrar tanto na edição quanto na visualização.

## Quebra automática de linha

- A preferência continua persistida em `text_code_editor`.
- A ação **Quebra linha** foi colocada diretamente na barra do editor/visualizador.
- A mesma opção aparece em **Mais** e permanece também nas configurações do editor.
- Quando ativa, o `CodeEditText` desabilita a rolagem horizontal, volta `scrollX` para zero e deixa o layout quebrar visualmente dentro da largura disponível.
- Os números de linha continuam representando linhas lógicas; linhas apenas quebradas visualmente não recebem numeração falsa.

## Arquivos recebidos por Abrir com

O arquivo continua sendo copiado para `cache/external-open` antes da edição. Isso mantém a sessão de trabalho separada da origem e evita gravar no documento externo durante digitação, desfazer/refazer ou preview.

`ExternalOpenOrigin` conserva URI, MIME, nome, permissões concedidas e SHA-256 do conteúdo recebido.

### Com permissão de escrita

- o editor permite alterar a cópia de trabalho;
- **Salvar original** verifica primeiro se o SHA-256 atual da URI ainda corresponde à versão aberta;
- se não houve mudança externa, o conteúdo é gravado em `content://` e verificado novamente por SHA-256;
- se outro aplicativo alterou o original, a sobrescrita é interrompida e o usuário é orientado a usar **Salvar como**.

### Sem permissão de escrita

A cópia ainda pode ser editada, mas **Salvar original** fica indisponível. **Salvar como** abre `CreateDocument`, garantindo que a cópia escolhida pelo usuário fique fora do cache temporário.

## Segurança de gravação

Para arquivos editáveis, uma cópia curta do conteúdo anterior é mantida apenas durante a gravação externa. Se o provedor falhar após começar a sobrescrita, o editor tenta restaurar esse conteúdo. A gravação final é validada por SHA-256.

## Compatibilidade

- `applicationId`: `com.exploradorxp.app` (inalterado)
- Versão: `0.1.0-alpha.62` / `versionCode 62`
- Nenhuma imagem ou mockup criado/modificado nesta etapa.
