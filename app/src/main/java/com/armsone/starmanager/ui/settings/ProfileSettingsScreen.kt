package com.armsone.starmanager.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armsone.starmanager.design.BrandTheme
import com.armsone.starmanager.design.IconWell
import com.armsone.starmanager.design.IconWellVariant
import com.armsone.starmanager.design.LocalAppAppearance
import com.armsone.starmanager.design.StarSegmentedControl
import com.armsone.starmanager.design.StarSlider
import com.armsone.starmanager.design.StarSwitch
import com.armsone.starmanager.model.AppAppearance
import com.armsone.starmanager.model.CreatorProfileStore
import com.armsone.starmanager.model.GenerationStylePreset
import com.armsone.starmanager.model.PostLength
import com.armsone.starmanager.model.PostMood
import com.armsone.starmanager.model.WritingPreset
import com.armsone.starmanager.service.DirectAIProvider
import com.armsone.starmanager.ui.externalai.ExternalAISurface
import com.armsone.starmanager.ui.externalai.ExternalAISurfaceMode
import com.armsone.starmanager.update.DirectUpdateManager
import com.armsone.starmanager.update.DirectUpdateSettings

/**
 * "설정" 화면.
 * iOS StarManager 기준 순서:
 * 1. 내 프리셋
 * 2. 기본 설정 (말투, 이모지 사용)
 * 3. 글쓰기 취향 (MZ/X/386/꼰대, 분위기, 이야기 비중, 글자 수)
 * 4. 추가 옵션 (한 줄 추가 요청 1..3줄, 금지 표현, 해시태그)
 * 5. 프리셋 보관
 * 6. 외부 AI 로그인 관리 (Gemini, ChatGPT, Claude)
 * 7. 앱 업데이트
 * 8. 테마 관리 (BK / 클래식)
 */
