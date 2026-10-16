# Explorador XP 0.1.0-alpha.128

**Versão:** `0.1.0-alpha.128`  
**versionCode:** `128`

## Correção do build 119

- Ajustado o teste unitário da tela **Novidades** que procurava textos da alpha.126, mas era executado sobre a alpha.127.
- Agora o teste valida as propriedades estáveis das novidades, sem exigir frases específicas de lançamentos antigos.
- O workflow valida a versão atual dinamicamente, evitando que o verificador fique preso ao número de uma versão anterior.
- Mantidos os quatro botões compactos do visualizador ZIP e a barra de comandos do editor.

**Validação:** o build 119 compilou o Kotlin da alpha.127 e falhou por um dos 168 testes. Esta versão inclui correção da causa detectada. Compilação Android completa, lint e teste em aparelho devem ser repetidos no GitHub Actions.
