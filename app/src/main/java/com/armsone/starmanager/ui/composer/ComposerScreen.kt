package com.armsone.starmanager.ui.composer

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armsone.starmanager.R
import com.armsone.starmanager.design.BrandTheme
import com.armsone.starmanager.design.StarSegmentedControl
import com.armsone.starmanager.design.StarSlider
import com.armsone.starmanager.design.starCard
import com.armsone.starmanager.model.GenerationStylePreset
import com.armsone.starmanager.model.PostLength
import com.armsone.starmanager.model.PostMood
import com.armsone.starmanager.service.DirectAIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
fun ComposerScreen(viewModel: ComposerViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(BrandTheme.canvas)
    ) {
        // iOS horizontalSizeClass == .regular 대응: 600dp 이상이면 2열.
        val isRegular = maxWidth >= 600.dp
        val horizontalPadding = if (isRegular) 24.dp else 16.dp
        val verticalPadding = if (isRegular) 20.dp else 12.dp
        val contentMaxWidth = if (isRegular) 1120.dp else 680.dp

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isRegular) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = contentMaxWidth),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(Modifier.weight(1f)) { CreationColumn(viewModel, state) }
                    Box(Modifier.weight(1f)) { PreviewColumn(viewModel, state) }
                }
            } else {
                Column(
                    Modifier.widthIn(max = contentMaxWidth),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CreationColumn(viewModel, state)
                    PreviewColumn(viewModel, state)
                }
            }
        }
    }
}

// MARK: - 만들기 카드

