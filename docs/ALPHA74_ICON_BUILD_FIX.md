# Explorador XP 0.1.0-alpha.74

## Correção do build do novo ícone

O workflow **Gerar APK** falhava em `:app:processDebugResources` durante o link de recursos do AAPT2.

### Causa
Os arquivos `mipmap-anydpi-v26/ic_launcher*.xml` e `mipmap-anydpi-v33/ic_launcher*.xml`
referenciavam `@color/launcher_blue`, porém a declaração em `values/colors.xml` havia sido
gravada incorretamente e, portanto, o recurso não existia para o Android.

### Correção
- `launcher_blue` declarado corretamente como `#087CF0`.
- Adaptive Icons validados para API 26+ e API 33+.
- Foreground do novo ícone preservado.
- Ícones legacy e round preservados.
- Versão atualizada para `0.1.0-alpha.74` / `versionCode 74`.
