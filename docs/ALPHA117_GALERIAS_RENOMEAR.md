# Alpha.117: galerias e renomeação

As galerias indexadas por MediaStore mantêm uma raiz **virtual** de pastas. Ações de criação/colar só estão habilitadas quando uma pasta real está selecionada. Copiar e Cortar armazenam os arquivos no mesmo clipboard do gerenciador; Colar envia o diretório **explícito** ao repositório, respeitando conflitos, pausas e permissões. A cópia pela antiga janela SAF continua disponível no compartilhamento ou pode ser reavaliada em versões futuras.

A barra de comandos (Voltar, Avançar, Início, Subir, Pesquisar, Novo, Atualizar, Favoritos/Colar), a barra de menus e os menus Ferramentas/Ajuda são componentes compartilhados. As operações exclusivas de mídia (grade, agrupamento por pasta e filtros Câmera/Capturas/Downloads) continuam no menu Exibir das galerias. A pesquisa dentro da pasta é feita somente nos itens já carregados; o próprio aplicativo avisa quando há páginas restantes.

Renomear utiliza FileRenameRules e RenameFileDialog tanto em Arquivos quanto em Fotos/Vídeos. O campo do nome é pré-selecionado e a extensão só muda após seleção explícita da opção correspondente. A alteração de extensão não converte formatos.

Limites pendentes: paginação e exclusões/conclusões exigem recarregar a galeria quando uma transferência termina; pastas vazias não são listadas pelo MediaStore; operações sobre arquivos localizados em áreas protegidas dependem das permissões Android. A interface não promete acesso a armazenamento restrito.
