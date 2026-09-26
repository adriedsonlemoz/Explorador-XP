package com.exploradorxp.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun UsageAccessEducationScreen(
    hasUsageAccess: Boolean,
    onOpenAppInfo: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XpBackground),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(XpBlueDark)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = .15f), RoundedCornerShape(9.dp))
                    .border(1.dp, Color.White.copy(alpha = .28f), RoundedCornerShape(9.dp)),
            ) {
                CachedResourceIcon(R.drawable.file_apk, null, Modifier.size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Acesso para analisar aplicativos",
                    color = Color.White,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Configuração opcional para mostrar tamanhos completos",
                    color = Color.White.copy(alpha = .82f),
                    fontSize = 10.5.sp,
                )
            }
        }

        HorizontalDivider(color = XpChromeBorder)

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color.White)
                    .border(1.dp, XpCardBorder, RoundedCornerShape(11.dp))
                    .padding(13.dp),
            ) {
                Text(
                    "Por que o Explorador XP pede isso?",
                    color = XpBlueDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "O módulo Aplicativos instalados funciona sem esse acesso, mas mostra apenas o tamanho real dos APKs quando o Android não libera estatísticas detalhadas. Com Acesso ao uso, o sistema pode fornecer código, dados e cache de cada aplicativo.",
                    color = Color(0xFF33475B),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    "Essa autorização não dá ao Explorador XP acesso a mensagens, fotos, contas ou conteúdo privado dos outros aplicativos.",
                    color = XpTextSecondary,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (hasUsageAccess) Color(0xFFE9F7ED) else Color(0xFFFFF8E6))
                    .border(
                        1.dp,
                        if (hasUsageAccess) Color(0xFF9ECBAA) else Color(0xFFE5C468),
                        RoundedCornerShape(11.dp),
                    )
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    CachedResourceIcon(
                        if (hasUsageAccess) R.drawable.check else R.drawable.warning,
                        null,
                        Modifier.size(21.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (hasUsageAccess) "Acesso ao uso já está liberado" else "Se o Android bloquear a autorização",
                            color = if (hasUsageAccess) Color(0xFF2F6C3D) else Color(0xFF6A4C00),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            if (hasUsageAccess) {
                                "O Explorador XP já pode solicitar ao Android as estatísticas de armazenamento dos aplicativos."
                            } else {
                                "Em alguns aparelhos, principalmente quando o app foi instalado por APK, o Android pode exigir a liberação de configurações restritas antes de permitir Acesso ao uso."
                            },
                            color = if (hasUsageAccess) Color(0xFF3F6849) else Color(0xFF5D4B16),
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp,
                        )
                    }
                }

                if (!hasUsageAccess) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "1. Abra Informações do Explorador XP.\n2. Toque nos 3 pontos no canto superior direito.\n3. Escolha “Permitir configurações restritas”, se essa opção existir.\n4. Volte e abra Acesso ao uso para liberar o Explorador XP.",
                        color = Color(0xFF5D4B16),
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    XpDialogButton(
                        label = "Abrir informações do Explorador XP",
                        iconRes = R.drawable.settings,
                        onClick = onOpenAppInfo,
                    )
                    Spacer(Modifier.height(7.dp))
                    XpDialogButton(
                        label = "Abrir Acesso ao uso",
                        iconRes = R.drawable.info,
                        onClick = onOpenUsageAccess,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Os nomes e a posição dos menus podem mudar conforme a versão do Android e o fabricante.",
                        color = Color(0xFF78662F),
                        fontSize = 9.5.sp,
                        lineHeight = 13.sp,
                    )
                }
            }
        }

        HorizontalDivider(color = XpChromeBorder)
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(
                if (hasUsageAccess) "Permissão pronta" else "Você pode continuar sem conceder agora",
                color = XpTextSecondary,
                fontSize = 10.sp,
                modifier = Modifier.weight(1f),
            )
            XpDialogButton(
                label = "Continuar",
                iconRes = R.drawable.forward,
                onClick = onContinue,
            )
        }
    }
}
