package com.armsone.starmanager.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.armsone.starmanager.model.AppAppearance

val LocalAppAppearance = staticCompositionLocalOf { AppAppearance.BK }

/**
 * BK Style 및 Classic 테마 토큰 및 중앙화된 디자인 레이어.
 *
 * BK Style 원칙:
 * - Pure white enamel: 가장 강하고 명확한 표면 (카드, 결과창 등 핵심 컨테이너)
 * - Carbon: 다크 테크니컬 구조 (버튼, 아이콘 웰, 탑바/탭바 구조, 칩)
 * - Warm dark oxblood leather: 개인 취향/터치 영역 (나의 취향 섹션, 선호 컨트롤)
 * - Chrome: 5% 미만의 헤어라인 정밀 엣지 및 콤팩트 메커니즘
 * - Signature red (#E41E25): 2% 미만, 구조적 엣지/선택 스트로크/언더라인으로만 통합
 *
 * Classic:
 * - 기존 visual theme 전체를 100% 보존
 */
object BrandTheme {
    // 공통 및 클래식 기본 토큰
    val accent = Color(228, 30, 37) // #E41E25 Signature Red
    val accentSoft = Color(0.965f, 0.90f, 0.90f)
    val warm = Color(0.65f, 0.27f, 0.12f)
    val paper = Color(0.97f, 0.94f, 0.88f)
    val canvas = Color(0.955f, 0.945f, 0.925f)
    val surface = Color(0.985f, 0.975f, 0.96f)
    val resultSurface = Color.White
    val ink = Color(0.075f, 0.065f, 0.06f)
    val chrome = Color(0.58f, 0.56f, 0.53f)
    val border = Color(0.55f, 0.52f, 0.47f).copy(alpha = 0.34f)

    val labelPrimary = ink
    val labelSecondary = ink.copy(alpha = 0.6f)
    val orange = Color(0xFFFF9500)
    val red = Color(0xFFFF3B30)
    val green = Color(0xFF34C759)

    // BK Style 특화 토큰
    val bkEnamelWhite = Color(0xFFFFFFFF)
    val bkCarbonDark = Color(0xFF141518)
    val bkCarbonSurface = Color(0xFF1B1C20)
    val bkCarbonElevated = Color(0xFF26272C)
    val bkCanvas = Color(0xFFF1F3F6)
    val bkOxblood = Color(0xFF3E1219)
    val bkOxbloodSurface = Color(0xFFFAF5F6)
    val bkOxbloodBorder = Color(0xFFE8D5D8)
    val bkChromeHairline = Color(0xFFD3D7DE)
    val bkLabelPrimary = Color(0xFF141518)
    val bkLabelSecondary = Color(0xFF6B6E76)
    val bkDivider = Color(0x1F141518)

    val glossyBlack = Brush.verticalGradient(
        0f to Color(0.23f, 0.22f, 0.23f),
        0.48f to Color(0.09f, 0.08f, 0.085f),
        1f to Color(0.035f, 0.03f, 0.035f)
    )

    val bkGlossyCarbon = Brush.verticalGradient(
        0f to Color(0xFF25262B),
        0.48f to Color(0xFF16171A),
        1f to Color(0xFF0C0D0F)
    )

    val canvasGradient = Brush.linearGradient(
        listOf(
            Color(0.98f, 0.97f, 0.95f),
            canvas,
            Color(0.92f, 0.90f, 0.87f)
        )
    )

    val bkCanvasGradient = Brush.verticalGradient(
        listOf(
            Color(0xFFF7F8FA),
            Color(0xFFF0F2F5),
            Color(0xFFE8EBF0)
        )
    )

    val cardBorderGradient = Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.9f), chrome.copy(alpha = 0.5f), border)
    )

    val bkCardBorderGradient = Brush.linearGradient(
        listOf(Color(0xFFE5E7EB), Color(0xFFCBD2DC), Color(0xFFE5E7EB))
    )

    // 동적 테마 헬퍼
    fun canvas(appearance: AppAppearance): Color =
        if (appearance == AppAppearance.BK) bkCanvas else canvas

    fun canvasBrush(appearance: AppAppearance): Brush =
        if (appearance == AppAppearance.BK) bkCanvasGradient else canvasGradient

    fun surface(appearance: AppAppearance): Color =
        if (appearance == AppAppearance.BK) bkEnamelWhite else surface

    fun labelPrimary(appearance: AppAppearance): Color =
        if (appearance == AppAppearance.BK) bkLabelPrimary else labelPrimary

    fun labelSecondary(appearance: AppAppearance): Color =
        if (appearance == AppAppearance.BK) bkLabelSecondary else labelSecondary

    fun divider(appearance: AppAppearance): Color =
        if (appearance == AppAppearance.BK) bkDivider else Color(0x293C3C43)

    fun settingsBackground(appearance: AppAppearance): Color =
        if (appearance == AppAppearance.BK) bkCanvas else Color(0xFFF2F2F7)

    fun settingsSectionBackground(appearance: AppAppearance): Color =
        if (appearance == AppAppearance.BK) bkEnamelWhite else Color.White
}

