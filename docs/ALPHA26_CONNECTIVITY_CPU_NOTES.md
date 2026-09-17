# Alpha 26 — Conectividade, CPU e seções

## Conectividade

A tela de Informações do dispositivo ganhou leitura local de conexão ativa, Wi‑Fi, rede móvel, SIM/eSIM, Bluetooth, Ethernet e VPN. Dados de Wi‑Fi incluem apenas informações técnicas permitidas (banda, padrão, frequência e velocidade do link quando expostas); SSID/BSSID/MAC não são coletados.

## Processador

Além do fabricante/modelo do SoC, o painel mostra número de núcleos, 32/64 bits, ABI principal, hardware e resumo das frequências máximas que o kernel expõe por núcleo. Quando sysfs ou o fabricante bloquear esses valores, a interface mostra “Não disponível”.

## Navegação

As setas dos cards de seção deixaram de ser decorativas. Sistema, Conectividade, Bateria, Recursos e Sensores agora podem ser expandidos/recolhidos tocando no cabeçalho. Sensores foram compactados para três colunas; nomes longos podem usar duas linhas para evitar truncamento excessivo.

## Privacidade

Nenhuma coleta de IMEI, IMSI, ICCID, número de telefone, Android ID, serial, SSID, BSSID, MAC ou localização foi adicionada.