@Composable
fun ProfileSettingsScreen(store: CreatorProfileStore) {
    val context = LocalContext.current
    val updateManager = remember(context) { DirectUpdateManager.get(context) }
    val appearance by store.appearance.collectAsStateWithLifecycle()
    val profile by store.profile.collectAsStateWithLifecycle()
    val presets by store.presets.collectAsStateWithLifecycle()

    var presetName by rememberSaveable { mutableStateOf("") }
    var activeLoginProvider by remember { mutableStateOf<DirectAIProvider?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(BrandTheme.settingsBackground(appearance))
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
                // 1. 내 프리셋
                SettingsSection(
                    header = "내 프리셋",
                    icon = Icons.Filled.CheckCircle,
                    appearance = appearance,
                    variant = IconWellVariant.CARBON
                ) {
                    if (presets.isEmpty()) {
                        Text(
                            "보관된 프리셋이 없어요.",
                            fontSize = 15.sp,
                            color = BrandTheme.labelSecondary(appearance),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        presets.forEachIndexed { index, preset ->
                            if (index > 0) {
                                HorizontalDivider(color = BrandTheme.divider(appearance))
                            }
                            PresetRow(
                                preset = preset,
                                appearance = appearance,
                                onApply = { store.apply(preset) },
                                onDelete = { store.deletePreset(index) },
                                modifier = Modifier.testTag("settings.preset.$index")
                            )
                        }
                    }
                }

                // 2. 기본 설정 (말투, 이모지 사용)
                SettingsSection(
                    header = "기본 설정",
                    icon = Icons.Filled.Tune,
                    appearance = appearance,
                    variant = IconWellVariant.OXBLOOD
                ) {
                    LabeledFieldRow("말투", profile.voice, appearance, testTag = "settings.voice") {
                        store.updateProfile { p -> p.copy(voice = it) }
                    }
                    HorizontalDivider(color = BrandTheme.divider(appearance))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("이모지 사용", fontSize = 17.sp, color = BrandTheme.labelPrimary(appearance))
                        Spacer(Modifier.weight(1f))
                        StarSwitch(
                            checked = profile.usesEmoji,
                            appearance = appearance,
                            onCheckedChange = { checked ->
                                store.updateProfile { p -> p.copy(usesEmoji = checked) }
                            },
                            modifier = Modifier.testTag("settings.usesEmoji")
                        )
                    }
                }

                // 3. 글쓰기 취향
                SettingsSection(
                    header = "글쓰기 취향",
                    icon = Icons.Filled.AutoAwesome,
                    footer = "프리셋·분위기·이야기 비중·글자 수만 간단히 요청에 반영됩니다.",
                    appearance = appearance,
                    variant = IconWellVariant.CARBON
                ) {
                    val columns = if (LocalDensity.current.fontScale >= 1.3f) 1 else 4
                    GenerationStylePreset.entries.chunked(columns).forEach { rowPresets ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPresets.forEach { preset ->
                                StyleButton(
                                    preset = preset,
                                    selected = profile.selectedGenerationStyle == preset,
                                    appearance = appearance,
                                    onClick = { store.applyGenerationStyle(preset) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings.style.${preset.rawValue}")
                                )
                            }
                            repeat(columns - rowPresets.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    HorizontalDivider(color = BrandTheme.divider(appearance))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("분위기", fontSize = 17.sp, color = BrandTheme.labelPrimary(appearance), modifier = Modifier.width(80.dp))
                        StarSegmentedControl(
                            options = PostMood.entries.map { it.rawValue },
                            selectedIndex = PostMood.entries.indexOf(profile.mood),
                            appearance = appearance,
                            onSelect = { store.updateProfile { p -> p.copy(mood = PostMood.entries[it]) } },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings.mood")
                        )
                    }
                    HorizontalDivider(color = BrandTheme.divider(appearance))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("이야기 비중", fontSize = 17.sp, color = BrandTheme.labelPrimary(appearance), modifier = Modifier.width(80.dp))
                        StarSegmentedControl(
                            options = PostLength.entries.map { it.storyWeightTitle },
                            selectedIndex = PostLength.entries.indexOf(profile.preferredLength),
                            appearance = appearance,
                            onSelect = { store.updateProfile { p -> p.copy(preferredLength = PostLength.entries[it]) } },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings.length")
                        )
                    }
                    HorizontalDivider(color = BrandTheme.divider(appearance))
                    ControlSliderRow(
                        title = "글자 수",
                        value = profile.controls.characterCount,
                        range = 50f..500f,
                        step = 10f,
                        suffix = "자",
                        appearance = appearance,
                        testTag = "settings.slider.characterCount"
                    ) { newValue ->
                        store.updateProfile { p ->
                            p.withControls(p.controls.copy(characterCount = newValue))
                        }
                    }
                }

                // 4. 추가 옵션
                SettingsSection(
                    header = "추가 옵션",
                    icon = Icons.Outlined.Description,
                    appearance = appearance,
                    variant = IconWellVariant.CARBON
                ) {
                    MultilineFieldRow(
                        placeholder = "한 줄 추가 요청",
                        value = profile.additionalInstructions ?: "",
                        minLines = 1,
                        maxLines = 3,
                        appearance = appearance,
                        testTag = "settings.additionalInstructions"
                    ) {
                        store.updateProfile { p -> p.copy(additionalInstructions = it) }
                    }
                    HorizontalDivider(color = BrandTheme.divider(appearance))
                    MultilineFieldRow(
                        placeholder = "금지 표현",
                        value = profile.prohibitedPhrases,
                        minLines = 1,
                        maxLines = 4,
                        appearance = appearance,
                        testTag = "settings.prohibitedPhrases"
                    ) {
                        store.updateProfile { p -> p.copy(prohibitedPhrases = it) }
                    }
                    HorizontalDivider(color = BrandTheme.divider(appearance))
                    MultilineFieldRow(
                        placeholder = "해시태그",
                        value = profile.hashtagStyle,
                        minLines = 1,
                        maxLines = 1,
                        appearance = appearance,
                        testTag = "settings.hashtagStyle"
                    ) {
                        store.updateProfile { p -> p.copy(hashtagStyle = it) }
                    }
                }

                // 5. 프리셋 보관
                SettingsSection(
                    header = "프리셋 보관",
                    icon = Icons.Filled.CheckCircle,
                    appearance = appearance,
                    variant = IconWellVariant.CARBON
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(Modifier.weight(1f)) {
                            if (presetName.isEmpty()) {
                                Text("새 프리셋 이름", fontSize = 17.sp, color = BrandTheme.labelSecondary(appearance))
                            }
                            BasicTextField(
                                value = presetName,
                                onValueChange = { presetName = it },
                                textStyle = TextStyle(fontSize = 17.sp, color = BrandTheme.labelPrimary(appearance)),
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
                            fontWeight = FontWeight.Medium,
                            color = if (saveEnabled) BrandTheme.accent else BrandTheme.labelSecondary(appearance),
                            modifier = Modifier
                                .clickable(enabled = saveEnabled) {
                                    store.savePreset(presetName)
                                    presetName = ""
                                }
                                .testTag("settings.savePreset")
                        )
                    }
                }

                // 6. 외부 AI 로그인 관리 (Gemini, ChatGPT, Claude)
                SettingsSection(
                    header = "외부 AI 로그인 관리",
                    icon = Icons.Filled.AccountCircle,
                    footer = "처음 열면 각 서비스의 공식 로그인 페이지가 떠요. 한 번 로그인하면 이 기기에서는 서비스가 로그아웃시키기 전까지 기억돼요. 스타매니저는 비밀번호를 보거나 저장하지 않아요.",
                    appearance = appearance,
                    variant = IconWellVariant.CARBON
                ) {
                    val providers = listOf(DirectAIProvider.GEMINI, DirectAIProvider.OPEN_AI, DirectAIProvider.CLAUDE)
                    providers.forEachIndexed { index, provider ->
                        if (index > 0) {
                            HorizontalDivider(color = BrandTheme.divider(appearance))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeLoginProvider = provider }
                                .padding(vertical = 12.dp)
                                .testTag("settings.loginRow.${provider.rawValue}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                provider.title,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = BrandTheme.labelPrimary(appearance),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "로그인 열기",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandTheme.accent,
                                modifier = Modifier.testTag("settings.login.${provider.rawValue}")
                            )
                        }
                    }
                }

                // 7. 앱 업데이트
                SettingsSection(
                    header = "앱 업데이트",
                    icon = Icons.Outlined.ArrowCircleDown,
                    appearance = appearance,
                    variant = IconWellVariant.CARBON
                ) {
                    DirectUpdateSettings(updateManager)
                }

                // 8. 테마 관리 (BK / 클래식 외형 전환)
                SettingsSection(
                    header = "테마 관리",
                    icon = Icons.Outlined.Diamond,
                    footer = "클래식을 고르면 예전 모습으로 볼 수 있어요.",
                    appearance = appearance,
                    variant = IconWellVariant.CARBON
                ) {
                    StarSegmentedControl(
                        options = listOf(AppAppearance.BK.title, AppAppearance.CLASSIC.title),
                        selectedIndex = if (appearance == AppAppearance.BK) 0 else 1,
                        appearance = appearance,
                        onSelect = { index ->
                            store.setAppearance(if (index == 0) AppAppearance.BK else AppAppearance.CLASSIC)
                        },
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .testTag("settings.appearance")
                    )
                }
            }
        }
    }

    val loginProvider = activeLoginProvider
    if (loginProvider != null) {
        ExternalAISurface(
            provider = loginProvider,
            mode = ExternalAISurfaceMode.LOGIN,
            prompt = "",
            onClose = { activeLoginProvider = null },
            appearance = appearance
        )
    }
}

