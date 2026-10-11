# Explorador XP 0.1.0-alpha.96

## Descoberta na rede local por mDNS

- **Cliente:** Ferramentas > Rede local (WebDAV) > Procurar aparelhos. Os servidores WebDAV HTTPS detectados aparecem com nome e IP. Toque em **Usar** para preencher o endereço.
- **Servidor:** ao ativar o compartilhamento, anuncia `_webdavs._tcp.` na rede Wi-Fi; o anúncio some quando o servidor é parado.
- **Autenticação:** a descoberta não é confiável por si só. Antes de conectar, compare manualmente o SHA-256 exibido no outro aparelho e informe a senha de sessão. Escolher um endereço encontrado limpa os campos de credenciais anteriores.
- **Bateria:** a busca mDNS é manual e termina ao tocar em Parar, ao escolher Usar ou ao fechar a tela.
- **Fallback:** se o hotspot/roteador bloquear multicast, informe o IP manualmente.
- **Disponibilidade:** somente WebDAV por HTTPS. SMB2/3, sincronização e varredura de portas não foram implementados.
- **Validação:** testes JVM de endereços e compilação Kotlin isolada com stubs; compilar APK pelo GitHub Actions e testar Wi-Fi real entre aparelhos antes de usar em dados importantes.

Versão: `0.1.0-alpha.96`  
Version code: `96`
