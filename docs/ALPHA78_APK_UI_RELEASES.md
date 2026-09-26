# Explorador XP 0.1.0-alpha.78

## Tela de APK

A tela de inspeção/instalação foi compactada para reduzir a rolagem inicial sem remover os dados adicionados nas versões anteriores. A comparação com o app instalado inicia recolhida, enquanto detalhes técnicos, permissões e ações extras continuam expansíveis.

O rodapé do visualizador usa o mesmo `XpDialogButton` com ícone de fechamento adotado em Informações do dispositivo e nas demais telas padronizadas.

## Publicação no GitHub

O workflow `Gerar APK` deixou de usar a release fixa `explorador-xp-dev`. Cada `versionName` gera uma tag `v<versionName>` e uma prerelease própria. Reexecutar o workflow da mesma versão atualiza o APK daquela release; uma nova versão cria uma nova release.

Isso evita que todos os APKs fiquem acumulados na mesma lista de assets e mantém a release recém-criada como a entrada mais recente do histórico do GitHub.
