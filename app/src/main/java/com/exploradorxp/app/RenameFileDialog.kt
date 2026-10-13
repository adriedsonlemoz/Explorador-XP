package com.exploradorxp.app

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/** Mesmo diálogo para o explorador principal e as galerias; extensão imutável por padrão. */
@Composable
internal fun RenameFileDialog(file: File, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val parts = remember(file.absolutePath) { FileRenameRules.parts(file.name, file.isDirectory) }
    var stem by remember(file.absolutePath) {
        mutableStateOf(TextFieldValue(parts.stem, selection = TextRange(0, parts.stem.length)))
    }
    var extension by remember(file.absolutePath) { mutableStateOf(parts.extension.removePrefix(".")) }
    var editingExtension by remember(file.absolutePath) { mutableStateOf(false) }
    val requester = remember { FocusRequester() }
    val proposed = parts.compose(stem.text, if (editingExtension) extension else parts.extension)
    val invalid = FileRenameRules.validationError(proposed)
    val duplicate = proposed != file.name && File(file.parentFile, proposed).exists()
    val canSave = invalid == null && !duplicate && proposed != file.name
    XpDialogFrame(title = if (file.isDirectory) "Renomear pasta" else "Renomear arquivo", onDismiss = onDismiss) {
        Text("Nome", color = XpTextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box(Modifier.weight(1f).border(1.dp, XpControlBorder).padding(9.dp)) {
                BasicTextField(stem, { stem = it }, singleLine = true,
                    textStyle = TextStyle(color = Color(0xFF202020), fontSize = 15.sp),
                    modifier = Modifier.fillMaxWidth().focusRequester(requester))
            }
            if (parts.extension.isNotEmpty() && !editingExtension) {
                Text(parts.extension, color = XpTextSecondary, modifier = Modifier.padding(start = 6.dp))
            }
        }
        LaunchedEffect(file.absolutePath) { requester.requestFocus() }
        if (parts.extension.isNotEmpty() && !file.isDirectory) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = editingExtension, onCheckedChange = { editingExtension = it })
                Text("Alterar extensão", fontSize = 12.sp, color = XpTextSecondary)
            }
            if (editingExtension) {
                Text("Extensão (mudar não converte o formato)", fontSize = 11.sp, color = XpTextSecondary)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().border(1.dp, XpControlBorder).padding(9.dp)) {
                    BasicTextField(extension, { extension = it.removePrefix(".") }, singleLine = true,
                        textStyle = TextStyle(color = Color(0xFF202020), fontSize = 14.sp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Resultado: $proposed", fontSize = 12.sp, color = XpTextSecondary)
        if (invalid != null || duplicate) {
            Text(invalid ?: "Já existe um item com esse nome.", fontSize = 12.sp, color = Color(0xFFB3261E))
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            XpDialogButton("Cancelar", onClick = onDismiss)
            Spacer(Modifier.width(8.dp))
            XpDialogButton("Renomear", enabled = canSave, onClick = { if (canSave) onConfirm(proposed) })
        }
    }
}
