package com.armsone.starmanager

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armsone.starmanager.design.BrandTheme
import com.armsone.starmanager.design.LocalAppAppearance
import com.armsone.starmanager.model.AppAppearance
import com.armsone.starmanager.model.CreatorProfileStore
import com.armsone.starmanager.model.SharedPreferencesKeyValueStore
import com.armsone.starmanager.update.DirectUpdateManager
import com.armsone.starmanager.ui.composer.ComposerScreen
import com.armsone.starmanager.ui.composer.ComposerViewModel
import com.armsone.starmanager.ui.settings.ProfileSettingsScreen

class MainActivity : ComponentActivity() {

    private val composerViewModel: ComposerViewModel by viewModels()
    private lateinit var profileStore: CreatorProfileStore

    private val automationPickerTrigger = mutableIntStateOf(0)
    private val cameraShortcutTrigger = mutableIntStateOf(0)

    private var hasStartedBefore = false
    private var lastStopElapsedRealtimeMs = 0L
    private var incomingExplicitIntentHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 디버그 전용 결정적 픽스처 훅 — 릴리스 빌드에서는 인텐트를 읽지 않는다.
        if (BuildConfig.DEBUG) {
            if (intent.getBooleanExtra(FixtureHooks.EXTRA_ZERO_DELAY, false)) {
                FixtureHooks.captionDelayMillis = 0L
            }
            if (intent.getBooleanExtra(FixtureHooks.EXTRA_RESET_STATE, false)) {
                FixtureHooks.resetStateRequested = true
            }
        }

        val storage = SharedPreferencesKeyValueStore(applicationContext)
        if (BuildConfig.DEBUG && FixtureHooks.resetStateRequested) {
            storage.remove(CreatorProfileStore.STORAGE_KEY)
            storage.remove(CreatorProfileStore.PRESETS_STORAGE_KEY)
            storage.remove(CreatorProfileStore.DEFAULT_STYLE_VERSION_KEY)
            storage.remove(CreatorProfileStore.APPEARANCE_STORAGE_KEY)
            storage.remove(CreatorProfileStore.SHOW_EXTERNAL_AI_BROWSER_STORAGE_KEY)
            storage.remove(CreatorProfileStore.AUTOMATION_ENABLED_STORAGE_KEY)
            storage.remove(ComposerViewModel.KEY_PASTE_GUIDANCE_SHOWN)
            FixtureHooks.resetStateRequested = false
        }
        profileStore = CreatorProfileStore(storage)
        composerViewModel.profileStore = profileStore
        DirectUpdateManager.get(applicationContext).start()

        handleIncomingIntent(intent)

