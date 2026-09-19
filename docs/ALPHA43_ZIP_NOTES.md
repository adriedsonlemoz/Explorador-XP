# Alpha 43 — ZIP

Versão: `0.1.0-alpha.43` (`versionCode 43`)

## Escopo

O visualizador ZIP foi substituído por um navegador hierárquico com pesquisa, ordenação, seleção, preview por item, extração total/parcial e escolha de destino.

## Extração

- `Extrair aqui` cria uma pasta com o nome do ZIP por padrão.
- `Extrair para...` abre seletor interno de pasta e lembra o último destino escolhido.
- Conflitos: renomear automaticamente, substituir ou ignorar.
- Progresso: porcentagem, bytes, velocidade, item atual e contagem.
- Cancelamento remove o arquivo parcial atualmente em escrita quando possível.
- Resumo final inclui extraídos, renomeados, ignorados e erros; quando somente um arquivo foi extraído, oferece **Abrir arquivo**, além de **Abrir pasta**.

## Segurança e compatibilidade

- Caminhos `..`, absolutos Unix/Windows e escapes da pasta destino são bloqueados.
- ZIP protegido por senha usa Zip4j 2.11.5, incluindo AES/Zip Standard suportados pela biblioteca.
- Verificação de integridade lê as entradas até o fim para acionar verificação CRC.
- O suporte interno completo continua restrito a ZIP; RAR/7Z/TAR/GZ não são anunciados como extraíveis internamente sem implementação real.
- Nenhuma imagem/mockup foi criada ou alterada.
