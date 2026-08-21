package com.armsone.starmanager.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armsone.starmanager.design.BrandTheme
import com.armsone.starmanager.design.StarSlider
import com.armsone.starmanager.design.StarSwitch
import com.armsone.starmanager.model.CreatorProfileStore
import com.armsone.starmanager.model.GenerationControls
import com.armsone.starmanager.model.WritingPreset

/**
 * iOS ProfileSettingsView(Form) 포팅.
 * SwiftUI Form의 inset grouped 스타일을 커스텀 섹션 카드로 근사한다.
 * 콘텐츠 최대 폭 760dp.
 */
@Composable
fun ProfileSettingsScreen(store: CreatorProfileStore) {
    val profile by store.profile.collectAsStateWithLifecycle()
    val presets by store.presets.collectAsStateWithLifecycle()

    var presetName by rememberSaveable { mutableStateOf("") }
    var showsToneControls by rememberSaveable { mutableStateOf(false) }
    var showsAdvancedPrompt by rememberSaveable { mutableStateOf(false) }
    var showsRestoreConfirmation by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7)) // iOS grouped background
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // 내 프리셋
                SettingsSection(header = "내 프리셋") {
                    presets.forEachIndexed { index, preset ->
                        PresetRow(
                            preset = preset,
                            onApply = { store.apply(preset) },
                            onDelete = { store.deletePreset(preset) },
                            modifier = Modifier.testTag("settings.preset.$index")
                        )
                        if (index < presets.lastIndex) HorizontalDivider(color = Color(0x293C3C43))
                    }
                }

                // 기본 설정
                SettingsSection(header = "기본 설정") {
                    LabeledFieldRow("주제", profile.accountTopic, testTag = "settings.topic") {
                        store.updateProfile { p -> p.copy(accountTopic = it) }
                    }
                    HorizontalDivider(color = Color(0x293C3C43))
                    LabeledFieldRow("독자", profile.audience, testTag = "settings.audience") {
                        store.updateProfile { p -> p.copy(audience = it) }
                    }
                    HorizontalDivider(color = Color(0x293C3C43))
                    LabeledFieldRow("말투", profile.voice, testTag = "settings.voice") {
                        store.updateProfile { p -> p.copy(voice = it) }
                    }
                    HorizontalDivider(color = Color(0x293C3C43))
                    ControlSliderRow(
                        title = "글자 수",
                        value = profile.controls.characterCount,
                        range = 50f..500f,
                        step = 10f,
                        suffix = "자",
                        testTag = "settings.slider.characterCount"
                    ) { newValue ->
                        store.updateProfile { p ->
                            p.withControls(p.controls.copy(characterCount = newValue))
                        }
                    }
                    HorizontalDivider(color = Color(0x293C3C43))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("이모지 사용", fontSize = 17.sp)
                        Spacer(Modifier.weight(1f))
                        StarSwitch(
                            checked = profile.usesEmoji,
                            onCheckedChange = { checked ->
                                store.updateProfile { p -> p.copy(usesEmoji = checked) }
                            },
                            modifier = Modifier.testTag("settings.usesEmoji")
                        )
                    }
                }

                // 분위기 조절 (DisclosureGroup)
                SettingsSection {
                    DisclosureHeader(
                        title = "분위기 조절",
                        expanded = showsToneControls,
                        onToggle = { showsToneControls = !showsToneControls },
                        testTag = "settings.toneDisclosure"
                    )
                    AnimatedVisibility(visible = showsToneControls) {
                        Column {
                            ToneSliderRow(store, "감동", profile.controls.emotion, "settings.slider.emotion") { c, v -> c.copy(emotion = v) }
                            ToneSliderRow(store, "친절함", profile.controls.kindness, "settings.slider.kindness") { c, v -> c.copy(kindness = v) }
                            ToneSliderRow(store, "참신함", profile.controls.originality, "settings.slider.originality") { c, v -> c.copy(originality = v) }
                            ToneSliderRow(store, "단단함", profile.controls.masculinity, "settings.slider.masculinity") { c, v -> c.copy(masculinity = v) }
                            ToneSliderRow(store, "시크함", profile.controls.chic, "settings.slider.chic") { c, v -> c.copy(chic = v) }

                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("합계", fontSize = 13.sp)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "${profile.controls.toneTotal}%",
                                    fontSize = 13.sp,
                                    color = if (profile.controls.toneTotal == 100) {
                                        BrandTheme.labelSecondary
                                    } else {
                                        BrandTheme.orange
                                    },
                                    modifier = Modifier.testTag("settings.toneTotal")
                                )
                            }
                        }
                    }
                }

                // 추가 옵션
                SettingsSection(header = "추가 옵션") {
                    MultilineFieldRow(
                        placeholder = "추가 지침",
                        value = profile.additionalInstructions ?: "",
                        minLines = 3,
                        maxLines = 8,
                        testTag = "settings.additionalInstructions"
                    ) {
                        store.updateProfile { p -> p.copy(additionalInstructions = it) }
                    }
                    HorizontalDivider(color = Color(0x293C3C43))
                    MultilineFieldRow(
                        placeholder = "금지 표현",
                        value = profile.prohibitedPhrases,
                        minLines = 1,
                        maxLines = 4,
                        testTag = "settings.prohibitedPhrases"
                    ) {
                        store.updateProfile { p -> p.copy(prohibitedPhrases = it) }
                    }
                    HorizontalDivider(color = Color(0x293C3C43))
                    MultilineFieldRow(
                        placeholder = "해시태그",
                        value = profile.hashtagStyle,
                        minLines = 1,
                        maxLines = 1,
                        testTag = "settings.hashtagStyle"
                    ) {
                        store.updateProfile { p -> p.copy(hashtagStyle = it) }
                    }
                }

                // 프리셋 보관
                SettingsSection(header = "프리셋 보관") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(Modifier.weight(1f)) {
                            if (presetName.isEmpty()) {
                                Text("새 프리셋 이름", fontSize = 17.sp, color = BrandTheme.labelSecondary)
                            }
                            BasicTextField(
                                value = presetName,
                                onValueChange = { presetName = it },
                                textStyle = TextStyle(fontSize = 17.sp, color = BrandTheme.labelPrimary),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings.presetName")
                            )
                        }
                        val saveEnabled = presetName.trim().isNotEmpty()
                        Text(
                            "이 기기에 보관",
                            fontSize = 17.sp,
                            color = if (saveEnabled) BrandTheme.accent else BrandTheme.labelSecondary,
                            modifier = Modifier
                                .clickable(enabled = saveEnabled) {
                                    store.savePreset(presetName)
                                    presetName = ""
                                }
                                .testTag("settings.savePreset")
                        )
                    }
                }

                // 작성 원칙
                SettingsSection(header = "작성 원칙") {
                    DisclosureHeader(
                        title = "직접 편집",
                        expanded = showsAdvancedPrompt,
                        onToggle = { showsAdvancedPrompt = !showsAdvancedPrompt },
                        testTag = "settings.guidelinesDisclosure"
                    )
                    AnimatedVisibility(visible = showsAdvancedPrompt) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            BasicTextField(
                                value = profile.writingGuidelines,
                                onValueChange = { value ->
                                    store.updateProfile { p -> p.copy(writingGuidelines = value) }
                                },
                                textStyle = TextStyle(fontSize = 16.sp, color = BrandTheme.labelPrimary, lineHeight = 22.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 360.dp)
                                    .background(Color(0xFFF7F7F9), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                                    .testTag("settings.guidelines")
                            )
                            Text(
                                "기본값으로 되돌리기",
                                fontSize = 17.sp,
                                color = BrandTheme.red,
                                modifier = Modifier
                                    .clickable { showsRestoreConfirmation = true }
                                    .padding(vertical = 8.dp)
                                    .testTag("settings.restoreDefaults")
                            )
                        }
                    }
                }
            }
        }
    }

    if (showsRestoreConfirmation) {
        RestoreConfirmationDialog(
            onConfirm = {
                store.restoreDefaultWritingGuidelines()
                showsRestoreConfirmation = false
            },
            onDismiss = { showsRestoreConfirmation = false }
        )
    }
}