@Composable
private fun StyleButton(
    preset: GenerationStylePreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    appearance: AppAppearance = LocalAppAppearance.current
) {
    val isBk = appearance == AppAppearance.BK
    val shape = RoundedCornerShape(if (isBk) 14.dp else 12.dp)
    val bg = if (isBk) {
        if (selected) Color.White else Color(0xFFF1F3F6)
    } else {
        if (selected) BrandTheme.paper else BrandTheme.surface
    }
    val borderStroke = if (selected) {
        BorderStroke(1.5.dp, BrandTheme.accent)
    } else {
        BorderStroke(1.dp, if (isBk) Color(0xFFE2E6EC) else BrandTheme.border)
    }

    Column(
        modifier = modifier
            .heightIn(min = 58.dp)
            .then(if (borderStroke != null) Modifier.border(borderStroke, shape) else Modifier)
            .background(bg, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        IconWell(
            icon = presetIcon(preset),
            appearance = appearance,
            variant = if (isBk) {
                if (selected) IconWellVariant.OXBLOOD else IconWellVariant.CARBON
            } else IconWellVariant.CARBON,
            size = 28.dp,
            iconSize = 16.dp
        )
        Text(
            preset.title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandTheme.labelPrimary(appearance)
        )
    }
}

private fun presetIcon(preset: GenerationStylePreset): ImageVector = when (preset) {
    GenerationStylePreset.MZ -> Icons.Filled.EmojiPeople
    GenerationStylePreset.GEN_X -> Icons.Filled.DirectionsWalk
    GenerationStylePreset.GENERATION_386 -> Icons.Outlined.Accessibility
    GenerationStylePreset.BABY_BOOM -> Icons.Filled.Person
}

@Composable
private fun SettingsSection(
    header: String? = null,
    icon: ImageVector? = null,
    footer: String? = null,
    appearance: AppAppearance = LocalAppAppearance.current,
    variant: IconWellVariant = IconWellVariant.CARBON,
    content: @Composable () -> Unit
) {
    val isBk = appearance == AppAppearance.BK
    Column {
        if (header != null) {
            Row(
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null && isBk) {
                    IconWell(
                        icon = icon,
                        appearance = appearance,
                        variant = variant,
                        size = 22.dp,
                        iconSize = if (isBk) 13.dp else 16.dp
                    )
                }
                Text(
                    header,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandTheme.labelSecondary(appearance)
                )
            }
        }
        val shape = RoundedCornerShape(if (isBk) 14.dp else 10.dp)
        val bg = BrandTheme.settingsSectionBackground(appearance)
        val border = if (isBk) BorderStroke(1.dp, Color(0xFFE2E6EC)) else null
        val shadowElevation = if (isBk) 3.dp else 0.dp

        Column(
            Modifier
                .fillMaxWidth()
                .shadow(shadowElevation, shape, ambientColor = Color(0x08141518), spotColor = Color(0x0C141518))
                .then(if (border != null) Modifier.border(border, shape) else Modifier)
                .background(bg, shape)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            content()
        }

        if (footer != null) {
            Text(
                footer,
                fontSize = 13.sp,
                color = BrandTheme.labelSecondary(appearance),
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp, end = 4.dp)
            )
        }
    }
}

