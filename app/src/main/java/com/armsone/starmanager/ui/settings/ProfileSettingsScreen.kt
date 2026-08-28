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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armsone.starmanager.design.BrandTheme
import com.armsone.starmanager.design.IconWell
import com.armsone.starmanager.design.IconWellVariant
import com.armsone.starmanager.design.LocalAppAppearance
import com.armsone.starmanager.design.StarSegmentedControl
import com.armsone.starmanager.design.StarSwitch
import com.armsone.starmanager.model.AppAppearance
import com.armsone.starmanager.model.CreatorProfileStore
import com.armsone.starmanager.service.DirectAIProvider
import com.armsone.starmanager.ui.externalai.ExternalAIAuthStatus
import com.armsone.starmanager.ui.externalai.ExternalAIAuthState
import com.armsone.starmanager.ui.externalai.ExternalAISurface
import com.armsone.starmanager.ui.externalai.ExternalAISurfaceMode
import com.armsone.starmanager.update.DirectUpdateManager
import com.armsone.starmanager.update.DirectUpdateSettings

/**
 * "설정" 화면.
 * 1. 외부 로그인 관리 (브라우저 보기 토글, Gemini, ChatGPT, Claude)
 * 2. 앱 업데이트 관리
 * 3. 테마 관리 (BK / 클래식 외형 전환)
 */
@Composable
fun ProfileSettingsScreen(store: CreatorProfileStore) {
    val context = LocalContext.current
    val updateManager = remember(context) { DirectUpdateManager.get(context) }
    val appearance by store.appearance.collectAsStateWithLifecycle()
    val showsExternalAIBrowser by store.showsExternalAIBrowser.collectAsStateWithLifecycle()

    var activeLoginProvider by remember { mutableStateOf<DirectAIProvider?>(null) }
    val loginProviders = remember {
        listOf(DirectAIProvider.GEMINI, DirectAIProvider.OPEN_AI, DirectAIProvider.CLAUDE)
    }
    var authStates by remember {
        mutableStateOf(loginProviders.associateWith { ExternalAIAuthState.CHECKING })
    }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        loginProviders.forEach { provider ->
            val state = ExternalAIAuthStatus.probe(context, provider)
            authStates = authStates + (provider to state)
        }
    }

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
                // 1. 외부 로그인 관리
                SettingsSection(
                    header = "외부 로그인 관리",
                    icon = Icons.Filled.AccountCircle,
                    footer = "브라우저 보기는 기본적으로 꺼져 있어요. 켜면 글을 만드는 과정을 처음부터 볼 수 있어요. 한 번 로그인하면 이 기기에서는 서비스가 로그아웃시키기 전까지 기억돼요. 스타매니저는 비밀번호를 보거나 저장하지 않아요.",
                    appearance = appearance,
                    variant = IconWellVariant.CARBON
                ) {
                    // 브라우저 보기 토글
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "브라우저 보기",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = BrandTheme.labelPrimary(appearance)
                            )
                            Text(
                                "AI가 답하는 화면을 처음부터 보여줘요.",
                                fontSize = 12.sp,
                                color = BrandTheme.labelSecondary(appearance)
                            )
                        }
                        StarSwitch(
                            checked = showsExternalAIBrowser,
                            appearance = appearance,
                            onCheckedChange = { checked ->
                                store.setShowsExternalAIBrowser(checked)
                            },
                            modifier = Modifier.testTag("settings.showsExternalAIBrowser")
                        )
                    }

                    HorizontalDivider(color = BrandTheme.divider(appearance))

                    // 로그인 제공사 행들
                    val providers = listOf(DirectAIProvider.GEMINI, DirectAIProvider.OPEN_AI, DirectAIProvider.CLAUDE)
                    providers.forEachIndexed { index, provider ->
                        if (index > 0) {
                            HorizontalDivider(color = BrandTheme.divider(appearance))
                        }
                        val state = authStates[provider] ?: ExternalAIAuthState.CHECKING
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.testTag("settings.login.${provider.rawValue}")
                            ) {
                                if (state == ExternalAIAuthState.LOGGED_IN) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF34C759),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Text(
                                    state.label,
                                    fontSize = 15.sp,
                                    fontWeight = if (state == ExternalAIAuthState.REQUIRES_LOGIN) FontWeight.SemiBold else FontWeight.Normal,
                                    color = when (state) {
                                        ExternalAIAuthState.REQUIRES_LOGIN -> BrandTheme.accent
                                        ExternalAIAuthState.LOGGED_IN -> Color(0xFF34C759)
                                        ExternalAIAuthState.CHECKING -> BrandTheme.labelSecondary(appearance)
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. 앱 업데이트 관리
                SettingsSection(
                    header = "앱 업데이트 관리",
                    icon = Icons.Outlined.ArrowCircleDown,
                    footer = "최신 버전을 확인하고 설치할 수 있어요.",
                    appearance = appearance,
                    variant = IconWellVariant.CARBON
                ) {
                    DirectUpdateSettings(updateManager)
                }

                // 3. 테마 관리 (BK / 클래식 외형 전환)
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
            onClose = {
                activeLoginProvider = null
                refreshTrigger += 1
            },
            onLoginSuccess = {
                authStates = authStates + (loginProvider to ExternalAIAuthState.LOGGED_IN)
                refreshTrigger += 1
            },
            appearance = appearance
        )
    }
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
                if (icon != null) {
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
