from pathlib import Path
import json
root=Path('/mnt/data/explorador124_work')
src=root/'app/src/main/java/com/exploradorxp/app/InstalledAppsManager.kt'
s=src.read_text()
needle='import androidx.compose.material3.Icon\n'
assert needle in s
s=s.replace(needle,needle+'import androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.rounded.AccessTime\nimport androidx.compose.material.icons.rounded.Android\nimport androidx.compose.material.icons.rounded.Apps\nimport androidx.compose.material.icons.rounded.ArrowDownward\nimport androidx.compose.material.icons.rounded.ArrowUpward\nimport androidx.compose.material.icons.rounded.Person\nimport androidx.compose.material.icons.rounded.SortByAlpha\nimport androidx.compose.material.icons.rounded.Update\n')
needle='import androidx.compose.ui.graphics.Color\n'
assert needle in s
s=s.replace(needle,needle+'import androidx.compose.ui.graphics.vector.ImageVector\nimport androidx.compose.ui.semantics.semantics\nimport androidx.compose.ui.semantics.selected\nimport androidx.compose.ui.semantics.stateDescription\nimport androidx.compose.ui.semantics.Role\n')
old='''            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AppManagerChip("Todos", filter == AppManagerFilter.ALL) { filter = AppManagerFilter.ALL }
                AppManagerChip("Usuário", filter == AppManagerFilter.USER) { filter = AppManagerFilter.USER }
                AppManagerChip("Sistema", filter == AppManagerFilter.SYSTEM) { filter = AppManagerFilter.SYSTEM }
                AppManagerChip("Nome", sort == AppManagerSort.NAME) { sort = AppManagerSort.NAME }
                AppManagerChip("Maiores ↓", sort == AppManagerSort.SIZE) { sort = AppManagerSort.SIZE }
                AppManagerChip("Menores ↑", sort == AppManagerSort.SIZE_ASC) { sort = AppManagerSort.SIZE_ASC }
                AppManagerChip("Instalados", sort == AppManagerSort.INSTALL_DATE) { sort = AppManagerSort.INSTALL_DATE }
                AppManagerChip("Atualizados", sort == AppManagerSort.LAST_UPDATE) { sort = AppManagerSort.LAST_UPDATE }
            }
'''
new='''            // Os oito atalhos permanecem em uma única faixa rolável, mesmo com fontes maiores.
            // Os ícones substituem as setas textuais e ajudam a identificar a classificação.
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppManagerChip("Todos", Icons.Rounded.Apps, filter == AppManagerFilter.ALL) { filter = AppManagerFilter.ALL }
                AppManagerChip("Usuário", Icons.Rounded.Person, filter == AppManagerFilter.USER) { filter = AppManagerFilter.USER }
                AppManagerChip("Sistema", Icons.Rounded.Android, filter == AppManagerFilter.SYSTEM) { filter = AppManagerFilter.SYSTEM }
                AppManagerChip("Nome", Icons.Rounded.SortByAlpha, sort == AppManagerSort.NAME) { sort = AppManagerSort.NAME }
                AppManagerChip("Maiores", Icons.Rounded.ArrowDownward, sort == AppManagerSort.SIZE) { sort = AppManagerSort.SIZE }
                AppManagerChip("Menores", Icons.Rounded.ArrowUpward, sort == AppManagerSort.SIZE_ASC) { sort = AppManagerSort.SIZE_ASC }
                AppManagerChip("Instalados", Icons.Rounded.AccessTime, sort == AppManagerSort.INSTALL_DATE) { sort = AppManagerSort.INSTALL_DATE }
                AppManagerChip("Atualizados", Icons.Rounded.Update, sort == AppManagerSort.LAST_UPDATE) { sort = AppManagerSort.LAST_UPDATE }
            }
'''
assert old in s, 'chip row source drift'
s=s.replace(old,new)
old='''@Composable
private fun AppManagerChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Color(0xFFDDEEFF) else Color.White)
            .border(1.dp, if (selected) XpBlue else XpControlBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
    ) {
        Text(label, fontSize = 10.5.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = Color(0xFF27435F))
    }
}
'''
new='''@Composable
private fun AppManagerChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(shape)
            .background(if (selected) XpSelection else Color.White)
            .border(1.dp, if (selected) XpBlue else XpControlBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                this.selected = selected
                stateDescription = if (selected) "Selecionado" else "Não selecionado"
            }
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Rótulo do chip já identifica a ação.
            tint = if (selected) XpBlueDark else XpTextSecondary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            fontSize = 10.3.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = Color(0xFF27435F),
            maxLines = 1,
        )
    }
}
'''
assert old in s, 'AppManagerChip source drift'
s=s.replace(old,new)
src.write_text(s)

# Version and metadata
version='0.1.0-alpha.124'
for name in ('VERSION','app/build.gradle.kts','app_identity.json','github-manager.json','README.md','RELEASE.md','CHANGELOG.md','docs/VALIDATION.json'):
    p=root/name
    text=p.read_text()
    text=text.replace('0.1.0-alpha.123',version)
    if name in ('app/build.gradle.kts','app_identity.json','github-manager.json','README.md','RELEASE.md'):
        text=text.replace('versionCode = 123','versionCode = 124').replace('versionCode 123','versionCode 124').replace('"versionCode": 123','"versionCode": 124')
    if name=='docs/VALIDATION.json':
        obj=json.loads(text)
        obj['version_code']=124
        obj['notes']=[
            'Filtros de Aplicativos instalados em uma linha com chips compactos e ícones distintos.',
            'Seleção e acessibilidade dos filtros preservadas.',
            'Atalho Início na barra inferior com sete destinos preservado.'
        ]
        text=json.dumps(obj,ensure_ascii=False,indent=2)+'\n'
    p.write_text(text)