@Composable
private fun PresetRow(
    preset: WritingPreset,
    appearance: AppAppearance,
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
            Text(preset.name, fontSize = 17.sp, color = BrandTheme.labelPrimary(appearance))
            Text("${preset.controls.characterCount}자", fontSize = 12.sp, color = BrandTheme.labelSecondary(appearance))
        }
        Icon(
            Icons.Outlined.ArrowCircleDown,
            contentDescription = "적용",
            tint = BrandTheme.accent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
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
    appearance: AppAppearance,
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
        Text(title, fontSize = 17.sp, color = BrandTheme.labelPrimary(appearance))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontSize = 17.sp,
                color = BrandTheme.labelSecondary(appearance),
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
    appearance: AppAppearance,
    testTag: String,
    onValueChange: (String) -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, fontSize = 17.sp, color = BrandTheme.labelSecondary(appearance))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 17.sp, color = BrandTheme.labelPrimary(appearance)),
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
    appearance: AppAppearance,
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
            Text(title, fontSize = 17.sp, color = BrandTheme.labelPrimary(appearance))
            Spacer(Modifier.weight(1f))
            Text("$value$suffix", fontSize = 17.sp, color = BrandTheme.labelSecondary(appearance))
        }
        StarSlider(
            value = value.toFloat(),
            onValueChange = { raw ->
                onValueChange((Math.round(raw / step) * step).toInt())
            },
            valueRange = range,
            step = step,
            appearance = appearance,
            modifier = Modifier.testTag(testTag)
        )
    }
}
