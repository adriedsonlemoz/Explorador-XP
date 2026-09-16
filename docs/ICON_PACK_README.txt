EXPLORADOR XP PARA ANDROID — PACOTE DE ÍCONES v2

Pacote de assets para o mockup final do gerenciador de arquivos Android com identidade visual inspirada no Windows XP.

Estrutura:
- actions/      navegação e operações
- devices/      armazenamento e dispositivos
- file_types/   formatos conhecidos de arquivos
- folders/      pastas e variações
- locations/    atalhos e locais
- misc/         tipos/símbolos auxiliares
- status/       estados
- view/         modos de visualização
- mockup/       referência da interface final
- reference/    catálogo visual dos ícones

Os PNGs individuais existentes no pacote continuam preservando a identidade visual original.
Desde a alpha.23, os 150 ícones comuns usados pela interface ficam em `res/drawable-xxxhdpi/` com 192×192, para que o Android aplique density scaling e reduza custo de decodificação/memória. O Compose continua controlando o tamanho final em dp.

`drawable-nodpi` fica reservado ao launcher e às variantes grandes usadas em telas específicas (`file_apk_large`, `folder_open_large`, `search_large`).