@Composable
private fun SettingsSection(
    header: String? = null,
    content: @Composable () -> Unit
) {
    Column {
        if (header != null) {
            Text(
                header.uppercase(),
                fontSize = 13.sp,
                color = BrandTheme.labelSecondary,
                modifier = Modifier.padding(start = 16.dp, bottom = 7.dp)
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun PresetRow(
    preset: WritingPreset,
    onApply: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onApply)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(preset.name, fontSize = 17.sp, color = BrandTheme.labelPrimary)
            Text("${preset.controls.characterCount}자", fontSize = 12.sp, color = BrandTheme.labelSecondary)
        }
        Icon(
            Icons.Outlined.ArrowCircleDown,
            contentDescription = "적용",
            tint = BrandTheme.accent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        // iOS는 스와이프 삭제 — 명시적 삭제 버튼으로 대체한다.
        Icon(
            Icons.Filled.Delete,
            contentDescription = "삭제",
            tint = BrandTheme.red,
            modifier = Modifier
                .size(22.dp)
                .clickable(onClick = onDelete)
        )
    }
}

@Composable
private fun LabeledFieldRow(
    title: String,
    value: String,
    testTag: String,
    onValueChange: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, fontSize = 17.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontSize = 17.sp,
                color = BrandTheme.labelSecondary,
                textAlign = TextAlign.End
            ),
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag(testTag)
        )
    }
}

