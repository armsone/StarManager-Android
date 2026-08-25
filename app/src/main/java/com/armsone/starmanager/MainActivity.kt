package com.armsone.starmanager

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armsone.starmanager.design.BrandTheme
import com.armsone.starmanager.design.IconWell
import com.armsone.starmanager.design.IconWellVariant
import com.armsone.starmanager.design.LocalAppAppearance
import com.armsone.starmanager.model.AppAppearance
import com.armsone.starmanager.model.CreatorProfileStore
import com.armsone.starmanager.model.SharedPreferencesKeyValueStore
import com.armsone.starmanager.update.DirectUpdateManager
import com.armsone.starmanager.ui.composer.ComposerScreen
import com.armsone.starmanager.ui.composer.ComposerViewModel
import com.armsone.starmanager.ui.settings.ProfileSettingsScreen

class MainActivity : ComponentActivity() {

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
            storage.remove(ComposerViewModel.KEY_PASTE_GUIDANCE_SHOWN)
            FixtureHooks.resetStateRequested = false
        }
        val profileStore = CreatorProfileStore(storage)
        DirectUpdateManager.get(applicationContext).start()

        setContent {
            val appearance by profileStore.appearance.collectAsStateWithLifecycle()
            LaunchedEffect(appearance) {
                updateLauncherIcon(appearance)
            }
            CompositionLocalProvider(LocalAppAppearance provides appearance) {
                StarManagerTheme(appearance = appearance) {
                    StarManagerApp(profileStore)
                }
            }
        }
    }

    private fun updateLauncherIcon(appearance: AppAppearance) {
        val selected = if (appearance == AppAppearance.CLASSIC) "ClassicIconAlias" else "BkIconAlias"
        val other = if (appearance == AppAppearance.CLASSIC) "BkIconAlias" else "ClassicIconAlias"
        packageManager.setComponentEnabledSetting(
            ComponentName(packageName, "$packageName.$selected"),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        packageManager.setComponentEnabledSetting(
            ComponentName(packageName, "$packageName.$other"),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
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
    SETTINGS("나의 취향")
}

@Composable
private fun StarManagerApp(profileStore: CreatorProfileStore) {
    val appearance = LocalAppAppearance.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showsResetConfirmation by rememberSaveable { mutableStateOf(false) }
    var exitAfterReset by rememberSaveable { mutableStateOf(false) }
    val composerViewModel: ComposerViewModel = viewModel()
    composerViewModel.profileStore = profileStore

    Column(
        Modifier
            .fillMaxSize()
            .background(BrandTheme.canvas(appearance))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 인라인 네비게이션 타이틀 + 선행 아이콘 웰
        TopBar(
            title = if (selectedTab == 0) "스타메니저" else "나의 취향",
            icon = if (selectedTab == 0) Icons.Filled.AutoAwesome else Icons.Filled.Tune,
            appearance = appearance,
            onCancel = if (selectedTab == 0) {
                {
                    if (composerViewModel.hasContent()) {
                        exitAfterReset = true
                        showsResetConfirmation = true
                    } else {
                        composerViewModel.resetComposer()
                        selectedTab = 1
                    }
                }
            } else null
        )

        Box(Modifier.weight(1f)) {
            when (AppTab.entries[selectedTab]) {
                AppTab.COMPOSER -> ComposerScreen(composerViewModel)
                AppTab.SETTINGS -> ProfileSettingsScreen(profileStore)
            }
        }

        BottomTabBar(
            selectedIndex = selectedTab,
            appearance = appearance,
            onSelect = {
                exitAfterReset = false
                selectedTab = it
            },
            onReset = {
                exitAfterReset = false
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
                exitAfterReset = false
            },
            title = { Text("새 이야기로 시작할까요?") },
            text = { Text("현재 이야기, 만든 게시물과 미디어를 비웁니다. 내 프로필과 설정은 그대로 유지됩니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        composerViewModel.resetComposer()
                        showsResetConfirmation = false
                        if (exitAfterReset) {
                            selectedTab = 1
                            exitAfterReset = false
                        }
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
                        exitAfterReset = false
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
    icon: ImageVector,
    appearance: AppAppearance = LocalAppAppearance.current,
    onCancel: (() -> Unit)? = null
) {
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(BrandTheme.canvas(appearance)),
            contentAlignment = Alignment.Center
        ) {
            if (onCancel != null) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .testTag("composer.cancel")
                ) {
                    Text(
                        text = "취소",
                        fontSize = 16.sp,
                        color = BrandTheme.accent
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (appearance == AppAppearance.BK) {
                    IconWell(
                        icon = icon,
                        appearance = appearance,
                        variant = if (title == "나의 취향") IconWellVariant.OXBLOOD else IconWellVariant.CARBON,
                        size = 24.dp,
                        iconSize = 14.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandTheme.labelPrimary(appearance)
                )
            }
        }
        HorizontalDivider(color = BrandTheme.divider(appearance), thickness = 0.5.dp)
    }
}

/** 탭바 — 스튜디오/새 캔버스/나의 취향 */
@Composable
private fun BottomTabBar(
    selectedIndex: Int,
    appearance: AppAppearance = LocalAppAppearance.current,
    onSelect: (Int) -> Unit,
    onReset: () -> Unit
) {
    val isBk = appearance == AppAppearance.BK
    val background = if (isBk) Color.White else Color.White.copy(alpha = 0.98f)

    Column {
        HorizontalDivider(color = BrandTheme.divider(appearance), thickness = 0.5.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(background)
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
    val tint = if (selected) {
        BrandTheme.accent
    } else {
        if (appearance == AppAppearance.BK) BrandTheme.bkLabelSecondary else Color(0xFF77736F)
    }

    Column(
        modifier
            .fillMaxSize()
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
    AppTab.COMPOSER -> Icons.Filled.AutoAwesome    // sparkles 근사
    AppTab.SETTINGS -> Icons.Filled.AccountCircle  // person.crop.circle 근사
}
