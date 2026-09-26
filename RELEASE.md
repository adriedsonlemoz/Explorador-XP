# Explorador XP 0.1.0-alpha.86

## Gerenciador de aplicativos instalados

**Ferramentas > Aplicativos instalados** agora abre um módulo próprio para consultar e gerenciar os pacotes presentes no aparelho. A tela usa lista, busca por nome/pacote, filtros **Todos / Usuário / Sistema** e ordenação por nome ou tamanho. Cada linha exibe o ícone fornecido pelo aplicativo instalado, nome, packageName, versão, estado e tamanho disponível.

## Armazenamento sem valores inventados

Sem autorização especial, o Explorador XP soma apenas os arquivos APK base/splits que o Android informa e identifica esse número como **APK**. Quando o usuário concede **Acesso ao uso**, o módulo consulta `StorageStatsManager` e apresenta **Aplicativo/código**, **Dados**, **Cache** e **Total**. Como `dataBytes` já inclui cache, o Total usa código + dados e não soma cache novamente.

## Detalhes e ações

A ficha de cada pacote mostra tipo do aplicativo, versão/versionCode, SDK mínimo/alvo, datas de instalação e atualização, instalador quando informado pelo Android e permissões declaradas com o estado concedido/não concedido. Também oferece **Abrir aplicativo**, **Desinstalar**, **Copiar nome do pacote** e **Detalhes no Android**.

Em aplicativos do sistema, a ação de remoção é apresentada como **Desinstalar / remover atualizações** e o Android decide se o pacote pode ser removido, se apenas as atualizações podem ser revertidas ou se a operação deve ser bloqueada. Não há tentativa de remoção silenciosa.

## Limpar dados

O Android não permite que um aplicativo comum apague diretamente os dados privados de outro pacote. Por isso **Limpar dados** explica essa restrição e abre os detalhes oficiais do aplicativo para o usuário concluir a operação. Nenhuma API oculta, root ou comando privilegiado foi incluído.

## Compatibilidade

Todas as funções existentes do Explorador XP foram preservadas, incluindo explorador de arquivos, Lixeira, armazenamento, busca avançada, editor, visualizadores, Informações do dispositivo e inspector/instalador de APK com desinstalação e downgrade assistido.

Versão: `0.1.0-alpha.86`  
versionCode: `86`
