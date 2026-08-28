package com.armsone.starmanager.ui.composer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armsone.starmanager.design.BrandSectionTitle
import com.armsone.starmanager.design.BrandTheme
import com.armsone.starmanager.design.GlossyPrimaryButton
import com.armsone.starmanager.design.IconWell
import com.armsone.starmanager.design.IconWellVariant
import com.armsone.starmanager.design.LocalAppAppearance
import com.armsone.starmanager.design.StarAdaptiveSegmentedControl
import com.armsone.starmanager.design.StarSegmentedControl
import com.armsone.starmanager.design.StarSlider
import com.armsone.starmanager.design.oxbloodPreferenceCard
import com.armsone.starmanager.design.starCard
import com.armsone.starmanager.model.AppAppearance
import com.armsone.starmanager.model.AudienceAgeGroup
import com.armsone.starmanager.model.CreatorProfile
import com.armsone.starmanager.model.EmojiIntensity
import com.armsone.starmanager.model.LineBreakFrequency
import com.armsone.starmanager.model.PostDestination
import com.armsone.starmanager.model.PostLength
import com.armsone.starmanager.model.PostMood
import com.armsone.starmanager.model.PostStyle
import com.armsone.starmanager.model.PostTone
import com.armsone.starmanager.service.DirectAIProvider
import com.armsone.starmanager.ui.externalai.ExternalAIAutomationPhase
import com.armsone.starmanager.ui.externalai.ExternalAIErrorSanitizer
import com.armsone.starmanager.ui.externalai.ExternalAIFallbackClassifier
import com.armsone.starmanager.ui.externalai.ExternalAIFallbackReason
import com.armsone.starmanager.ui.externalai.ExternalAIPollResult
import com.armsone.starmanager.ui.externalai.ExternalAIScripts
import com.armsone.starmanager.ui.externalai.ExternalAISecurityPolicy
import com.armsone.starmanager.ui.externalai.ExternalAIStabilityReducer
import com.armsone.starmanager.ui.externalai.ExternalAIStabilityState
import com.armsone.starmanager.ui.externalai.ExternalAISurface
import com.armsone.starmanager.ui.externalai.ExternalAISurfaceMode
import com.armsone.starmanager.ui.externalai.ExternalAITimerFormatter
import com.armsone.starmanager.ui.externalai.ExternalAITimingProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ComposerScreen(viewModel: ComposerViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile by viewModel.profileStore.profile.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val appearance = LocalAppAppearance.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val dismissKeyboardOnUserScroll = remember(focusManager, keyboardController) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(state.lastImportSuccessToken) {
        if (state.lastImportSuccessToken > 0L) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    val maxSelection = maxOf(1, MediaAttachmentPolicy.availableSlots(state.mediaItems.size))
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = if (maxSelection < 2) 2 else maxSelection)
    ) { uris ->
        viewModel.addPickedMedia(uris.take(maxSelection), context.contentResolver)
    }

    var isMediaDropTargeted by remember { mutableStateOf(false) }
    val mediaDropTarget = remember(context, viewModel) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                isMediaDropTargeted = true
            }

            override fun onExited(event: DragAndDropEvent) {
                isMediaDropTargeted = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                isMediaDropTargeted = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                isMediaDropTargeted = false
                val androidEvent = event.toAndroidDragEvent()
                val clipData = androidEvent.clipData ?: return false
                val uris = buildList {
                    for (index in 0 until clipData.itemCount) {
                        clipData.getItemAt(index).uri?.let(::add)
                    }
                }
                if (uris.isEmpty()) return false

                val permissions = context.findActivity()?.requestDragAndDropPermissions(androidEvent)
                viewModel.addPickedMedia(
                    uris = uris,
                    resolver = context.contentResolver,
                    onFinished = { permissions?.release() }
                )
                return true
            }
        }
    }

    var showsContinuousCamera by rememberSaveable { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) showsContinuousCamera = true
    }

    if (showsContinuousCamera) {
        ContinuousCameraCapture(
            maxCount = MediaAttachmentPolicy.MAX_ITEMS,
            currentCount = state.mediaItems.size,
            onDone = { photos ->
                showsContinuousCamera = false
                scope.launch {
                    photos.forEach { bytes ->
                        val saved = withContext(Dispatchers.IO) { saveImageToGallery(context, bytes) }
                        viewModel.addCameraPhoto(bytes, gallerySaved = saved)
                    }
                }
            },
            onCancel = { showsContinuousCamera = false }
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BrandTheme.canvasBrush(appearance))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .nestedScroll(dismissKeyboardOnUserScroll)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .starCard(appearance)
                    .testTag("composer.creationCard"),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. 헤로 문구
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        "오늘 어떤 이야기를 전할까요?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTheme.labelPrimary(appearance),
                        lineHeight = 26.sp,
                        maxLines = 2
                    )
                    if (appearance == AppAppearance.BK) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 3.dp)
                                .background(BrandTheme.accent, RoundedCornerShape(1.5.dp))
                        )
                    }
                }

                // 2. 글쓰기 설정 카드 (CreatorProfileStore 바인딩)
                WritingSettingsCard(
                    viewModel = viewModel,
                    profile = profile,
                    appearance = appearance
                )

                // 3. 미디어 섹션 (미디어 우선 작문 흐름)
                val mediaSectionShape = RoundedCornerShape(14.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragAndDropTarget(
                            shouldStartDragAndDrop = { event ->
                                MediaAttachmentPolicy.availableSlots(state.mediaItems.size) > 0 &&
                                    event.mimeTypes().any { mime ->
                                        mime.startsWith("image/") || mime.startsWith("video/")
                                    }
                            },
                            target = mediaDropTarget
                        )
                        .then(
                            if (isMediaDropTargeted) {
                                Modifier.border(2.dp, BrandTheme.accent, mediaSectionShape)
                            } else {
                                Modifier
                            }
                        ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BrandSectionTitle(
                        title = "미디어",
                        icon = Icons.Filled.PhotoLibrary,
                        appearance = appearance
                    )
                    Text(
                        "미디어를 먼저 골라두면, 그에 어울리는 이야기를 적기 쉬워요. 미디어 없이 글부터 써도 괜찮아요.",
                        fontSize = 12.sp,
                        color = BrandTheme.labelSecondary(appearance)
                    )

                    StarSegmentedControl(
                        options = PreviewAspect.entries.map { it.title },
                        selectedIndex = PreviewAspect.entries.indexOf(state.previewAspect),
                        appearance = appearance,
                        onSelect = { viewModel.setPreviewAspect(PreviewAspect.entries[it]) },
                        modifier = Modifier.testTag("composer.aspect")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BorderedActionButton(
                            title = "미디어",
                            icon = if (state.mediaItems.isEmpty()) Icons.Filled.AddPhotoAlternate else Icons.Filled.CheckCircle,
                            enabled = !state.isLoadingMedia &&
                                MediaAttachmentPolicy.availableSlots(state.mediaItems.size) > 0,
                            appearance = appearance,
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
                            enabled = MediaAttachmentPolicy.availableSlots(state.mediaItems.size) > 0,
                            appearance = appearance,
                            onClick = {
                                val hasCamera = context.packageManager
                                    .hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
                                if (!hasCamera) {
                                    viewModel.cameraUnavailable()
                                } else {
                                    val permissions = buildList {
                                        add(Manifest.permission.CAMERA)
                                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }.filter {
                                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                                    }
                                    if (permissions.isEmpty()) showsContinuousCamera = true
                                    else cameraPermissionLauncher.launch(permissions.toTypedArray())
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

                    if (state.mediaItems.isNotEmpty() || state.isLoadingMedia) {
                        MediaPreview(state.mediaItems, state.previewAspect.ratio, state.isLoadingMedia)
                    }
                }

                // 4. 이야기 / 초안 단일 편집 필드 (생성/외부 결과도 동일 필드에 반영)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val source = state.activeCaptionSource
                    if (source != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                sourceIcon(source),
                                contentDescription = null,
                                tint = BrandTheme.labelSecondary(appearance),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                source.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = BrandTheme.labelSecondary(appearance)
                            )
                        }
                    }

                    IdeaField(
                        value = state.idea,
                        onValueChange = viewModel::setIdea,
                        isGenerating = state.isGenerating,
                        appearance = appearance,
                        modifier = Modifier.testTag("composer.idea")
                    )

                    // 실시간 검증 리포트
                    if (state.idea.trim().isNotEmpty()) {
                        val validation = viewModel.activeValidationReport()
                        if (validation != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
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
                                    "${validation.format.characterCount} / ${validation.format.destinationLimit}자",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandTheme.labelPrimary(appearance),
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
                    }
                }

                // 5. 스타일 요약 문구
                Row(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = BrandTheme.labelSecondary(appearance),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        "${profile.destination.title} · ${profile.mood.rawValue} · ${profile.style.title} · ${profile.tone.title} · 목표 ${profile.controls.characterCount}자",
                        fontSize = 13.sp,
                        color = BrandTheme.labelSecondary(appearance),
                        modifier = Modifier.testTag("composer.styleSummary")
                    )
                }

                // 6. AI 생성 버튼 (Gemini, ChatGPT, Claude, 기기 AI)
                AiChoiceButtons(viewModel, state)

                // 7. 공유 버튼 (게시 대상 일반화 라벨, 현재 편집된 텍스트 공유)
                if (state.idea.trim().isNotEmpty()) {
                    val shareEnabled = !state.isPreparingShare && !state.isGenerating
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlossyPrimaryButton(
                            onClick = {
                                scope.launch {
                                    val intent = viewModel.prepareShare(context)
                                    if (intent != null) {
                                        val chooser = Intent.createChooser(intent, null).apply {
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(chooser)
                                    }
                                }
                            },
                            enabled = shareEnabled,
                            appearance = appearance,
                            modifier = Modifier.testTag("preview.share")
                        ) {
                            if (state.isPreparingShare) {
                                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                if (state.isPreparingShare) "준비 중" else "${profile.destination.title}으로 →",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (state.shareMessageIsError) Icons.Filled.Warning else Icons.Outlined.Info,
                                contentDescription = null,
                                tint = if (state.shareMessageIsError) BrandTheme.red else BrandTheme.labelSecondary(appearance),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                state.shareMessage ?: "문구는 자동 복사됩니다",
                                fontSize = 12.sp,
                                color = if (state.shareMessageIsError) BrandTheme.red else BrandTheme.labelSecondary(appearance),
                                modifier = Modifier.testTag("preview.shareMessage")
                            )
                        }
                    }
                }

                // 상태 또는 에러 메시지
                val message = state.errorMessage ?: state.statusMessage
                if (message != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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
    }
}

// MARK: - 글쓰기 설정 카드

@Composable
private fun WritingSettingsCard(
    viewModel: ComposerViewModel,
    profile: CreatorProfile,
    appearance: AppAppearance
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .oxbloodPreferenceCard(appearance),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BrandSectionTitle(
            title = "글쓰기 설정",
            icon = Icons.Filled.Tune,
            appearance = appearance,
            variant = IconWellVariant.OXBLOOD
        )

        // 게시할 곳
        SettingsRow(title = "게시할 곳", icon = Icons.Filled.Send, appearance = appearance) {
            StarAdaptiveSegmentedControl(
                options = PostDestination.entries.map { it.title },
                selectedIndex = PostDestination.entries.indexOf(profile.destination),
                onSelect = { index ->
                    viewModel.profileStore.updateProfile { it.copy(destination = PostDestination.entries[index]) }
                },
                appearance = appearance,
                modifier = Modifier.testTag("settings.destination")
            )
            Text(
                profile.destination.limitBasisDescription,
                fontSize = 11.sp,
                color = BrandTheme.labelSecondary(appearance)
            )
        }

        // 목표 글자 수
        val maxAllowed = minOf(500, profile.destination.characterLimit)
        SettingsRow(title = "글자 수", icon = Icons.Filled.FormatSize, appearance = appearance) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${profile.controls.characterCount}자",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandTheme.labelPrimary(appearance)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "50~${maxAllowed}자",
                    fontSize = 12.sp,
                    color = BrandTheme.labelSecondary(appearance)
                )
            }
            StarSlider(
                value = profile.controls.characterCount.toFloat().coerceIn(50f, maxAllowed.toFloat()),
                onValueChange = { raw ->
                    val rounded = (Math.round(raw / 10f) * 10).coerceIn(50, maxAllowed)
                    viewModel.setCharacterCount(rounded)
                },
                valueRange = 50f..maxAllowed.toFloat(),
                step = 10f,
                appearance = appearance,
                modifier = Modifier.testTag("settings.slider.characterCount")
            )
        }

        // 이모지 사용
        SettingsRow(title = "이모지 사용", icon = Icons.Filled.EmojiEmotions, appearance = appearance) {
            StarAdaptiveSegmentedControl(
                options = EmojiIntensity.entries.map { it.title },
                selectedIndex = EmojiIntensity.entries.indexOf(profile.emojiIntensity),
                onSelect = { index ->
                    viewModel.profileStore.updateProfile { it.copy(emojiIntensity = EmojiIntensity.entries[index]) }
                },
                appearance = appearance,
                modifier = Modifier.testTag("settings.emojiIntensity")
            )
        }

        // 분위기
        SettingsRow(title = "분위기", icon = Icons.Filled.WbSunny, appearance = appearance) {
            StarAdaptiveSegmentedControl(
                options = PostMood.entries.map { it.rawValue },
                selectedIndex = PostMood.entries.indexOf(profile.mood),
                onSelect = { index ->
                    viewModel.setMood(PostMood.entries[index])
                },
                appearance = appearance,
                modifier = Modifier.testTag("settings.mood")
            )
        }

        // 스타일
        SettingsRow(title = "스타일", icon = Icons.Filled.MenuBook, appearance = appearance) {
            StarAdaptiveSegmentedControl(
                options = PostStyle.entries.map { it.title },
                selectedIndex = PostStyle.entries.indexOf(profile.style),
                onSelect = { index ->
                    viewModel.profileStore.updateProfile { it.copy(style = PostStyle.entries[index]) }
                },
                appearance = appearance,
                modifier = Modifier.testTag("settings.style")
            )
        }

        // 말투
        SettingsRow(title = "말투", icon = Icons.Filled.ChatBubble, appearance = appearance) {
            StarAdaptiveSegmentedControl(
                options = PostTone.entries.map { it.title },
                selectedIndex = PostTone.entries.indexOf(profile.tone),
                onSelect = { index ->
                    viewModel.profileStore.updateProfile { it.copy(tone = PostTone.entries[index]) }
                },
                appearance = appearance,
                modifier = Modifier.testTag("settings.tone")
            )
        }

        // 나잇대
        SettingsRow(title = "나잇대", icon = Icons.Filled.AccessTime, appearance = appearance) {
            StarAdaptiveSegmentedControl(
                options = AudienceAgeGroup.entries.map { it.title },
                selectedIndex = AudienceAgeGroup.entries.indexOf(profile.ageGroup),
                onSelect = { index ->
                    viewModel.profileStore.updateProfile { it.copy(ageGroup = AudienceAgeGroup.entries[index]) }
                },
                appearance = appearance,
                modifier = Modifier.testTag("settings.ageGroup")
            )
            Text(
                "나잇대는 프롬프트 힌트로만 쓰이고 다른 설정을 바꾸지 않아요.",
                fontSize = 11.sp,
                color = BrandTheme.labelSecondary(appearance)
            )
        }

        // 줄넘김
        SettingsRow(title = "줄넘김", icon = Icons.Filled.KeyboardReturn, appearance = appearance) {
            StarAdaptiveSegmentedControl(
                options = LineBreakFrequency.entries.map { it.title },
                selectedIndex = LineBreakFrequency.entries.indexOf(profile.lineBreakFrequency),
                onSelect = { index ->
                    viewModel.profileStore.updateProfile { it.copy(lineBreakFrequency = LineBreakFrequency.entries[index]) }
                },
                appearance = appearance,
                modifier = Modifier.testTag("settings.lineBreakFrequency")
            )
        }

        // 내 글 반영
        SettingsRow(title = "내 글 반영", icon = Icons.Filled.Edit, appearance = appearance) {
            StarAdaptiveSegmentedControl(
                options = PostLength.entries.map { it.storyWeightTitle },
                selectedIndex = PostLength.entries.indexOf(profile.preferredLength),
                onSelect = { index ->
                    viewModel.setLength(PostLength.entries[index])
                },
                appearance = appearance,
                modifier = Modifier.testTag("settings.length")
            )
            Text(
                profile.preferredLength.storyWeightExplanation,
                fontSize = 11.sp,
                color = BrandTheme.labelSecondary(appearance)
            )
        }

        // 주로 쓰는 주제
        SettingsRow(title = "주로 쓰는 주제", icon = Icons.Filled.Sell, appearance = appearance) {
            SingleLineInputRow(
                value = profile.accountTopic,
                placeholder = "예: 카페 창업 일지",
                onValueChange = { viewModel.profileStore.updateProfile { p -> p.copy(accountTopic = it) } },
                appearance = appearance,
                testTag = "settings.accountTopic"
            )
        }

        // 읽을 사람
        SettingsRow(title = "읽을 사람", icon = Icons.Filled.Group, appearance = appearance) {
            SingleLineInputRow(
                value = profile.audience,
                placeholder = "예: 오픈 예정 매장 팔로워",
                onValueChange = { viewModel.profileStore.updateProfile { p -> p.copy(audience = it) } },
                appearance = appearance,
                testTag = "settings.audience"
            )
        }

        // 금지 표현
        SettingsRow(title = "금지 표현", icon = Icons.Filled.Block, appearance = appearance) {
            SingleLineInputRow(
                value = profile.prohibitedPhrases,
                placeholder = "쉼표로 구분해 적어 주세요",
                onValueChange = { viewModel.profileStore.updateProfile { p -> p.copy(prohibitedPhrases = it) } },
                appearance = appearance,
                testTag = "settings.prohibitedPhrases"
            )
        }

        // 해시태그 취향
        SettingsRow(title = "해시태그 취향", icon = Icons.Filled.Tag, appearance = appearance) {
            SingleLineInputRow(
                value = profile.hashtagStyle,
                placeholder = "예: 핵심 키워드 중심",
                onValueChange = { viewModel.profileStore.updateProfile { p -> p.copy(hashtagStyle = it) } },
                appearance = appearance,
                testTag = "settings.hashtagStyle"
            )
        }

        // 추가로 하고 싶은 설정 (여러 줄)
        SettingsRow(title = "추가로 하고 싶은 설정", icon = Icons.Filled.RateReview, appearance = appearance) {
            MultilineInputRow(
                value = profile.detailedGuidelines,
                placeholder = "예: 이모티콘 대신 물결표를 즐겨 써줘 / 문장은 짧게 끊어줘",
                onValueChange = { viewModel.profileStore.updateProfile { p -> p.copy(detailedGuidelines = it) } },
                minLines = 3,
                maxLines = 8,
                appearance = appearance,
                testTag = "settings.detailedGuidelines"
            )
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    icon: ImageVector,
    appearance: AppAppearance,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandTheme.labelPrimary(appearance),
                modifier = Modifier.size(15.dp)
            )
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandTheme.labelPrimary(appearance)
            )
        }
        content()
    }
}