p=root/'README.md'
s=p.read_text()
s=s.replace('Filtros de aplicativos em linha única e botão Início na barra inferior para voltar à pasta principal do armazenamento.', 'Chips compactos com ícones nos filtros de aplicativos, em linha única com rolagem horizontal. Botão Início na barra inferior preservado.')
p.write_text(s)

p=root/'CHANGELOG.md'
s=p.read_text()
s='''## 0.1.0-alpha.124 (2026-10-14)

- Redesenha os filtros rápidos de Aplicativos instalados como chips compactos com ícones, todos em uma linha rolável.
- Diferencia visualmente Todos, Usuário, Sistema, Nome, Maiores, Menores, Instalados e Atualizados; conserva filtro de tipo e ordenação simultâneos.
- Mantém realce dos filtros ativos e descrição de acessibilidade para estado selecionado.
- Preserva o botão Início central da barra inferior com sete atalhos e as demais funcionalidades da alpha.123.

'''+s
p.write_text(s)

p=root/'RELEASE.md'
p.write_text('''# Explorador XP 0.1.0-alpha.124

**Versão:** `0.1.0-alpha.124`  
**versionCode:** `124`

## Destaques

- Filtros rápidos de Aplicativos instalados em chips menores com ícones: Todos, Usuário, Sistema, Nome, Maiores, Menores, Instalados e Atualizados.
- Uma única faixa horizontal rolável, com realce do filtro de tipo e do critério de ordem ativos.
- Barra inferior de sete atalhos com Início no centro preservada.
- Sem mudança na lógica dos filtros, na consulta de tamanho, nas permissões ou nas operações de arquivos.

## Validação

- Verificações locais de estrutura, metadados e XML/JSON; build Android completo e testes visuais no aparelho ainda pendentes.
''')

# News screen currently still shows old alpha122 notes in alpha123; synchronize now.
p=root/'app/src/main/java/com/exploradorxp/app/ReleaseNotes.kt'
s=p.read_text()
a=s.index('            changes = listOf(')
b=s.index('            )',a)+len('            )')
s=s[:a]+'''            changes = listOf(
                "Filtros rápidos de aplicativos redesenhados em chips compactos com ícones, numa única linha rolável.",
                "Identificação visual de Todos, Usuário, Sistema e das classificações por nome, tamanho e datas.",
                "Seleção dos filtros mantida, com estados acessíveis e botão Início da barra inferior preservado.",
            )'''+s[b:]
p.write_text(s)

p=root/'app/src/test/java/com/exploradorxp/app/ReleaseNotesScopeTest.kt'
s=p.read_text()
s=s.replace('assertTrue(info.changes.any { it.contains("abas") && it.contains("OBB") })','assertTrue(info.changes.any { it.contains("chips") && it.contains("ícones") })')
s=s.replace('assertTrue(info.changes.any { it.contains("aplicativos") && it.contains("parciais") })','assertTrue(info.changes.any { it.contains("Usuário") && it.contains("Sistema") })')
s=s.replace('assertTrue(info.changes.any { it.contains("ZIP") && it.contains("GitHub Manager") })','assertTrue(info.changes.any { it.contains("Início") && it.contains("acessíveis") })')
p.write_text(s)

# Static regression validation of UI contract
p=root/'tools/validate_alpha124.py'
p.write_text('''#!/usr/bin/env python3
"""Small structural regression check. Does not replace Android build or UI tests."""
import json
from pathlib import Path
root = Path(__file__).resolve().parents[1]
assert (root/'VERSION').read_text().strip() == '0.1.0-alpha.124'
gradle = (root/'app/build.gradle.kts').read_text()
assert 'versionCode = 124' in gradle and 'versionName = "0.1.0-alpha.124"' in gradle
for meta in ('github-manager.json', 'app_identity.json'):
    data = json.loads((root/meta).read_text())
    assert data.get('versionCode') == 124 and data.get('versionName', data.get('version')) == '0.1.0-alpha.124'
src = (root/'app/src/main/java/com/exploradorxp/app/InstalledAppsManager.kt').read_text()
assert src.count('AppManagerChip("') == 8
for name in ('Todos','Usuário','Sistema','Nome','Maiores','Menores','Instalados','Atualizados'):
    assert f'AppManagerChip("{name}"' in src
assert src.count('horizontalScroll(rememberScrollState())') == 1
assert 'horizontalArrangement = Arrangement.spacedBy(4.dp)' in src
assert 'imageVector = icon' in src and 'stateDescription = if (selected)' in src
assert 'Modifier.size(14.dp)' in src
nav=(root/'app/src/main/java/com/exploradorxp/app/ExplorerQuickAccess.kt').read_text()
assert 'ExplorerQuickDestination.HOME, "Início"' in nav
screen=(root/'app/src/main/java/com/exploradorxp/app/ExplorerScreen.kt').read_text()
assert 'ExplorerQuickDestination.HOME -> {' in screen
assert 'onTabChange(ExplorerTab.FILES)' in screen and 'onHome()' in screen
news=(root/'app/src/main/java/com/exploradorxp/app/ReleaseNotes.kt').read_text()
assert 'chips compactos com ícones' in news
print('OK: alpha.124 chips, navigation, version metadata, release notes')
''')
print('Patched alpha124')
