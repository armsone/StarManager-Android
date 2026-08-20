package com.armsone.starmanager.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** iOS BrandTheme.swift — 색상은 sRGB 성분값을 그대로 옮겼다. */
object BrandTheme {
    val accent = Color(0.42f, 0.33f, 0.67f)
    val accentSoft = Color(0.93f, 0.91f, 0.98f)
    val warm = Color(0.84f, 0.36f, 0.18f)
    val paper = Color(0.99f, 0.95f, 0.86f)
    val canvas = Color(0.98f, 0.97f, 0.94f)
    val surface = Color.White.copy(alpha = 0.96f)
    val border = Color(0.81f, 0.78f, 0.72f).copy(alpha = 0.42f)

    /** SwiftUI 시맨틱 컬러 근사치 (라이트 모드 고정) */
    val labelPrimary = Color(0xFF000000)
    val labelSecondary = Color(0x993C3C43)
    val orange = Color(0xFFFF9500)
    val red = Color(0xFFFF3B30)
    val green = Color(0xFF34C759)
}

/** iOS starCard(): padding 18, surface 배경, radius 20, border 1. */
fun Modifier.starCard(): Modifier {
    val shape = RoundedCornerShape(20.dp)
    return this
        .background(BrandTheme.surface, shape)
        .border(1.dp, BrandTheme.border, shape)
        .padding(18.dp)
}