@Composable
private fun SingleLineInputRow(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    appearance: AppAppearance,
    testTag: String
) {
    val isBk = appearance == AppAppearance.BK
    val shape = RoundedCornerShape(10.dp)
    val bg = if (isBk) Color.White else BrandTheme.surface
    val border = BorderStroke(0.8.dp, if (isBk) Color(0xFFD6DAE0) else BrandTheme.border)

    Box(
        Modifier
            .fillMaxWidth()
            .border(border, shape)
            .background(bg, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, fontSize = 14.sp, color = BrandTheme.labelSecondary(appearance))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 14.sp, color = BrandTheme.labelPrimary(appearance)),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

@Composable
private fun MultilineInputRow(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 3,
    maxLines: Int = 8,
    appearance: AppAppearance,
    testTag: String
) {
    val isBk = appearance == AppAppearance.BK
    val shape = RoundedCornerShape(10.dp)
    val bg = if (isBk) Color.White else BrandTheme.surface
    val border = BorderStroke(0.8.dp, if (isBk) Color(0xFFD6DAE0) else BrandTheme.border)

    Box(
        Modifier
            .fillMaxWidth()
            .border(border, shape)
            .background(bg, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                fontSize = 14.sp,
                color = BrandTheme.labelSecondary(appearance),
                lineHeight = 19.sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 14.sp, color = BrandTheme.labelPrimary(appearance), lineHeight = 19.sp),
            minLines = minLines,
            maxLines = maxLines,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

// MARK: - 단일 이야기 / 초안 텍스트 필드

@Composable
private fun IdeaField(
    value: String,
    onValueChange: (String) -> Unit,
    isGenerating: Boolean = false,
    modifier: Modifier = Modifier,
    appearance: AppAppearance = LocalAppAppearance.current
) {
    val isBk = appearance == AppAppearance.BK
    val shape = RoundedCornerShape(14.dp)
    val bg = if (isBk) Color(0xFFF6F8FA) else BrandTheme.canvas
    val border = if (isBk) BorderStroke(1.dp, Color(0xFFE2E6EC)) else null

    Box(
        modifier
            .fillMaxWidth()
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .background(bg, shape)
            .padding(14.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                "이야기를 적어 주세요 · AI 결과도 여기에 채워져요",
                fontSize = 16.sp,
                color = BrandTheme.labelSecondary(appearance),
                lineHeight = 22.sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = !isGenerating,
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = BrandTheme.labelPrimary(appearance),
                lineHeight = 23.sp
            ),
            minLines = 3,
            maxLines = 20,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BorderedActionButton(
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    appearance: AppAppearance = LocalAppAppearance.current
) {
    val isBk = appearance == AppAppearance.BK
    val shape = RoundedCornerShape(12.dp)
    val bg = if (isBk) {
        if (enabled) Color(0xFFF1F3F6) else Color(0xFFF6F7F9)
    } else {
        BrandTheme.accent.copy(alpha = if (enabled) 0.12f else 0.06f)
    }
    val borderStroke = if (isBk) {
        BorderStroke(1.dp, if (enabled) Color(0xFFD8DCE3) else Color(0xFFE5E8EE))
    } else null
    val contentColor = if (isBk) {
        if (enabled) BrandTheme.bkCarbonDark else BrandTheme.bkLabelSecondary
    } else {
        BrandTheme.accent.copy(alpha = if (enabled) 1f else 0.4f)
    }

    Row(
        modifier = modifier
            .height(44.dp)
            .then(if (borderStroke != null) Modifier.border(borderStroke, shape) else Modifier)
            .background(bg, shape)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

// MARK: - AI 선택 버튼

@Composable
private fun AiChoiceButtons(viewModel: ComposerViewModel, state: ComposerUiState) {
    val context = LocalContext.current
    val appearance = LocalAppAppearance.current
    val showsExternalAIBrowser by viewModel.profileStore.showsExternalAIBrowser.collectAsStateWithLifecycle()
    val isEnabled = !state.isGenerating

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val columns = if (LocalDensity.current.fontScale >= 1.3f) 2 else 4
        AIChoice.all.chunked(columns).forEach { rowChoices ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowChoices.forEach { choice ->
                    AIChoiceCard(
                        choice = choice,
                        enabled = isEnabled,
                        onClick = {
                            when (choice) {
                                AIChoice.OnDevice -> viewModel.generateDraft()
                                is AIChoice.External -> viewModel.startExternalGeneration(choice.provider)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("composer.ai.${choice.id}")
                    )
                }
                repeat(columns - rowChoices.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        if (state.isGenerating) {
            GenerationStatusCard(
                viewModel = viewModel,
                state = state,
                appearance = appearance
            )
        }

        val pendingProvider = state.pendingExternalProvider
        if (pendingProvider != null && !state.isGenerating) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(BrandTheme.accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .clickable {
                        val text = viewModel.readClipboard(context)
                        viewModel.importAIResult(text, pendingProvider, context)
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

        if (viewModel.hasRepresentativePhoto) {
            Text(
                "대표 사진은 외부 AI 버튼을 누를 때만 함께 보내요.",
                fontSize = 11.sp,
                color = BrandTheme.labelSecondary(appearance),
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }

    // 설정이 꺼져 있으면 백그라운드 WebView, 켜져 있으면 보이는 WebView에서 수행한다.
    val automationProvider = state.activeAutomationProvider
    if (
        state.isGenerating &&
        automationProvider != null &&
        !showsExternalAIBrowser &&
        !state.isFallbackBrowserVisible
    ) {
        HiddenExternalAIWebView(
            provider = automationProvider,
            prompt = viewModel.externalPrompt(),
            requestId = state.automationRequestId,
            onSubmitted = {
                viewModel.onAutomationSubmitted()
            },
            onFallbackRequired = { reason ->
                viewModel.onAutomationFallbackRequired(reason)
            },
            onError = { err ->
                viewModel.onAutomationError(err, automationProvider)
            },
            onSuccess = { answer ->
                viewModel.importAIResult(answer, automationProvider, context)
            }
        )
    }

    // 사용자가 항상 보기를 선택했거나 상호작용 폴백이 필요할 때 보이는 표면.
    if (
        state.isGenerating &&
        automationProvider != null &&
        (showsExternalAIBrowser || state.isFallbackBrowserVisible)
    ) {
        ExternalAISurface(
            provider = automationProvider,
            mode = ExternalAISurfaceMode.GENERATION,
            fallbackReason = state.fallbackReason,
            prompt = viewModel.externalPrompt(),
            onClose = {
                if (showsExternalAIBrowser) {
                    if (viewModel.state.value.isGenerating) {
                        viewModel.cancelGeneration()
                    }
                } else {
                    viewModel.dismissFallbackBrowser()
                }
            },
            onSubmitted = viewModel::onAutomationSubmitted,
            onError = { err ->
                viewModel.onAutomationError(err, automationProvider)
            },
            autoImportOnComplete = true,
            onImport = { text ->
                viewModel.importAIResult(text, automationProvider, context)
            },
            appearance = appearance
        )
    }
}

@Composable
private fun GenerationStatusCard(
    viewModel: ComposerViewModel,
    state: ComposerUiState,
    appearance: AppAppearance = LocalAppAppearance.current
) {
    val isBk = appearance == AppAppearance.BK
    val shape = RoundedCornerShape(14.dp)
    val bg = if (isBk) Color(0xFFF1F3F6) else BrandTheme.paper
    val border = BorderStroke(1.dp, if (isBk) Color(0xFFE2E6EC) else BrandTheme.border)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(border, shape)
            .background(bg, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("composer.statusCard"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = BrandTheme.accent,
            strokeWidth = 2.5.dp
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                state.automationStepTitle ?: "글을 만드는 중…",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandTheme.labelPrimary(appearance),
                maxLines = 1
            )
            Text(
                state.automationStepSubtitle ?: "잠시만 기다려 주세요",
                fontSize = 13.sp,
                color = BrandTheme.labelSecondary(appearance),
                maxLines = 1
            )
            if (state.automationPhase == ExternalAIAutomationPhase.SUBMITTED ||
                state.automationPhase == ExternalAIAutomationPhase.WAITING_ELAPSED
            ) {
                LinearProgressIndicator(
                    progress = { ExternalAITimerFormatter.progress(state.automationElapsedSeconds) },
                    modifier = Modifier.fillMaxWidth(),
                    color = BrandTheme.accent
                )
            }
        }

        TextButton(
            onClick = { viewModel.cancelGeneration() },
            modifier = Modifier.testTag("composer.ai.cancel")
        ) {
            Text("취소", fontSize = 14.sp, color = BrandTheme.labelSecondary(appearance))
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HiddenExternalAIWebView(
    provider: DirectAIProvider,
    prompt: String,
    requestId: Int,
    onSubmitted: () -> Unit,
    onFallbackRequired: (ExternalAIFallbackReason) -> Unit,
    onError: (String?) -> Unit,
    onSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    var webViewRef by remember(requestId) { mutableStateOf<WebView?>(null) }
    var isAutomationActive by remember(requestId) { mutableStateOf(true) }
    val timings = ExternalAITimingProfile.DEFAULT

    DisposableEffect(requestId, provider) {
        val hostView = context.findActivity()?.window?.decorView as? ViewGroup
        val density = context.resources.displayMetrics.density
        val wv = WebView(context).apply {
            alpha = 0.001f
            isClickable = false
            isFocusable = false
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString = userAgentString.replace("; wv", "")
            }
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val targetUrl = request?.url?.toString()
                    if (!ExternalAISecurityPolicy.isAllowedUrl(targetUrl, provider)) {
                        isAutomationActive = false
                        onFallbackRequired(ExternalAIFallbackReason.NAVIGATION_DISALLOWED)
                        return true
                    }
                    val fallback = ExternalAIFallbackClassifier.classifyUrl(targetUrl)
                    if (fallback != null) {
                        isAutomationActive = false
                        onFallbackRequired(fallback)
                        return true
                    }
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    cookieManager.flush()

                    val fallback = ExternalAIFallbackClassifier.classifyUrl(url)
                    if (fallback != null) {
                        isAutomationActive = false
                        onFallbackRequired(fallback)
                        return
                    }

                    if (ExternalAISecurityPolicy.isAuthOrigin(url, provider)) {
                        isAutomationActive = false
                        onFallbackRequired(ExternalAIFallbackReason.LOGIN_REQUIRED)
                        return
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        val desc = error?.description?.toString() ?: "연결 실패"
                        val sanitized = ExternalAIErrorSanitizer.sanitize("Network error: $desc", provider)
                        onError(sanitized)
                    }
                }
            }
        }

        val lp = FrameLayout.LayoutParams(
            (412 * density).toInt(),
            (892 * density).toInt()
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            setMargins(-10000, -10000, 0, 0)
        }

        hostView?.addView(wv, lp)
        webViewRef = wv
        wv.loadUrl(provider.url)

        onDispose {
            isAutomationActive = false
            wv.stopLoading()
            hostView?.removeView(wv)
            wv.destroy()
            webViewRef = null
        }
    }

    LaunchedEffect(requestId, provider) {
        val wv = webViewRef ?: return@LaunchedEffect
        val startTime = System.currentTimeMillis()
        var baselineCaptured = false
        var baselineCount = 0
        var promptInjected = false
        var promptSubmitted = false

        while (isAutomationActive && System.currentTimeMillis() - startTime < timings.visibleAutoFillTimeoutMs) {
            val url = wv.url
            if (ExternalAISecurityPolicy.canInjectScript(url, provider)) {
                if (!baselineCaptured) {
                    val baselineRes = suspendCancellableCoroutine<String?> { cont ->
                        wv.evaluateJavascript(ExternalAIScripts.recordBaselineScript(provider)) { cont.resume(it) }
                    }
                    baselineCount = ExternalAIScripts.parseBaselineCount(baselineRes)
                    baselineCaptured = true
                }

                if (!promptInjected) {
                    val injectRes = suspendCancellableCoroutine<String?> { cont ->
                        wv.evaluateJavascript(ExternalAIScripts.injectPromptScript(provider, prompt, force = false)) { cont.resume(it) }
                    }
                    val injection = ExternalAIScripts.parseInjectionResult(injectRes)
                    if (injection.success && injection.inputFound) {
                        promptInjected = true
                    }
                }

                if (promptInjected && !promptSubmitted) {
                    delay(350L)
                    val submitRes = suspendCancellableCoroutine<String?> { cont ->
                        wv.evaluateJavascript(ExternalAIScripts.submitPromptScript(provider, 1)) { cont.resume(it) }
                    }
                    delay(700L)
                    val verifyRes = suspendCancellableCoroutine<String?> { cont ->
                        wv.evaluateJavascript(ExternalAIScripts.verifySubmissionScript(provider, baselineCount)) { cont.resume(it) }
                    }
                    if (ExternalAIScripts.parseSubmissionVerified(verifyRes)) {
                        promptSubmitted = true
                        onSubmitted()
                        break
                    }
                }
            }
            delay(timings.observationCadenceMs)
        }

        if (!promptSubmitted && isAutomationActive) {
            onFallbackRequired(ExternalAIFallbackReason.MANUAL_CONFIRMATION)
            return@LaunchedEffect
        }

        // 응답 관찰 루프
        var stabilityState = ExternalAIStabilityState()
        while (isAutomationActive) {
            delay(timings.observationCadenceMs)
            val errorRes = suspendCancellableCoroutine<String?> { cont ->
                wv.evaluateJavascript(ExternalAIScripts.extractErrorScript()) { cont.resume(it) }
            }
            val domError = ExternalAIScripts.parseErrorResult(errorRes)
            if (domError.hasError && !domError.error.isNullOrBlank()) {
                val sanitized = ExternalAIErrorSanitizer.sanitize(domError.error, provider)
                onError(sanitized)
                return@LaunchedEffect
            }

            val answerRes = suspendCancellableCoroutine<String?> { cont ->
                wv.evaluateJavascript(ExternalAIScripts.extractAnswerScript(provider)) { cont.resume(it) }
            }
            val poll = ExternalAIScripts.parsePollResult(answerRes)
            val nextStability = ExternalAIStabilityReducer.step(stabilityState, poll)
            stabilityState = nextStability

            if (nextStability.isStable && nextStability.stableAnswer != null) {
                onSuccess(nextStability.stableAnswer)
                return@LaunchedEffect
            }
        }
    }
}

// MARK: - 미디어 순서 편집기

@Composable
private fun MediaOrderEditor(viewModel: ComposerViewModel, state: ComposerUiState) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val itemSlotPx = with(density) { (104 + 10).dp.toPx() }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            state.mediaItems.forEachIndexed { index, media ->
                Box(
                    modifier = Modifier
                        .then(
                            if (draggingId == media.id) {
                                Modifier
                                    .graphicsLayer { translationX = dragOffset }
                                    .shadow(8.dp, RoundedCornerShape(14.dp))
                            } else Modifier
                        )
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun MediaThumbnail(media: ComposerMedia) {
    val context = LocalContext.current
    val appearance = LocalAppAppearance.current
    val isBk = appearance == AppAppearance.BK
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
            .background(if (isBk) Color(0xFFF1F3F6) else BrandTheme.paper, shape)
            .border(1.dp, if (isBk) Color(0xFFE2E6EC) else Color.Black.copy(alpha = 0.09f), shape)
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

private fun saveImageToGallery(context: Context, bytes: ByteArray): Boolean {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
    }
    val resolver = context.contentResolver
    val filename = "IMG_${System.currentTimeMillis()}.jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = runCatching {
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }.getOrNull() ?: return false

    return try {
        val written = resolver.openOutputStream(uri)?.use { stream ->
            stream.write(bytes)
            stream.flush()
            true
        } ?: false
        if (!written) {
            runCatching { resolver.delete(uri, null, null) }
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            val updated = resolver.update(uri, values, null, null)
            if (updated == 0) {
                runCatching { resolver.delete(uri, null, null) }
                return false
            }
        }
        true
    } catch (_: Exception) {
        runCatching { resolver.delete(uri, null, null) }
        false
    }
}

@Composable
private fun MediaPreview(items: List<ComposerMedia>, aspect: Float, isLoading: Boolean) {
    val appearance = LocalAppAppearance.current
    val isBk = appearance == AppAppearance.BK
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .heightIn(max = 420.dp)
            .background(if (isBk) Color(0xFFF1F3F6) else BrandTheme.paper, shape)
            .border(1.dp, if (isBk) Color(0xFFE2E6EC) else Color(0x2E3C3C43), shape)
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
                Text("미디어 불러오는 중", fontSize = 13.sp, color = BrandTheme.labelSecondary(appearance))
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
                    tint = BrandTheme.labelSecondary(appearance),
                    modifier = Modifier.size(34.dp)
                )
                Text("불러오는 중", fontSize = 12.sp, color = BrandTheme.labelSecondary(appearance))
            }
        }
    }
}

private fun sourceIcon(source: CaptionSource): ImageVector = when (source) {
    CaptionSource.DEVICE -> Icons.Filled.Smartphone
    CaptionSource.DETERMINISTIC -> Icons.Filled.Smartphone
    CaptionSource.GEMINI -> Icons.Outlined.Diamond
    CaptionSource.CHAT_GPT -> Icons.Filled.Forum
    CaptionSource.CLAUDE -> Icons.Filled.AutoAwesome
    CaptionSource.GROK -> Icons.Filled.Close
}

@Composable
private fun AIChoiceCard(
    choice: AIChoice,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    appearance: AppAppearance = LocalAppAppearance.current
) {
    val isBk = appearance == AppAppearance.BK
    val shape = RoundedCornerShape(if (isBk) 14.dp else 12.dp)
    val bg = if (isBk) {
        if (enabled) Color.White else Color(0xFFF6F7F9)
    } else {
        if (enabled) BrandTheme.paper else BrandTheme.canvas
    }
    val border = BorderStroke(
        1.dp,
        if (isBk) {
            if (enabled) Color(0xFFD8DCE3) else Color(0xFFE5E8EE)
        } else {
            BrandTheme.border
        }
    )

    Column(
        modifier = modifier
            .heightIn(min = 60.dp)
            .border(border, shape)
            .background(bg, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        IconWell(
            icon = choiceIcon(choice),
            appearance = appearance,
            variant = if (isBk) IconWellVariant.CARBON else IconWellVariant.CARBON,
            size = 28.dp,
            iconSize = 16.dp
        )
        Text(
            choice.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) BrandTheme.labelPrimary(appearance) else BrandTheme.labelSecondary(appearance)
        )
    }
}

private fun choiceIcon(choice: AIChoice): ImageVector = when (choice) {
    AIChoice.OnDevice -> Icons.Filled.Smartphone
    is AIChoice.External -> when (choice.provider) {
        DirectAIProvider.GEMINI -> Icons.Outlined.Diamond
        DirectAIProvider.OPEN_AI -> Icons.Filled.Forum
        DirectAIProvider.CLAUDE -> Icons.Filled.AutoAwesome
        DirectAIProvider.GROK -> Icons.Filled.Close
    }
}
