package com.armsone.starmanager.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * iOS UISegmentedControl 근사치. Material 기본 SegmentedButton은 외형이 크게
 * 달라 커스텀으로 그린다.
 */
@Composable
fun StarSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerShape = RoundedCornerShape(9.dp)
    val segmentShape = RoundedCornerShape(7.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(Color(0x1F767680), containerShape)
            .padding(2.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (selected) {
                            Modifier
                                .shadow(2.dp, segmentShape)
                                .background(Color.White, segmentShape)
                        } else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(index) }
                    .testTag("segment.$option"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = BrandTheme.labelPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/** iOS UISlider 근사치 — 흰색 원형 썸, 좁은 트랙, 액센트 채움. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    modifier: Modifier = Modifier,
    tint: Color = BrandTheme.accent
) {
    val stepsCount = (((valueRange.endInclusive - valueRange.start) / step).toInt() - 1)
        .coerceAtLeast(0)
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = stepsCount,
        modifier = modifier.height(28.dp),
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = tint,
            inactiveTrackColor = Color(0x2E787880),
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent
        ),
        thumb = {
            Box(
                Modifier
                    .size(26.dp)
                    .shadow(3.dp, CircleShape)
                    .background(Color.White, CircleShape)
            )
        }
    )
}

/** iOS 토글 스위치 근사치 — 켜짐 색은 전역 tint(accent)를 따른다. */
@Composable
fun StarSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor = if (checked) BrandTheme.accent else Color(0x2E787880)
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
        Box(
            Modifier
                .size(27.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}

/**
 * iOS GlossyPrimaryButtonStyle 근사치 — 광택 차콜 배경의 단일 지배적 액션 버튼.
 * 화면당 한 곳에만 써서 "하나의 강조 액션" 규칙을 지킨다.
 */
@Composable
fun GlossyPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, label = "glossyPrimaryScale")
    val shape = RoundedCornerShape(16.dp)
    val shadowAlpha = if (pressed) 0.18f else 0.34f
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .shadow(
                elevation = if (pressed) 4.dp else 10.dp,
                shape = shape,
                ambientColor = BrandTheme.ink.copy(alpha = shadowAlpha),
                spotColor = BrandTheme.ink.copy(alpha = shadowAlpha)
            )
            .background(BrandTheme.glossyBlack, shape)
            .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.24f)), shape)
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
