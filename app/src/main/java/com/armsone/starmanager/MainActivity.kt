package com.armsone.starmanager

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armsone.starmanager.design.BrandTheme
import com.armsone.starmanager.model.CreatorProfileStore
import com.armsone.starmanager.model.SharedPreferencesKeyValueStore
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
            FixtureHooks.resetStateRequested = false
        }
        val profileStore = CreatorProfileStore(storage)

        setContent {
            StarManagerTheme {
                StarManagerApp(profileStore)
            }
        }
    }
}

/** iOS는 preferredColorScheme(.light) — 다크 모드와 무관하게 라이트 팔레트를 강제한다. */
@Composable
fun StarManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = BrandTheme.accent,
            background = BrandTheme.canvas,
            surface = Color.White
        ),
        content = content
    )
}

private enum class AppTab(val title: String) {
    COMPOSER("만들기"),
    SETTINGS("내 설정")
}

@Composable
private fun StarManagerApp(profileStore: CreatorProfileStore) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val composerViewModel: ComposerViewModel = viewModel()
    composerViewModel.profileStore = profileStore

    Column(
        Modifier
            .fillMaxSize()
            .background(BrandTheme.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // iOS inline 네비게이션 타이틀 근사치
        TopBar(title = if (selectedTab == 0) "스타메니저" else "내 설정")

        Box(Modifier.weight(1f)) {
            when (AppTab.entries[selectedTab]) {
                AppTab.COMPOSER -> ComposerScreen(composerViewModel)
                AppTab.SETTINGS -> ProfileSettingsScreen(profileStore)
            }
        }

        BottomTabBar(
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it }
        )
    }
}

@Composable
private fun TopBar(title: String) {
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(BrandTheme.canvas),
            contentAlignment = Alignment.Center
        ) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = BrandTheme.labelPrimary)
        }
        HorizontalDivider(color = Color(0x293C3C43), thickness = 0.5.dp)
    }
}

/** iOS 탭바 근사치 — sparkles/person.crop.circle 대신 앱 소유 근사 아이콘 사용. */
@Composable
private fun BottomTabBar(selectedIndex: Int, onSelect: (Int) -> Unit) {
    Column {
        HorizontalDivider(color = Color(0x293C3C43), thickness = 0.5.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(Color.White.copy(alpha = 0.98f))
        ) {
            AppTab.entries.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val tint = if (selected) BrandTheme.accent else Color(0xFF999999)
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(index) }
                        .testTag("tab.${tab.name}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        tabIcon(tab),
                        contentDescription = tab.title,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(tab.title, fontSize = 10.sp, color = tint)
                }
            }
        }
    }
}

private fun tabIcon(tab: AppTab): ImageVector = when (tab) {
    AppTab.COMPOSER -> Icons.Filled.AutoAwesome    // sparkles 근사
    AppTab.SETTINGS -> Icons.Filled.AccountCircle  // person.crop.circle 근사
}
