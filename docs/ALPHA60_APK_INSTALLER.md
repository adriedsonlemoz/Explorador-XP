# Explorador XP — alpha.60

Versão: `0.1.0-alpha.60` (`versionCode 60`)

## Renovação do instalador de APK

A alpha.60 transforma o visualizador de APK em uma etapa de inspeção antes de encaminhar o arquivo ao instalador do Android. O Explorador XP não substitui o instalador do sistema; ele analisa o pacote e apresenta as informações relevantes para que a instalação seja iniciada com contexto.

### Comparação de versões

- instalação nova: nenhum pacote com o mesmo `packageName` está instalado;
- atualização: `versionCode` do APK é maior;
- reinstalação: `versionCode` é igual;
- downgrade: `versionCode` é menor e a tentativa direta é bloqueada, pois o Android normalmente exige remover a versão atual primeiro.

### Compatibilidade

- `minSdk` é comparado à API do aparelho;
- as bibliotecas nativas presentes em `lib/<ABI>/*.so` são indexadas;
- as ABIs do APK são comparadas a `Build.SUPPORTED_ABIS`;
- APK sem bibliotecas nativas é tratado como compatível no critério de CPU.

### Assinatura

O inspetor solicita dados de assinatura do arquivo e, quando há pacote instalado, também do aplicativo atual. Os certificados são resumidos por SHA-256. Se não houver certificado em comum entre as cadeias lidas, o painel alerta que o Android não permitirá atualizar diretamente o pacote instalado.

### Permissões

As permissões declaradas no manifesto do APK são listadas. Quando o Android conhece a permissão, o Explorador usa o rótulo fornecido pelo sistema e identifica as permissões cuja proteção base é `dangerous`. Isso é informativo: não significa que a permissão já foi concedida.

### Fluxo de instalação

- **Instalar / Atualizar / Reinstalar** continua abrindo o instalador oficial do Android;
- se `REQUEST_INSTALL_PACKAGES` ainda não estiver autorizado para o Explorador XP, a tela **Permitir desta fonte** é aberta;
- ao voltar depois de autorizar, a instalação pendente continua automaticamente;
- **Gerenciar app** abre `ACTION_APPLICATION_DETAILS_SETTINGS`; ao retornar, os metadados são lidos novamente.

### Validação local

O projeto ainda não inclui `gradlew` e o ambiente não possui Android SDK/Gradle configurado. Foram executados:

- compilação JVM do helper puro `ApkInspectorSupport.kt`;
- smoke test real de ZIP/APK sintético para extração de ABIs;
- comparação de versão e assinatura;
- SHA-256 conhecido para `abc`;
- compilação de `ApkInspector.kt` contra stubs mínimos da API Android, incluindo os overloads API 33 de `PackageInfoFlags`/`PermissionInfoFlags`;
- parser smoke de `InternalViewer.kt` sem diagnósticos de sintaxe.

Nenhum recurso de imagem foi adicionado ou alterado nesta etapa.
