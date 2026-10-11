# Explorador XP 0.1.0-alpha.103

## Correção de compilação: build Gerar APK 96

- Corrigida a referência não resolvida `ensureActive` em `ExplorerQuickAccess.kt`, causada pela ausência do import da extensão `kotlinx.coroutines.ensureActive`.
- Mantido cancelamento cooperativo na cópia de arquivos selecionados da galeria.
- Mantidas as melhorias da alpha.102: Aplicativos Instalados compacto, Fotos/Vídeos com miniaturas e QR de conexão WebDAV HTTPS.
- Funcionalidades anteriores SMB2/SMB3, rede WebDAV e barra inferior também preservadas.

**Versão:** `0.1.0-alpha.103`  
**Version code:** `103`

**Validação:** falha comprovada no log do build 96; testes locais descritos em `docs/VALIDATION.json`. A compilação Android completa no GitHub Actions e os testes físicos ainda são necessários.
