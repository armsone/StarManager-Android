package com.armsone.imanagerai.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armsone.imanagerai.model.AppAppearance

/**
 * UISegmentedControl 대응.
 * BK Style: 정밀 헤어라인 크롬 엣지와 에나멜 화이트 세그먼트.
 * Classic: 오리지널 iOS 형태 유지.
 */
@Composable
fun StarSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    appearance: AppAppearance = LocalAppAppearance.current
) {
    val isBk = appearance == AppAppearance.BK
    val isInterstellar = appearance == AppAppearance.INTERSTELLAR
    val containerShape = RoundedCornerShape(9.dp)
    val segmentShape = RoundedCornerShape(7.dp)

    val containerBackground = when (appearance) {
        AppAppearance.INTERSTELLAR -> Color(0xFF24262E)
        AppAppearance.BK -> Color(0xFFE8EBF0)
        AppAppearance.CLASSIC -> Color(0x1F767680)
    }
    val containerBorder = when (appearance) {
        AppAppearance.INTERSTELLAR -> BorderStroke(0.8.dp, Color(0xFF3A3E4B))
        AppAppearance.BK -> BorderStroke(0.6.dp, BrandTheme.bkChromeHairline)
        AppAppearance.CLASSIC -> null
    }

    val fontSize = when {
        options.size >= 5 -> 12.sp
        options.size == 4 -> 12.5.sp
        else -> 13.sp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .then(if (containerBorder != null) Modifier.border(containerBorder, containerShape) else Modifier)
            .background(containerBackground, containerShape)
            .padding(2.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val segmentModifier = if (selected) {
                when (appearance) {
                    AppAppearance.INTERSTELLAR -> {
                        Modifier
                            .shadow(2.dp, segmentShape)
                            .background(Color(0xFF13151A), segmentShape)
                            .border(BorderStroke(0.8.dp, BrandTheme.interstellarAccent.copy(alpha = 0.6f)), segmentShape)
                    }
                    AppAppearance.BK -> {
                        Modifier
                            .shadow(2.dp, segmentShape)
                            .background(Color.White, segmentShape)
                            .border(BorderStroke(0.5.dp, Color(0xFFD6DAE0)), segmentShape)
                    }
                    AppAppearance.CLASSIC -> {
                        Modifier
                            .shadow(2.dp, segmentShape)
                            .background(Color.White, segmentShape)
                    }
                }
            } else Modifier

            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(segmentModifier)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(index) }
                    .semantics {
                        contentDescription = option
                        this.selected = selected
                    }
                    .testTag("segment.$option"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    fontSize = fontSize,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) {
                        when (appearance) {
                            AppAppearance.INTERSTELLAR -> BrandTheme.interstellarAccent
                            AppAppearance.BK -> BrandTheme.bkLabelPrimary
                            AppAppearance.CLASSIC -> BrandTheme.labelPrimary
                        }
                    } else {
                        when (appearance) {
                            AppAppearance.INTERSTELLAR -> BrandTheme.interstellarLabelSecondary
                            AppAppearance.BK -> BrandTheme.bkLabelSecondary
                            AppAppearance.CLASSIC -> BrandTheme.labelSecondary
                        }
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * 4~5개 이상의 선택지일 때 좁은 화면에서 찌그러지지 않도록 2줄로 나누어 표시하는 적응형 세그먼트 컨트롤.
 */
@Composable
fun StarAdaptiveSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    appearance: AppAppearance = LocalAppAppearance.current
) {
    if (options.size <= 3) {
        StarSegmentedControl(
            options = options,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            modifier = modifier,
            appearance = appearance
        )
    } else {
        val firstRowCount = if (options.size == 4) 2 else 3
        val firstRowOptions = options.take(firstRowCount)
        val secondRowOptions = options.drop(firstRowCount)

        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StarSegmentedControl(
                options = firstRowOptions,
                selectedIndex = if (selectedIndex in 0 until firstRowCount) selectedIndex else -1,
                onSelect = { onSelect(it) },
                appearance = appearance
            )
            StarSegmentedControl(
                options = secondRowOptions,
                selectedIndex = if (selectedIndex >= firstRowCount) selectedIndex - firstRowCount else -1,
                onSelect = { onSelect(firstRowCount + it) },
                appearance = appearance
            )
        }
    }
}

/** UISlider 대응 — 에나멜 화이트 원형 썸, 좁은 트랙. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    modifier: Modifier = Modifier,
    tint: Color = BrandTheme.accent,
    appearance: AppAppearance = LocalAppAppearance.current
) {
    val isBk = appearance == AppAppearance.BK
    val stepsCount = (((valueRange.endInclusive - valueRange.start) / step).toInt() - 1)
        .coerceAtLeast(0)
    val inactiveColor = if (isBk) Color(0xFFD9DDE4) else Color(0x2E787880)
    val sliderColors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = tint,
        inactiveTrackColor = inactiveColor,
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent
    )

    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = stepsCount,
        modifier = modifier.height(28.dp),
        colors = sliderColors,
        thumb = {
            val thumbModifier = if (isBk) {
                Modifier
                    .size(26.dp)
                    .shadow(3.dp, CircleShape)
                    .border(BorderStroke(0.8.dp, BrandTheme.bkChromeHairline), CircleShape)
                    .background(Color.White, CircleShape)
            } else {
                Modifier
                    .size(26.dp)
                    .shadow(3.dp, CircleShape)
                    .background(Color.White, CircleShape)
            }
            Box(thumbModifier)
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                colors = sliderColors,
                drawStopIndicator = null
            )
        }
    )
}

/** 토글 스위치 대응. */
@Composable
fun StarSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    appearance: AppAppearance = LocalAppAppearance.current
) {
    val trackColor = if (checked) {
        when (appearance) {
            AppAppearance.INTERSTELLAR -> BrandTheme.interstellarAccent
            else -> BrandTheme.accent
        }
    } else {
        when (appearance) {
            AppAppearance.INTERSTELLAR -> Color(0xFF2E313B)
            AppAppearance.BK -> Color(0xFFD0D5DD)
            AppAppearance.CLASSIC -> Color(0x2E787880)
        }
    }

    Box(
        modifier = modifier
            .width(51.dp)
            .height(31.dp)
            .background(trackColor, RoundedCornerShape(15.5.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        val thumbModifier = when (appearance) {
            AppAppearance.INTERSTELLAR -> {
                Modifier
                    .size(27.dp)
                    .shadow(2.dp, CircleShape)
                    .border(BorderStroke(0.6.dp, BrandTheme.interstellarPlatinum), CircleShape)
                    .background(Color.White, CircleShape)
            }
            AppAppearance.BK -> {
                Modifier
                    .size(27.dp)
                    .shadow(2.dp, CircleShape)
                    .border(BorderStroke(0.6.dp, BrandTheme.bkChromeHairline), CircleShape)
                    .background(Color.White, CircleShape)
            }
            AppAppearance.CLASSIC -> {
                Modifier
                    .size(27.dp)
                    .shadow(2.dp, CircleShape)
                    .background(Color.White, CircleShape)
            }
        }
        Box(thumbModifier)
    }
}

/**
 * GlossyPrimaryButton — 클래식의 원래 광택 차콜을 보존하고 BK는 더 짙은 카본, Interstellar는 흑요석/골드로 표현한다.
 * 화면당 하나의 강조 액션 규칙을 엄격하게 준수한다.
 */
@Composable
fun GlossyPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp),
    appearance: AppAppearance = LocalAppAppearance.current,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, label = "glossyPrimaryScale")
    val shape = RoundedCornerShape(16.dp)

    val buttonModifier = when (appearance) {
        AppAppearance.INTERSTELLAR -> {
            val shadowAlpha = if (pressed) 0.25f else 0.45f
            Modifier
                .shadow(
                    elevation = if (pressed) 4.dp else 10.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = shadowAlpha),
                    spotColor = Color.Black.copy(alpha = shadowAlpha)
                )
                .background(BrandTheme.interstellarGlossy, shape)
                .border(BorderStroke(0.8.dp, BrandTheme.interstellarAccent.copy(alpha = 0.5f)), shape)
        }
        AppAppearance.BK -> {
            val shadowAlpha = if (pressed) 0.18f else 0.34f
            Modifier
                .shadow(
                    elevation = if (pressed) 4.dp else 10.dp,
                    shape = shape,
                    ambientColor = BrandTheme.ink.copy(alpha = shadowAlpha),
                    spotColor = BrandTheme.ink.copy(alpha = shadowAlpha)
                )
                .background(BrandTheme.bkGlossyCarbon, shape)
                .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.28f)), shape)
        }
        AppAppearance.CLASSIC -> {
            val shadowAlpha = if (pressed) 0.18f else 0.34f
            Modifier
                .shadow(
                    elevation = if (pressed) 4.dp else 10.dp,
                    shape = shape,
                    ambientColor = BrandTheme.ink.copy(alpha = shadowAlpha),
                    spotColor = BrandTheme.ink.copy(alpha = shadowAlpha)
                )
                .background(BrandTheme.glossyBlack, shape)
                .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.24f)), shape)
        }
    }

    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .then(buttonModifier)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .alpha(if (enabled) 1f else 0.46f)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
