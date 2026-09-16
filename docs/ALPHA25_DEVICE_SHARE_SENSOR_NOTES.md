# Alpha 25 — Sensores e compartilhamento

## Rodapé
A área rolável agora aplica padding da barra de navegação e um espaço final extra, evitando que o botão de exportação fique atrás dos controles do Android.

## Sensores
A disponibilidade é consultada pelo `SensorManager`, sem inferir sensores pelo modelo do aparelho. São verificados acelerômetro, giroscópio, magnetômetro, luz, proximidade, pressão, passos, gravidade, aceleração linear, rotação, temperatura ambiente e umidade.

## Compartilhamento
- **Copiar resumo:** texto curto e legível no clipboard.
- **Salvar PNG:** gera uma ficha visual local via Canvas e salva pelo seletor do Android.
- **Compartilhar imagem:** gera a mesma ficha no cache e compartilha por `FileProvider`.
- **Relatório para IA:** permanece separado e mais técnico, agora com `schema_version=2` e seção `[sensors]`.

Nenhuma dessas saídas inclui IMEI, serial, Android ID, MAC, localização ou lista/conteúdo de arquivos pessoais.
