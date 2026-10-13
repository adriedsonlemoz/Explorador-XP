#!/usr/bin/env python3
"""Validação estática e reprodutível da paleta clara do Explorador XP.

Não substitui a compilação Android nem inspeção visual em um aparelho.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'app/src/main/java/com/exploradorxp/app'


def luminance(color):
    parts = [int(color[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    channels = [v / 12.92 if v <= .04045 else ((v + .055) / 1.055) ** 2.4 for v in parts]
    return sum(a * b for a, b in zip(channels, (.2126, .7152, .0722)))


def contrast(color1, color2):
    low, high = sorted((luminance(color1), luminance(color2)))
    return (high + .05) / (low + .05)


def require(test, msg):
    if not test:
        raise AssertionError(msg)
    print('OK:', msg)


def file(relative):
    return (SRC / relative).read_text(encoding='utf-8')


palette = file('ExplorerScreen.kt')
colors = dict(re.findall(r'val (Xp\w+) = Color\(0xFF([A-Fa-f0-9]{6})\)', palette))
require(colors['XpBackground'] == 'FFFFFF', 'Fundo compartilhado é branco puro')
require(colors['XpSurface'] == 'FFFFFF', 'Superfícies claras são brancas')
require(colors['XpSelection'] != 'FFFFFF', 'Seleção ainda é distinguível do fundo')
require(contrast('FFFFFF', colors['XpBlueDark']) >= 4.5, 'Branco do cabeçalho azul legível')
require(contrast('FFFFFF', colors['XpBlue']) >= 4.5, 'Azul dos textos e ações no fundo branco legível')
require(contrast('FFFFFF', colors['XpTextSecondary']) >= 4.5, 'Textos secundários no fundo branco legíveis')
require(contrast('FFFFFF', colors['XpControlBorder']) >= 3, 'Borda do campo de entrada visível no branco (>=3:1)')
require(colors['XpCardBorder'] != 'FFFFFF', 'Cartões possuem contorno distinto do fundo')
require('background(XpBackground)' in file('ToolsPanelScreen.kt'), 'Painel Ferramentas com fundo compartilhado')
apps = file('InstalledAppsManager.kt')
require(apps.count('.background(XpBackground)') >= 2, 'Lista e detalhes de aplicativos com fundo branco')
require('border(1.dp, XpCardBorder, RoundedCornerShape(16.dp))' in apps, 'Cartão de aplicativo contornado')
require('private val DeviceSurface = XpBackground' in file('DeviceInfoScreen.kt'), 'Dispositivo em fundo branco')
require('background = XpBackground' in file('MainActivity.kt'), 'Paleta Material 3 é branca')
require('@android:color/white' in (ROOT / 'app/src/main/res/values/themes.xml').read_text(), 'Janela Android inicia com fundo branco')
require('background(Color.Black)' in file('VideoPlayerViewer.kt'), 'Visualizador de vídeo preserva seu fundo preto')
print('Verificação estática de cores e superfícies: APROVADA')
