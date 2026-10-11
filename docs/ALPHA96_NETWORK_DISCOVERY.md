# Alpha.96: descoberta WebDAV HTTPS

A nova implementação usa Android NsdManager (DNS Service Discovery/mDNS). Tipo `_webdavs._tcp.`.

### Ciclo de vida
- O servidor anuncia seu serviço assim que o WebDAV HTTPS inicia com sucesso; unregister durante desligamento. Uma falha de mDNS não afeta o acesso direto via IP.
- Busca somente iniciada por ação do usuário, mantida enquanto a tela estiver visível e liberada no fechamento. MulticastLock adquirido apenas durante a busca e liberado no `stop()` para atender versões Android antigas.
- Resultados são resolvidos em sequência para compatibilidade com versões antigas do Android. Recebimentos após parar/reiniciar são ignorados por identificador de sessão.
- Endereços IPv4 devem pertencer às faixas privadas RFC1918 ou link-local.

### Segurança
- Nenhuma credencial, certificado ou nome de pasta é anunciado por mDNS.
- Selecionar um serviço apenas preenche a URL e zera campos de SHA-256/senha.
- Autenticação Basic continua sendo feita apenas por HTTPS e *depois* da validação de certificado SHA-256.
- DNS-SD pode ser falsificado: usuário precisa conferir fingerprint em tela no equipamento correto.
- Não há conexão automática, fallback HTTP, SMB ou descoberta em IP público.

### Validação pendente
- Build Android Gradle completo no GitHub Actions; testes em dois Android na mesma rede; anúncio com roteador e hotspot; suspensão de tela; cancelamento da busca; tentativa de anúncio forjado; limpeza de MulticastLock.
- Se os servidores não forem descobertos, continuar com conexão manual.
