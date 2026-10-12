# Explorador XP 0.1.0-alpha.105

## Rede local com interface simplificada

- A entrada de Rede usa três ações grandes: Acessar, Compartilhar e QR.
- A busca exibe os compartilhamentos anunciados, com alternativas por IP e QR sem menus longos.
- Compartilhar pastas é uma tela compacta com controles de pastas, servidor e informações de conexão sob demanda.
- QR gerado apenas ao solicitar; leitor QR em janela com câmera integrada, permissão explícita e fechamento ao decodificar.
- Conexão WebDAV por IP com formulário reduzido, opções avançadas opcionais e confirmação antes de autenticar.
- Mensagens de erro como `connection closed` foram substituídas por categorias explicativas e diagnósticos copiáveis, sem segredos.
- Mantido pinning SHA-256 para WebDAV HTTPS e demais recursos de SMB/galeria.

**Versão:** `0.1.0-alpha.105`  
**Version code:** `105`  

**Verificação:** análise sintática e testes Kotlin puros. Necessário validar Gradle, Android Lint, layout e câmera em aparelhos reais.