@Composable
private fun CreationColumn(viewModel: ComposerViewModel, state: ComposerUiState) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val profile by viewModel.profileStore.profile.collectAsStateWithLifecycle()

    val maxSelection = maxOf(1, 10 - state.mediaItems.size)
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = if (maxSelection < 2) 2 else maxSelection)
    ) { uris ->
        // maxItems<2가 계약상 불가하므로 초과분은 뷰모델에서 10개 제한으로 잘라낸다.
        viewModel.addPickedMedia(uris.take(maxSelection), context.contentResolver)
    }

    var cameraOutput by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val file = cameraOutput
        if (success && file != null && file.exists()) {
            viewModel.addCameraPhoto(file.readBytes())
        }
        file?.delete()
        cameraOutput = null
    }

    Column(
        Modifier
            .widthIn(max = 540.dp)
            .fillMaxWidth()
            .starCard()
            .testTag("composer.creationCard"),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            "오늘 전하고 싶은 이야기는 무엇인가요?",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 28.sp,
            maxLines = 2
        )

        IdeaField(
            value = state.idea,
            onValueChange = viewModel::setIdea,
            modifier = Modifier.testTag("composer.idea")
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("스타일", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            val columns = if (LocalDensity.current.fontScale >= 1.3f) 1 else 4
            GenerationStylePreset.entries.chunked(columns).forEach { rowPresets ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowPresets.forEach { preset ->
                        StyleButton(
                            preset = preset,
                            selected = state.selectedGenerationStyle == preset,
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.applyGenerationStyle(preset)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("composer.style.${preset.rawValue}")
                        )
                    }
                    repeat(columns - rowPresets.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("분위기", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(70.dp))
            StarSegmentedControl(
                options = PostMood.entries.map { it.rawValue },
                selectedIndex = PostMood.entries.indexOf(state.mood),
                onSelect = { viewModel.setMood(PostMood.entries[it]) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("composer.mood")
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("이야기 비중", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(70.dp))
            StarSegmentedControl(
                options = PostLength.entries.map { it.storyWeightTitle },
                selectedIndex = PostLength.entries.indexOf(state.length),
                onSelect = { viewModel.setLength(PostLength.entries[it]) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("composer.length")
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("글자 수", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(
                    "${profile.controls.characterCount}자",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandTheme.accent
                )
            }
            StarSlider(
                value = profile.controls.characterCount.toFloat(),
                onValueChange = { viewModel.setCharacterCount((Math.round(it / 10f) * 10)) },
                valueRange = 50f..500f,
                step = 10f,
                modifier = Modifier.testTag("composer.slider.characterCount")
            )
        }

        AiChoiceButtons(viewModel, state)

        if (state.generatedPost != null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("미디어", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

                StarSegmentedControl(
                    options = PreviewAspect.entries.map { it.title },
                    selectedIndex = PreviewAspect.entries.indexOf(state.previewAspect),
                    onSelect = { viewModel.setPreviewAspect(PreviewAspect.entries[it]) },
                    modifier = Modifier.testTag("composer.aspect")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BorderedActionButton(
                        title = "미디어",
                        icon = if (state.mediaItems.isEmpty()) Icons.Filled.AddPhotoAlternate else Icons.Filled.CheckCircle,
                        enabled = !state.isLoadingMedia && state.mediaItems.size < 10,
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("composer.pickMedia")
                    )
                    BorderedActionButton(
                        title = "카메라",
                        icon = Icons.Filled.PhotoCamera,
                        enabled = state.mediaItems.size < 10,
                        onClick = {
                            val hasCamera = context.packageManager
                                .hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
                            if (!hasCamera) {
                                viewModel.cameraUnavailable()
                            } else {
                                val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                                val file = File(dir, "capture-${UUID.randomUUID()}.jpg")
                                cameraOutput = file
                                cameraLauncher.launch(
                                    FileProvider.getUriForFile(
                                        context, "com.armsone.starmanager.fileprovider", file
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("composer.camera")
                    )
                }

                if (state.mediaItems.isNotEmpty()) {
                    MediaOrderEditor(viewModel, state)
                }
            }
        }

        val message = state.errorMessage ?: state.statusMessage
        if (message != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    if (state.errorMessage == null) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (state.errorMessage == null) BrandTheme.accent else BrandTheme.red,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    message,
                    fontSize = 13.sp,
                    color = if (state.errorMessage == null) BrandTheme.accent else BrandTheme.red,
                    modifier = Modifier.testTag("composer.message")
                )
            }
        }
    }
}

@Composable
private fun IdeaField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(BrandTheme.canvas, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        if (value.isEmpty()) {
            Text("오늘의 이야기", fontSize = 17.sp, color = BrandTheme.labelSecondary)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 17.sp, color = BrandTheme.labelPrimary),
            minLines = 2,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StyleButton(
    preset: GenerationStylePreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .heightIn(min = 58.dp)
            .background(if (selected) BrandTheme.paper else BrandTheme.canvas, shape)
            .border(1.5.dp, if (selected) BrandTheme.accent else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            Modifier
                .size(30.dp)
                .background(BrandTheme.paper, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                presetIcon(preset),
                contentDescription = null,
                tint = BrandTheme.accent,
                modifier = Modifier.size(17.dp)
            )
        }
        Text(preset.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun presetIcon(preset: GenerationStylePreset): ImageVector = when (preset) {
    GenerationStylePreset.MZ -> Icons.Filled.EmojiPeople          // figure.wave 근사
    GenerationStylePreset.GEN_X -> Icons.Filled.DirectionsWalk    // figure.walk 근사
    GenerationStylePreset.GENERATION_386 -> Icons.Outlined.Accessibility // figure.stand 근사
    GenerationStylePreset.BABY_BOOM -> Icons.Filled.Person        // person.fill 근사
}

@Composable
private fun BorderedActionButton(
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .height(44.dp)
            .background(BrandTheme.accent.copy(alpha = if (enabled) 0.12f else 0.06f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = BrandTheme.accent.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = BrandTheme.accent.copy(alpha = if (enabled) 1f else 0.4f)
        )
    }
}

// MARK: - AI 선택 버튼

@Composable
private fun AiChoiceButtons(viewModel: ComposerViewModel, state: ComposerUiState) {
    val context = LocalContext.current
    val trimmedIdeaEmpty = state.idea.trim().isEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val columns = if (LocalDensity.current.fontScale >= 1.3f) 2 else 4
        AIChoice.all.chunked(columns).forEach { rowChoices ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                rowChoices.forEach { choice ->
                    val opacity = when {
                        trimmedIdeaEmpty -> 0.48f
                        state.isGenerating && choice != AIChoice.OnDevice -> 0.4f
                        else -> 1f
                    }
                    AiChoiceButton(
                        choice = choice,
                        isGenerating = state.isGenerating,
                        enabled = !trimmedIdeaEmpty && !state.isGenerating,
                        onClick = {
                            when (choice) {
                                is AIChoice.OnDevice -> viewModel.generateDraft()
                                is AIChoice.External -> {
                                    val intent = viewModel.sharePrompt(choice.provider, context)
                                    if (intent != null) {
                                        context.startActivity(android.content.Intent.createChooser(intent, null))
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer { alpha = opacity }
                            .testTag("composer.ai.${choice.id}")
                    )
                }
                repeat(columns - rowChoices.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        if (state.isGenerating) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(BrandTheme.accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    color = BrandTheme.accent,
                    strokeWidth = 2.dp
                )
                Text(
                    "Apple AI가 게시물을 만드는 중…",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("composer.generating")
                )
            }
        }

        val provider = state.pendingExternalProvider
        if (provider != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(BrandTheme.accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .clickable {
                        val text = viewModel.readClipboard(context)
                        viewModel.importAIResult(text, provider)
                    }
                    .testTag("composer.paste"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.ContentPaste,
                    contentDescription = null,
                    tint = BrandTheme.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("붙여넣기", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = BrandTheme.accent)
            }
        }
    }
}

@Composable
private fun AiChoiceButton(
    choice: AIChoice,
    isGenerating: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(13.dp)
    val (background, foreground, border) = when (choice) {
        is AIChoice.OnDevice -> Triple(BrandTheme.accent, Color.White, BrandTheme.accent)
        is AIChoice.External -> when (choice.provider) {
            DirectAIProvider.OPEN_AI -> Triple(
                Color.White, Color(0.10f, 0.10f, 0.11f), Color.Black.copy(alpha = 0.14f)
            )
            DirectAIProvider.GEMINI -> Triple(
                Color(0.92f, 0.95f, 1.00f),
                Color(0.10f, 0.10f, 0.11f),
                Color(0.25f, 0.52f, 0.96f).copy(alpha = 0.32f)
            )
            DirectAIProvider.GROK -> Triple(Color.Black, Color.White, Color.Black)
        }
    }
    Column(
        modifier = modifier
            .heightIn(min = 58.dp)
            .background(background, shape)
            .border(1.dp, border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            when {
                isGenerating && choice is AIChoice.OnDevice ->
                    CircularProgressIndicator(Modifier.size(20.dp), color = foreground, strokeWidth = 2.dp)
                choice is AIChoice.OnDevice ->
                    // iOS는 apple.logo — 상표 자산이라 스파클 근사치를 쓴다.
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = foreground)
                choice is AIChoice.External ->
                    BrandIcon(choice.provider)
            }
        }
        Text(
            choice.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = foreground,
            maxLines = 1
        )
    }
}

/**
 * iOS ChatGPTBrand/GeminiBrand/GrokBrand 이미지셋 대응.
 * scripts/copy-ios-assets.sh 로 복사된 drawable(brand_chatgpt 등)이 있으면 그대로 쓰고,
 * 없으면 첫 글자 배지로 대체한다.
 */
@Composable
private fun BrandIcon(provider: DirectAIProvider) {
    val resId = when (provider) {
        DirectAIProvider.OPEN_AI -> R.drawable.brand_chatgpt
        DirectAIProvider.GEMINI -> R.drawable.brand_gemini
        DirectAIProvider.GROK -> R.drawable.brand_grok
    }
    Image(
        painter = painterResource(resId),
        contentDescription = provider.title,
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(7.dp))
    )
}

// MARK: - 미디어 순서 편집

@Composable
private fun MediaOrderEditor(viewModel: ComposerViewModel, state: ComposerUiState) {
    val density = LocalDensity.current
    val itemSlotPx = with(density) { (104.dp + 10.dp).toPx() }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Column(
        Modifier
            .fillMaxWidth()
            .background(BrandTheme.canvas, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("길게 눌러 순서 변경", fontSize = 12.sp, color = BrandTheme.labelSecondary)

        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            state.mediaItems.forEachIndexed { index, media ->
                val isDragging = draggingId == media.id
                Box(
                    Modifier
                        .graphicsLayer {
                            if (isDragging) {
                                translationX = dragOffset
                                shadowElevation = 12f
                            }
                        }
                        .pointerInput(media.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = media.id
                                    dragOffset = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.x
                                    var currentIndex = viewModel.state.value.mediaItems
                                        .indexOfFirst { it.id == media.id }
                                    if (currentIndex >= 0) {
                                        while (dragOffset > itemSlotPx * 0.6f &&
                                            currentIndex + 1 < viewModel.state.value.mediaItems.size
                                        ) {
                                            viewModel.moveMedia(currentIndex, 1)
                                            currentIndex += 1
                                            dragOffset -= itemSlotPx
                                        }
                                        while (dragOffset < -itemSlotPx * 0.6f && currentIndex > 0) {
                                            viewModel.moveMedia(currentIndex, -1)
                                            currentIndex -= 1
                                            dragOffset += itemSlotPx
                                        }
                                    }
                                },
                                onDragEnd = { draggingId = null; dragOffset = 0f },
                                onDragCancel = { draggingId = null; dragOffset = 0f }
                            )
                        }
                        .semantics {
                            customActions = listOf(
                                CustomAccessibilityAction("앞으로 이동") {
                                    viewModel.moveMedia(index, -1)
                                    true
                                },
                                CustomAccessibilityAction("뒤로 이동") {
                                    viewModel.moveMedia(index, 1)
                                    true
                                }
                            )
                        }
                        .testTag("composer.media.$index")
                ) {
                    MediaThumbnail(media)

                    // 삭제 버튼 (우상단)
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(44.dp)
                            .clickable { viewModel.removeMedia(media.id) },
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Icon(
                            Icons.Filled.Cancel,
                            contentDescription = "${index + 1}번째 미디어 삭제",
                            tint = Color.Black.copy(alpha = 0.72f),
                            modifier = Modifier
                                .padding(6.dp)
                                .size(22.dp)
                                .background(Color.White, CircleShape)
                        )
                    }

                    if (index == 0) {
                        Text(
                            "대표",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .background(BrandTheme.accent, RoundedCornerShape(50))
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaThumbnail(media: ComposerMedia) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(14.dp)
    val imageBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, media.id) {
        value = withContext(Dispatchers.IO) {
            when (media.kind) {
                MediaKind.IMAGE -> runCatching {
                    BitmapFactory.decodeByteArray(media.data, 0, media.data.size)
                }.getOrNull()
                MediaKind.VIDEO -> makeVideoThumbnail(media, context.cacheDir)
            }
        }
    }
    Box(
        Modifier
            .size(width = 104.dp, height = 118.dp)
            .background(BrandTheme.paper, shape)
            .border(1.dp, Color.Black.copy(alpha = 0.09f), shape)
            .clip(shape)
    ) {
        val bitmap = imageBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = media.kind.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(20.dp), color = BrandTheme.accent, strokeWidth = 2.dp)
            }
        }
        Icon(
            if (media.kind == MediaKind.IMAGE) Icons.Filled.Photo else Icons.Filled.Videocam,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(7.dp)
                .background(Color.Black.copy(alpha = 0.66f), CircleShape)
                .padding(6.dp)
                .size(12.dp)
        )
    }
}

private fun makeVideoThumbnail(media: ComposerMedia, cacheDir: File): android.graphics.Bitmap? {
    val ext = media.fileExtension?.takeIf { it.isNotEmpty() } ?: "mp4"
    val file = File(cacheDir, "starmanager-thumb-${media.id}.$ext")
    return try {
        file.writeBytes(media.data)
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.path)
        val frame = retriever.getFrameAtTime(0)
        retriever.release()
        frame
    } catch (_: Exception) {
        null
    } finally {
        file.delete()
    }
}

// MARK: - 미리보기 카드

@Composable
private fun PreviewColumn(viewModel: ComposerViewModel, state: ComposerUiState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val post = state.generatedPost
    val candidates = viewModel.comparisonCandidates()
    // 프로필이 바뀌면 조건 만료 배지가 즉시 갱신되도록 구독한다.
    val profile by viewModel.profileStore.profile.collectAsStateWithLifecycle()
    val draftIsCurrent = state.generatedSignature ==
        DraftSignature(state.idea.trim(), state.mood, state.length, profile)

    Column(
        Modifier
            .widthIn(max = 540.dp)
            .fillMaxWidth()
            .starCard()
            .testTag("composer.previewCard"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("미리보기", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            val source = state.activeCaptionSource
            if (source != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        sourceIcon(source),
                        contentDescription = null,
                        tint = BrandTheme.labelSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(source.title, fontSize = 12.sp, color = BrandTheme.labelSecondary)
                }
            }
        }

        if (post != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (candidates.size > 1) {
                    CandidateComparison(viewModel, state, candidates)
                }

                if (state.mediaItems.isNotEmpty() || state.isLoadingMedia) {
                    MediaPreview(state.mediaItems, state.previewAspect.ratio, state.isLoadingMedia)
                }

                if (!draftIsCurrent) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = BrandTheme.orange,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "조건이 바뀌었어요. 다시 만들어 주세요.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandTheme.orange,
                            modifier = Modifier.testTag("preview.stale")
                        )
                    }
                }

                val validation = viewModel.activeValidationReport()
                if (validation != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(
                                if (validation.passesAllRules) Icons.Filled.Verified else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (validation.passesAllRules) BrandTheme.green else BrandTheme.orange,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                if (validation.passesAllRules) "기준 통과" else "확인 필요",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (validation.passesAllRules) BrandTheme.green else BrandTheme.orange,
                                modifier = Modifier.testTag("preview.validation")
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${post.characterCount} / ${validation.format.requiredCharacterCount}자",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("preview.characterCount")
                        )
                    }

                    if (!validation.passesAllRules) {
                        Text(
                            validation.failedRuleDescriptions.joinToString(" · "),
                            fontSize = 11.sp,
                            color = BrandTheme.orange,
                            lineHeight = 15.sp
                        )
                    }
                }

                SelectionContainer {
                    Text(
                        post.composedText,
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BrandTheme.paper, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                            .testTag("preview.text")
                    )
                }

                // Instagram으로 공유
                val shareEnabled = !state.isPreparingShare && !state.isGenerating
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(
                            BrandTheme.accent.copy(alpha = if (shareEnabled) 1f else 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = shareEnabled) {
                            scope.launch {
                                val intent = viewModel.prepareShare(post, context)
                                if (intent != null) {
                                    context.startActivity(
                                        android.content.Intent.createChooser(intent, null)
                                    )
                                }
                            }
                        }
                        .testTag("preview.share"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.isPreparingShare) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (state.isPreparingShare) "준비 중" else "Instagram으로 →",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        if (state.shareMessageIsError) Icons.Filled.Warning else Icons.Outlined.Info,
                        contentDescription = null,
                        tint = if (state.shareMessageIsError) BrandTheme.red else BrandTheme.labelSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        state.shareMessage ?: "문구는 자동 복사됩니다",
                        fontSize = 12.sp,
                        color = if (state.shareMessageIsError) BrandTheme.red else BrandTheme.labelSecondary,
                        modifier = Modifier.testTag("preview.shareMessage")
                    )
                }
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = BrandTheme.labelSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "게시물을 만들면 여기에 표시됩니다",
                    fontSize = 15.sp,
                    color = BrandTheme.labelSecondary,
                    modifier = Modifier.testTag("preview.empty")
                )
            }
        }
    }
}

private fun sourceIcon(source: CaptionSource): ImageVector = when (source) {
    CaptionSource.DEVICE -> Icons.Filled.AutoAwesome
    CaptionSource.DETERMINISTIC -> Icons.Filled.Smartphone
    CaptionSource.CHAT_GPT -> Icons.Filled.Forum
    CaptionSource.GEMINI -> Icons.Outlined.Diamond
    CaptionSource.GROK -> Icons.Filled.Close
}

@Composable
private fun CandidateComparison(
    viewModel: ComposerViewModel,
    state: ComposerUiState,
    candidates: List<CaptionCandidate>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("결과 비교", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            candidates.forEach { candidate ->
                val report = viewModel.validationReport(candidate)
                val selected = state.activeCaptionSource == candidate.source
                val shape = RoundedCornerShape(14.dp)
                Column(
                    Modifier
                        .size(width = 220.dp, height = 112.dp)
                        .background(if (selected) BrandTheme.paper else BrandTheme.canvas, shape)
                        .border(
                            if (selected) 1.5.dp else 1.dp,
                            if (selected) BrandTheme.accent else BrandTheme.labelSecondary.copy(alpha = 0.2f),
                            shape
                        )
                        .clickable { viewModel.useCandidate(candidate) }
                        .padding(12.dp)
                        .testTag("preview.candidate.${candidate.source.name}"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            sourceIcon(candidate.source),
                            contentDescription = null,
                            tint = BrandTheme.labelPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(candidate.source.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Icon(
                            if (report.passesAllRules) Icons.Filled.Verified else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (report.passesAllRules) BrandTheme.green else BrandTheme.orange,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        candidate.post.previewSnippet,
                        fontSize = 12.sp,
                        color = BrandTheme.labelSecondary,
                        maxLines = 3,
                        lineHeight = 15.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${candidate.post.characterCount}자 · ${if (report.passesAllRules) "통과" else "확인 필요"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (report.passesAllRules) BrandTheme.green else BrandTheme.orange
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaPreview(items: List<ComposerMedia>, aspect: Float, isLoading: Boolean) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .heightIn(max = 420.dp)
            .background(BrandTheme.paper, shape)
            .border(1.dp, Color(0x2E3C3C43), shape)
            .clip(shape)
            .testTag("preview.media"),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(Modifier.size(24.dp), color = BrandTheme.accent, strokeWidth = 2.dp)
                Text("미디어 불러오는 중", fontSize = 13.sp, color = BrandTheme.labelSecondary)
            }
            items.isNotEmpty() -> {
                val pagerState = rememberPagerState(pageCount = { items.size })
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val media = items[page]
                    Box(Modifier.fillMaxSize()) {
                        if (media.kind == MediaKind.IMAGE) {
                            val bitmap = remember(media.id) {
                                runCatching {
                                    BitmapFactory.decodeByteArray(media.data, 0, media.data.size)
                                }.getOrNull()
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "사진",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Column(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.SmartDisplay,
                                    contentDescription = null,
                                    tint = BrandTheme.accent,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text("영상", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = BrandTheme.accent)
                            }
                        }
                        Text(
                            "${page + 1}/${items.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }
            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    tint = BrandTheme.labelSecondary,
                    modifier = Modifier.size(34.dp)
                )
                Text("불러오는 중", fontSize = 12.sp, color = BrandTheme.labelSecondary)
            }
        }
    }
}
