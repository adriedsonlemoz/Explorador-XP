#!/usr/bin/env python3
"""Verificações estáticas conservadoras das telas alteradas na alpha.116.

Não substitui compilação Android nem testes instrumentados de interface.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1] / 'app/src/main/java/com/exploradorxp/app'
archive = (root / 'ArchiveViewer.kt').read_text()
viewer = (root / 'InternalViewer.kt').read_text()
trash = (root / 'ExplorerScreen.kt').read_text()
vm = (root / 'ExplorerViewModel.kt').read_text()
device = (root / 'DeviceInfoScreen.kt').read_text()
soc = (root / 'DeviceSoCResolver.kt').read_text()

requirements = {
    'ZIP sem barra superior redundante': 'if (extension != "zip") ViewerToolbar' in viewer,
    'ZIP única opção Abrir com no menu': archive.count('ArchiveMenuText("Abrir com outro aplicativo")') == 1,
    'ZIP ações principais': all(x in archive for x in ['"Extrair" else "Extrair', '"Verificar", R.drawable.check', '"Info", R.drawable.info']),
    'ZIP detalhes opcionais': 'if (technicalExpanded)' in archive,
    'ZIP pesquisa opcional': 'if (showSearch || query.isNotBlank())' in archive,
    'Lixeira exibe linhas compactas': 'private fun CompactTrashRow' in trash,
    'Lixeira sem botão grande de exclusão por item': 'private fun TrashItemRow' not in trash,
    'Lixeira filtro por tipo': 'TrashListFilter.FOLDERS' in trash and 'TrashListFilter.FILES' in trash,
    'Lixeira seleção múltipla': 'selectedIds' in trash and 'onRestoreSelected(selection)' in trash,
    'Lixeira exclusão ainda confirmada': 'title = "Apagar itens selecionados?"' in trash and 'confirmText = "Apagar"' in trash,
    'Restauração em lote única operação': 'repository.restoreTrash(items, onProgress' in vm,
    'Exclusão em lote única operação': 'repository.permanentlyDeleteTrash(items, onProgress' in vm,
    'Cópia por toque limitada a campos permitidos': 'label in safeCopyFields' in device,
    'Clipboard Android': 'setPrimaryClip(ClipData.newPlainText(label, value))' in device,
    'SoC identificado por alias exato': 'aliases("MT6877V/TZA")' in soc,
}
failures = [name for name, passed in requirements.items() if not passed]
for name, passed in requirements.items():
    print(f'{"OK" if passed else "ERRO"}: {name}')
if failures:
    raise SystemExit('Verificações falharam: ' + ', '.join(failures))
print(f'{len(requirements)} verificações estáticas da alpha.116 aprovadas')
