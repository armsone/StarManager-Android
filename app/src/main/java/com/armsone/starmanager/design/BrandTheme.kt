package com.armsone.starmanager.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** iOS BrandTheme.swift — 색상은 sRGB 성분값을 그대로 옮겼다. 광택 딥레드 액센트 + 따뜻한 오프화이트 캔버스. */
object BrandTheme {
    val accent = Color(228, 30, 37)
    val accentSoft = Color(0.965f, 0.90f, 0.90f)
    val warm = Color(0.65f, 0.27f, 0.12f)
    val paper = Color(0.97f, 0.94f, 0.88f)
    val canvas = Color(0.955f, 0.945f, 0.925f)
    val surface = Color(0.985f, 0.975f, 0.96f)
    val resultSurface = Color.White
    val ink = Color(0.075f, 0.065f, 0.06f)
    val chrome = Color(0.58f, 0.56f, 0.53f)
    val border = Color(0.55f, 0.52f, 0.47f).copy(alpha = 0.34f)

    /** SwiftUI 시맨틱 컬러 근사치 — ink 기반이며 순검정을 쓰지 않는다. */
    val labelPrimary = ink
    val labelSecondary = ink.copy(alpha = 0.6f)
    val orange = Color(0xFFFF9500)
    val red = Color(0xFFFF3B30)
    val green = Color(0xFF34C759)

    val glossyBlack = Brush.verticalGradient(
        0f to Color(0.23f, 0.22f, 0.23f),
        0.48f to Color(0.09f, 0.08f, 0.085f),
        1f to Color(0.035f, 0.03f, 0.035f)
    )

    val canvasGradient = Brush.linearGradient(
        listOf(
            Color(0.98f, 0.97f, 0.95f),
            canvas,
            Color(0.92f, 0.90f, 0.87f)
        )
    )

    val cardBorderGradient = Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.9f), chrome.copy(alpha = 0.5f), border)
    )
}

/** iOS starCard(): padding 18, surface 배경, radius 20, 크롬 보더, 은은한 깊이감. */
fun Modifier.starCard(): Modifier {
    val shape = RoundedCornerShape(20.dp)
    return this
        .shadow(7.dp, shape, ambientColor = BrandTheme.ink.copy(alpha = 0.07f), spotColor = BrandTheme.ink.copy(alpha = 0.07f))
        .background(BrandTheme.surface, shape)
        .border(BorderStroke(1.dp, BrandTheme.cardBorderGradient), shape)
        .padding(18.dp)
}