/** 카드 컨테이너 Modifier */
fun Modifier.starCard(appearance: AppAppearance = AppAppearance.BK): Modifier {
    return if (appearance == AppAppearance.BK) {
        val shape = RoundedCornerShape(18.dp)
        this
            .shadow(6.dp, shape, ambientColor = Color(0x0A141518), spotColor = Color(0x12141518))
            .background(BrandTheme.bkEnamelWhite, shape)
            .border(BorderStroke(1.dp, BrandTheme.bkCardBorderGradient), shape)
            .padding(18.dp)
    } else {
        val shape = RoundedCornerShape(20.dp)
        this
            .shadow(7.dp, shape, ambientColor = BrandTheme.ink.copy(alpha = 0.07f), spotColor = BrandTheme.ink.copy(alpha = 0.07f))
            .background(BrandTheme.surface, shape)
            .border(BorderStroke(1.dp, BrandTheme.cardBorderGradient), shape)
            .padding(18.dp)
    }
}

/** 취향/선호 설정 전용 옥스블러드 카드 Modifier */
fun Modifier.oxbloodPreferenceCard(appearance: AppAppearance = AppAppearance.BK): Modifier {
    return if (appearance == AppAppearance.BK) {
        val shape = RoundedCornerShape(16.dp)
        this
            .shadow(3.dp, shape, ambientColor = Color(0x0A3E1219), spotColor = Color(0x103E1219))
            .background(BrandTheme.bkOxbloodSurface, shape)
            .border(BorderStroke(1.dp, BrandTheme.bkOxbloodBorder), shape)
            .padding(16.dp)
    } else {
        val shape = RoundedCornerShape(14.dp)
        this
            .background(BrandTheme.surface, shape)
            .border(BorderStroke(1.dp, BrandTheme.border), shape)
            .padding(14.dp)
    }
}

enum class IconWellVariant {
    CARBON,
    OXBLOOD,
    ENAMEL,
    ACCENT
}

/**
 * 통일된 아이콘 웰 시스템.
 * 모든 실제 페이지 및 섹션 제목 행에 선행 아이콘으로 배치된다.
 */
@Composable
fun IconWell(
    icon: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    appearance: AppAppearance = LocalAppAppearance.current,
    variant: IconWellVariant = IconWellVariant.CARBON,
    size: Dp = 28.dp,
    iconSize: Dp = 16.dp,
    tint: Color? = null
) {
    val isBk = appearance == AppAppearance.BK
    if (!isBk) {
        // Classic: 기존 플레인 시스템 아이콘 표현 (웰 박스, 테두리, 그림자 없음)
        val defaultTint = when (variant) {
            IconWellVariant.OXBLOOD -> BrandTheme.warm
            else -> BrandTheme.accent
        }
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint ?: defaultTint,
                modifier = Modifier.size(iconSize)
            )
        }
    } else {
        // BK Style: 정밀 카본/옥스블러드/에나멜 웰 및 헤어라인 크롬 테두리
        val shape = RoundedCornerShape(8.dp)
        val background = when (variant) {
            IconWellVariant.CARBON -> BrandTheme.bkCarbonSurface
            IconWellVariant.OXBLOOD -> BrandTheme.bkOxblood
            IconWellVariant.ENAMEL -> BrandTheme.bkEnamelWhite
            IconWellVariant.ACCENT -> BrandTheme.bkCarbonSurface
        }
        val defaultTint = when (variant) {
            IconWellVariant.CARBON -> Color.White
            IconWellVariant.OXBLOOD -> Color(0xFFF7ECEE)
            IconWellVariant.ENAMEL -> BrandTheme.bkCarbonDark
            IconWellVariant.ACCENT -> BrandTheme.accent
        }
        val borderStroke = when (variant) {
            IconWellVariant.CARBON -> BorderStroke(0.7.dp, Color(0x33FFFFFF))
            IconWellVariant.OXBLOOD -> BorderStroke(0.7.dp, Color(0x44FFFFFF))
            IconWellVariant.ENAMEL -> BorderStroke(0.8.dp, BrandTheme.bkChromeHairline)
            IconWellVariant.ACCENT -> BorderStroke(1.dp, BrandTheme.accent.copy(alpha = 0.6f))
        }

        Box(
            modifier = modifier
                .size(size)
                .shadow(2.dp, shape)
                .background(background, shape)
                .border(borderStroke, shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint ?: defaultTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
