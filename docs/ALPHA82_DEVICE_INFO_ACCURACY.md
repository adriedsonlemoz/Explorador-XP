# Explorador XP — alpha.82 — Informações do dispositivo

## Objetivo

A alpha.82 torna a tela **Informações do dispositivo** mais técnica sem preencher lacunas com deduções. Campos sem fonte pública confiável ficam como **Não disponível**.

## Fontes utilizadas

- Identidade do aparelho e SoC técnico: `android.os.Build`; `SOC_MANUFACTURER`/`SOC_MODEL` quando disponíveis e `HARDWARE` apenas como identificador técnico de fallback.
- ABIs do sistema: `Build.SUPPORTED_ABIS`, `SUPPORTED_32_BIT_ABIS` e `SUPPORTED_64_BIT_ABIS`.
- Bitness do processo do Explorador XP: `Process.is64Bit()`.
- Arquitetura de runtime: `os.arch`; arquitetura do kernel: `Os.uname().machine`. Nenhuma delas é renomeada para “arquitetura física” da CPU.
- Frequências: arquivos `sysfs` de `cpufreq` quando legíveis.
- GPU: `OpenGL ES GL_RENDERER`; quando indisponível, metadado do catálogo local apenas em identificador exato e com origem visível.
- RAM: `ActivityManager.MemoryInfo`.
- Armazenamento: `StatFs` do volume utilizável pelo Android. Não representa necessariamente a capacidade comercial anunciada do aparelho.
- Bateria: broadcast `ACTION_BATTERY_CHANGED` e `BatteryManager`.
- Rede: `ConnectivityManager` e `TelephonyManager`, sujeitos às permissões e limitações da versão do Android.
- Sensores: `SensorManager` e `SensorEventListener`.

## Regra para MT6765

`MT6765` é tratado como identificador técnico de uma família. A alpha.82 não o promove automaticamente para **Helio P35**, pois o identificador isolado não confirma de forma inequívoca um nome comercial. GPU/processo podem continuar aparecendo como metadados do catálogo local quando a correspondência do identificador é exata, sempre com a fonte explícita.

## Privacidade

O relatório técnico não exporta IMEI, serial, Android ID, MAC, localização, SSID/BSSID, número de telefone, nome personalizado do aparelho nem conteúdo de arquivos pessoais.

## Interface

- Cabeçalho e espaços verticais reduzidos.
- Cards compactos para RAM, armazenamento e bateria.
- Diagnóstico rápido não interpreta ausência de hardware como defeito.
- Sensores disponíveis podem ser tocados para mostrar detalhes e valores reais em tempo de execução.
- Ações disponíveis: Copiar informações, Compartilhar relatório, Salvar imagem, Compartilhar imagem e Exportar diagnóstico completo.
