# alpha.133: resumo do APK e ordem de ações

- Comparação baseada em `versionCode`, evitando comparação textual incorreta entre alpha/beta/semver. `versionName` é o único valor mostrado no selo resumido, com fallback `Não informada` se o APK não possuir nome de versão.
- No cabeçalho, a etiqueta do APK fica à direita do nome; o nome aceita até duas linhas e elipse e o selo possui limite de largura para telas estreitas.
- O selo da versão do APK e o estado inferior compartilham a mesma cor: verde = atualização ou pacote não instalado; azul = igual; vermelho = pacote anterior. Versão instalada permanece sempre em azul.
- Pacote e nome do certificado deixam de aparecer no cartão de resumo; a verificação de assinatura continua ativa e pode ser lida nos detalhes de comparação.
- Ordem de ações: principal, Gerenciar, Desinstalar, Abrir. Gestores de intenção e flags de habilitação não foram alterados.
- Testes adicionados para renderização de versionName sem versionCode e classificação de estados; build Android e testes de interface em aparelho pendentes.