        setContent {
            val appearance by profileStore.appearance.collectAsStateWithLifecycle()
            LaunchedEffect(appearance) {
                updateLauncherIcon(appearance)
            }
            val automationTrigger by automationPickerTrigger
            val cameraTrigger by cameraShortcutTrigger
            CompositionLocalProvider(LocalAppAppearance provides appearance) {
                StarManagerTheme(appearance = appearance) {
                    StarManagerApp(
                        profileStore = profileStore,
                        composerViewModel = composerViewModel,
                        automationPickerTrigger = automationTrigger,
                        cameraShortcutTrigger = cameraTrigger
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        val now = SystemClock.elapsedRealtime()
        val isColdStart = !hasStartedBefore
        val returnedAfterThreshold = hasStartedBefore &&
            (now - lastStopElapsedRealtimeMs) >= ORDINARY_AUTOMATION_RETURN_THRESHOLD_MS
        hasStartedBefore = true
        val explicitIntentHandled = incomingExplicitIntentHandled
        incomingExplicitIntentHandled = false
        if (
            (isColdStart || returnedAfterThreshold) &&
            !explicitIntentHandled &&
            ::profileStore.isInitialized &&
            profileStore.automationEnabled.value
        ) {
            automationPickerTrigger.value += 1
        }
    }

    override fun onStop() {
        super.onStop()
        lastStopElapsedRealtimeMs = SystemClock.elapsedRealtime()
    }

    /** 외부 공유(SEND/SEND_MULTIPLE)와 카메라 런처 숏컷은 자동화 설정과 무관하게 항상 동작한다. */
    private fun handleIncomingIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> {
                incomingExplicitIntentHandled = true
                handleShareIntent(intent)
            }
            ACTION_CAMERA_CAPTURE -> {
                incomingExplicitIntentHandled = true
                cameraShortcutTrigger.value += 1
            }
        }
    }

    private fun handleShareIntent(intent: Intent) {
        val mimeType = intent.type
        if (mimeType == null || !mimeType.startsWith("image/")) return
        val uris: List<Uri> = when (intent.action) {
            Intent.ACTION_SEND ->
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?.let { listOf(it) }
                    ?: emptyList()
            Intent.ACTION_SEND_MULTIPLE ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?: emptyList()
            else -> emptyList()
        }.take(8)
        if (uris.isEmpty()) return

        lifecycleScope.launch {
            val images = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    runCatching {
                        val type = contentResolver.getType(uri)
                        if (type != null && !type.startsWith("image/")) return@runCatching null
                        contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }.getOrNull()
                }
            }
            if (images.isNotEmpty()) {
                composerViewModel.startPhotoAutomation(images)
            }
        }
    }

    companion object {
        private const val ACTION_CAMERA_CAPTURE = "com.armsone.starmanager.action.CAMERA_CAPTURE"
        private const val ORDINARY_AUTOMATION_RETURN_THRESHOLD_MS = 15_000L
    }

    private fun updateLauncherIcon(appearance: AppAppearance) {
        val selected = when (appearance) {
            AppAppearance.CLASSIC -> "ClassicIconAlias"
            AppAppearance.INTERSTELLAR -> "InterstellarIconAlias"
            AppAppearance.BK -> "BkIconAlias"
        }
        val allAliases = listOf("BkIconAlias", "ClassicIconAlias", "InterstellarIconAlias")
        packageManager.setComponentEnabledSetting(
            ComponentName(packageName, "$packageName.$selected"),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        for (alias in allAliases) {
            if (alias == selected) continue
            packageManager.setComponentEnabledSetting(
                ComponentName(packageName, "$packageName.$alias"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}

/** iOS는 preferredColorScheme(.light) — 다크 모드와 무관하게 라이트 팔레트를 강제한다. */
@Composable
fun StarManagerTheme(
    appearance: AppAppearance = LocalAppAppearance.current,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = BrandTheme.accent,
            background = BrandTheme.canvas(appearance),
            surface = BrandTheme.surface(appearance)
        ),
        content = content
    )
}

private enum class AppTab(val title: String) {
    COMPOSER("스튜디오"),
    SETTINGS("설정")
}

@Composable
private fun StarManagerApp(
    profileStore: CreatorProfileStore,
    composerViewModel: ComposerViewModel,
    automationPickerTrigger: Int,
    cameraShortcutTrigger: Int
) {
    val appearance = LocalAppAppearance.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showsResetConfirmation by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(BrandTheme.canvas(appearance))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 인라인 네비게이션 타이틀 + 런처 아이콘
        TopBar(
            title = if (selectedTab == 0) "스타메니저" else "설정",
            appearance = appearance,
            onCancel = if (selectedTab == 0) {
                {
                    if (composerViewModel.hasContent()) {
                        showsResetConfirmation = true
                    } else {
                        composerViewModel.resetComposer()
                    }
                }
            } else null
        )

        Box(Modifier.weight(1f)) {
            when (AppTab.entries[selectedTab]) {
                AppTab.COMPOSER -> ComposerScreen(
                    viewModel = composerViewModel,
                    automationPickerTrigger = automationPickerTrigger,
                    cameraShortcutTrigger = cameraShortcutTrigger
                )
                AppTab.SETTINGS -> ProfileSettingsScreen(profileStore)
            }
        }

        BottomTabBar(
            selectedIndex = selectedTab,
            appearance = appearance,
            onSelect = {
                selectedTab = it
            },
            onReset = {
                selectedTab = 0
                if (composerViewModel.hasContent()) {
                    showsResetConfirmation = true
                } else {
                    composerViewModel.resetComposer()
                }
            }
        )
    }

    if (showsResetConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showsResetConfirmation = false
            },
            title = { Text("새 이야기로 시작할까요?") },
            text = { Text("현재 이야기, 만든 게시물과 미디어를 비웁니다. 설정은 그대로 유지됩니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        composerViewModel.resetComposer()
                        showsResetConfirmation = false
                    },
                    modifier = Modifier.testTag("composer.resetConfirm")
                ) {
                    Text("새로 시작", color = BrandTheme.accent)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showsResetConfirmation = false
                    },
                    modifier = Modifier.testTag("composer.resetCancel")
                ) {
                    Text("취소", color = BrandTheme.labelSecondary(appearance))
                }
            }
        )
    }
}

