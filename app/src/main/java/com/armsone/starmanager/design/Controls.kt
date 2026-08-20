package com.armsone.starmanager.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
