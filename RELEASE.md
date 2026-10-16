# Explorador XP 0.1.0-alpha.130

**Versão:** `0.1.0-alpha.130`  
**versionCode:** `130`

## Interface ZIP

- Faixa superior com **Extrair, Verificar e Salvar** em uma única linha, de larguras iguais.
- **Informações** continua disponível em **⋮ > Informações**, sem perda da função.

## Interface de APK

- Cartão do aplicativo destaca a **versão instalada** quando identificada e a **versão do APK selecionado**, utilizando dados reais do PackageManager.
- Se o Android não fornecer `versionName`, o `versionCode` aparece como alternativa, sem inventar versões.
- Quando há aplicativo instalado, as quatro ações **Instalar/Reinstalar/Atualizar, Abrir, Desinstalar e Gerenciar** aparecem em uma única linha com ícone acima do texto. Versão anterior usa o rótulo compacto **Anterior**, mantendo confirmação de remoção antes da instalação.
- Quando não há aplicativo instalado, o botão **Instalar** permanece em largura total.
- Fluxos de permissões, assinatura, confirmação de desinstalação e atualização não foram alterados.

**Validação:** testes isolados e inspeção estrutural do código; Gradle/Android, visualização em aparelhos e geração de APK ainda pendentes.