@Composable
private fun TopBar(
    title: String,
    appearance: AppAppearance = LocalAppAppearance.current,
    onCancel: (() -> Unit)? = null
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(BrandTheme.canvas(appearance)),
        contentAlignment = Alignment.Center
    ) {
        if (onCancel != null) {
            val cancelShape = RoundedCornerShape(22.dp)
            TextButton(
                onClick = onCancel,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .shadow(5.dp, cancelShape, ambientColor = Color.Black.copy(alpha = 0.04f), spotColor = Color.Black.copy(alpha = 0.08f))
                    .background(Color.White, cancelShape)
                    .testTag("composer.cancel")
            ) {
                Text("취소", fontSize = 16.sp, color = BrandTheme.labelPrimary(appearance))
            }
        }

        if (title == "스타메니저") {
            val launcherIconRes = when (appearance) {
                AppAppearance.CLASSIC -> R.drawable.starmanager_app_icon_classic
                AppAppearance.INTERSTELLAR -> R.drawable.starmanager_app_icon_interstellar
                AppAppearance.BK -> R.drawable.starmanager_app_icon
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "스타메니저"
                }
            ) {
                Image(
                    painter = painterResource(launcherIconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandTheme.labelPrimary(appearance)
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = title
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = BrandTheme.labelPrimary(appearance),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandTheme.labelPrimary(appearance)
                )
            }
        }
    }
}

/** 탭바 — 스튜디오/새 캔버스/설정 */
@Composable
private fun BottomTabBar(
    selectedIndex: Int,
    appearance: AppAppearance = LocalAppAppearance.current,
    onSelect: (Int) -> Unit,
    onReset: () -> Unit
) {
    val barShape = RoundedCornerShape(32.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(BrandTheme.canvas(appearance)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier
                .fillMaxWidth(0.72f)
                .height(58.dp)
                .shadow(12.dp, barShape, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.12f))
                .background(Color.White.copy(alpha = 0.98f), barShape)
                .border(1.dp, Color.White.copy(alpha = 0.9f), barShape)
                .padding(4.dp)
        ) {
            BottomTabButton(
                title = AppTab.COMPOSER.title,
                icon = tabIcon(AppTab.COMPOSER),
                selected = selectedIndex == 0,
                appearance = appearance,
                onClick = { onSelect(0) },
                testTag = "tab.COMPOSER",
                modifier = Modifier.weight(1f)
            )
            BottomTabButton(
                title = "새 캔버스",
                icon = Icons.Filled.RestartAlt,
                selected = false,
                appearance = appearance,
                onClick = onReset,
                testTag = "tab.RESET",
                modifier = Modifier.weight(1f)
            )
            BottomTabButton(
                title = AppTab.SETTINGS.title,
                icon = tabIcon(AppTab.SETTINGS),
                selected = selectedIndex == 1,
                appearance = appearance,
                onClick = { onSelect(1) },
                testTag = "tab.SETTINGS",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomTabButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    appearance: AppAppearance,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) BrandTheme.labelPrimary(appearance) else BrandTheme.labelSecondary(appearance)
    val itemShape = RoundedCornerShape(26.dp)

    Column(
        modifier
            .fillMaxSize()
            .background(if (selected) Color(0xFFE9ECF0) else Color.Transparent, itemShape)
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(title, fontSize = 10.sp, color = tint, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

private fun tabIcon(tab: AppTab): ImageVector = when (tab) {
    AppTab.COMPOSER -> Icons.Filled.AutoAwesome
    AppTab.SETTINGS -> Icons.Filled.Settings
}
