# Explorador XP — alpha.82 — Nome comercial e imagem do aparelho

## Objetivo

Adicionar nome comercial e imagem real do modelo sem misturar catálogo externo com valores medidos pelo Android e sem transformar resultados ambíguos em dados confirmados.

## Identidade

A entrada sempre começa por `Build.MANUFACTURER`, `Build.BRAND`, `Build.DEVICE`, `Build.MODEL` e `Build.PRODUCT`. `DeviceIdentityMatcher` exige modelo exato e correspondência exata de fabricante ou marca. Se existirem múltiplas linhas com o mesmo modelo, `device` é usado apenas como desempate exato; dois nomes comerciais ainda possíveis resultam em **Não identificado**.

O catálogo local fica em `app/src/main/assets/device_catalog.json`. A entrada `Xiaomi / Redmi / 23129RN51X -> Redmi A3` foi incluída porque o código de modelo e o nome comercial são confirmados pela documentação do próprio fabricante. A estrutura do catálogo segue os campos públicos da lista de dispositivos suportados pelo Google Play.

## Atualização do catálogo

`DeviceCatalogService` usa `https://storage.googleapis.com/play_public/supported_devices.csv`, fonte pública oficial, sem chave ou API paga. O CSV é lido como UTF-16LE e apenas linhas que correspondem exatamente ao modelo e à marca/fabricante atuais são mantidas em cache.

- cache de correspondência: 30 dias;
- cache negativo: 30 dias;
- limite defensivo de tamanho do catálogo: 24 MiB;
- conexão HTTPS, timeouts e User-Agent próprios;
- falha de rede/JSON/cache nunca bloqueia a abertura da tela;
- o aquecimento no início do app acontece em `Dispatchers.IO` e respeita a configuração de consultas externas.

## Imagem

A busca só ocorre para identidade confirmada. `DeviceImageService` pesquisa Wikidata e só aceita uma entidade quando:

1. o nome/alias normalizado é exatamente o nome comercial identificado (ou fabricante + nome comercial);
2. o fabricante (`P176`) corresponde exatamente ao fabricante ou à marca do Android;
3. existe uma imagem própria (`P18`);
4. existe apenas uma entidade que satisfaça os critérios.

Se qualquer regra falhar, o app usa o ícone genérico. Assim, uma fotografia apenas tirada **com** o aparelho não é tratada como fotografia **do** aparelho.

Depois da validação da entidade, Wikimedia Commons fornece uma thumbnail de até 320 px e os metadados disponíveis de autor/licença/origem. O navegador só é aberto quando o usuário toca em **Ver fonte**. `DeviceImageCache` mantém a persistência de miniatura/metadados separada de `DeviceImageRepository` e `DeviceImageService`.

## Cache e economia de dados

- miniaturas e metadados: cache por 30 dias;
- nenhum conjunto de fotos é incluído no APK;
- resultado confiavelmente “sem imagem” recebe cache negativo;
- timeout, servidor indisponível, JSON inválido ou download interrompido não são gravados como “sem imagem”, permitindo nova tentativa futura;
- respostas JSON e imagens têm limites defensivos de tamanho.

## Privacidade

As consultas externas usam somente fabricante, marca, código do modelo, `device` e nome comercial já identificado. Não são enviados IMEI, serial, Android ID, MAC, localização, telefone, operadora, SIM ou conteúdo de arquivos.

## Configuração

Em **Ferramentas > Configurações** existe:

`Buscar imagem do modelo pela internet`

Quando desligada, o app usa apenas o catálogo local e dados Android, mostra ícone genérico e não faz atualização do catálogo nem consulta de Wikidata/Wikimedia.

## Interface

O cabeçalho exibe miniatura/ícone, nome comercial, fabricante e modelo. A seção **Informações do modelo** mostra claramente a origem da identificação e o estado da imagem. A partir daí, a legenda **DETECTADO NESTE APARELHO** antecede as métricas reais já existentes.

Ao tocar em uma imagem real, o painel de detalhes mostra modelo, fonte, autor, licença, entidade Wikidata, data da consulta e a ação explícita **Ver fonte**.

## Testes adicionados

- modelo conhecido (`Xiaomi / 23129RN51X / Redmi A3`);
- modelo desconhecido;
- mesmo modelo com fabricante diferente;
- colisão de nome comercial e desempate por `device`;
- política de rede desabilitada/sem internet;
- imagem encontrada;
- imagem inexistente;
- fabricante divergente na entidade;
- múltiplas entidades válidas;
- cache fresco e expirado;
- linha CSV inválida.

## Limitação de build neste ambiente

O ZIP recebido não contém `gradlew` e o ambiente de edição não possui Gradle/Android SDK configurados. Por isso, os testes Android/JVM, lint e `assemble` precisam rodar no workflow do projeto ou em uma máquina com o toolchain Android. As validações estáticas e de formato continuam sendo executadas antes da entrega.