@Composable
private fun MultilineFieldRow(
    placeholder: String,
    value: String,
    minLines: Int,
    maxLines: Int,
    testTag: String,
    onValueChange: (String) -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, fontSize = 17.sp, color = BrandTheme.labelSecondary)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 17.sp, color = BrandTheme.labelPrimary),
            minLines = minLines,
            maxLines = maxLines,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

@Composable
private fun ControlSliderRow(
    title: String,
    value: Int,
    range: ClosedFloatingPointRange<Float> = 0f..100f,
    step: Float = 5f,
    suffix: String = "%",
    testTag: String,
    onValueChange: (Int) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 17.sp)
            Spacer(Modifier.weight(1f))
            Text("$value$suffix", fontSize = 17.sp, color = BrandTheme.labelSecondary)
        }
        StarSlider(
            value = value.toFloat(),
            onValueChange = { raw ->
                onValueChange((Math.round(raw / step) * step).toInt())
            },
            valueRange = range,
            step = step,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun ToneSliderRow(
    store: CreatorProfileStore,
    title: String,
    value: Int,
    testTag: String,
    apply: (GenerationControls, Int) -> GenerationControls
) {
    ControlSliderRow(title = title, value = value, testTag = testTag) { newValue ->
        store.updateProfile { p -> p.withControls(apply(p.controls, newValue)) }
    }
}

@Composable
private fun DisclosureHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    testTag: String
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 17.sp)
        Spacer(Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = BrandTheme.accent
        )
    }
}

/** iOS confirmationDialog 대응. */
@Composable
private fun RestoreConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .background(Color.White, RoundedCornerShape(14.dp))
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "기본 작성 지침으로 되돌릴까요?",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.padding(4.dp))
            Text(
                "직접 수정한 내용은 사라집니다.",
                fontSize = 13.sp,
                color = BrandTheme.labelSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.padding(8.dp))
            HorizontalDivider(color = Color(0x293C3C43))
            TextButton(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings.restoreConfirm")
            ) {
                Text("되돌리기", fontSize = 17.sp, color = BrandTheme.red)
            }
            HorizontalDivider(color = Color(0x293C3C43))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings.restoreCancel")
            ) {
                Text("취소", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = BrandTheme.accent)
            }
        }
    }
}
