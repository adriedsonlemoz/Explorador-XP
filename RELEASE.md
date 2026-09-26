# Explorador XP 0.1.0-alpha.85

## Gerenciamento direto de aplicativos

O visualizador de APK ganhou uma ação **Desinstalar** para aplicativos já instalados. A ação abre diretamente a confirmação de remoção do Android, sem exigir que o usuário passe primeiro pela tela de detalhes do aplicativo.

## Instalação de versão anterior

Quando o APK selecionado possui `versionCode` menor que o aplicativo instalado, o Explorador XP identifica o downgrade e mostra a comparação das duas versões. Ao confirmar **Instalar versão anterior**, o aplicativo:

1. alerta que a desinstalação pode apagar dados locais;
2. solicita ao Android a remoção da versão instalada;
3. verifica se o pacote realmente deixou de estar instalado;
4. somente então abre o instalador para o APK antigo.

Se a remoção for cancelada ou falhar, o APK antigo não é aberto automaticamente. O Explorador XP também não tenta auto-downgrade do próprio pacote, pois se remover encerraria o processo antes da continuação.

## Compatibilidade preservada

Permanecem disponíveis instalação normal, atualização, reinstalação, Abrir, Gerenciar/Detalhes do app, comparação de versões e assinaturas, permissões, compartilhamento, localização, cópia de pacote/SHA-256, extração de ícone e resumo do manifesto.

Versão: `0.1.0-alpha.85`  
versionCode: `85`
