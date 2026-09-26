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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UpdateHighlightsScreen(
    release: ReleaseInfo = ReleaseNotes.current,
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
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF2F92F6), XpBlue, XpBlueDark),
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White.copy(alpha = .15f), RoundedCornerShape(5.dp))
                    .border(1.dp, Color.White.copy(alpha = .34f), RoundedCornerShape(5.dp)),
            ) {
                Image(
                    painter = painterResource(R.drawable.file_new),
                    contentDescription = null,
                    modifier = Modifier.size(27.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Novidades da atualização",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
                Text(
                    text = release.versionName,
                    color = Color.White.copy(alpha = .88f),
                    fontSize = 11.5.sp,
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
                .padding(horizontal = 14.dp, vertical = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, XpCardBorder, RoundedCornerShape(10.dp))
                    .padding(14.dp),
            ) {
                Text(
                    text = release.title,
                    color = XpBlueDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "Veja o que mudou nesta versão. Esta tela é exibida somente na primeira abertura após cada atualização.",
                    color = XpTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(XpPanel)
                    .border(1.dp, XpCardBorder, RoundedCornerShape(10.dp))
                    .padding(13.dp),
            ) {
                Text(
                    text = "O que mudou",
                    color = XpBlueDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                release.changes.forEach { change ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(6.dp)
                                .background(XpBlue, RoundedCornerShape(50)),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = change,
                            color = Color(0xFF303030),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = XpChromeBorder)
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(XpChrome)
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(
                text = "Versão ${release.versionName}",
                color = XpTextSecondary,
                fontSize = 10.5.sp,
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
