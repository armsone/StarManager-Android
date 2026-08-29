package com.armsone.imanagerai

/**
 * 디버그 빌드 전용 결정적 픽스처 훅.
 * MainActivity가 BuildConfig.DEBUG일 때만 인텐트 엑스트라를 읽어 값을 채우므로
 * 릴리스 빌드 동작에는 영향이 없다.
 *
 * adb 예시:
 *   adb shell am start -n com.armsone.starmanager/com.armsone.imanagerai.MainActivity \
 *     --ez imanagerai.fixture.zeroDelay true \
 *     --ez imanagerai.fixture.resetState true
 */
object FixtureHooks {
    /** 결정적 생성기의 인위적 지연(기본 550ms)을 재정의한다. 스크린샷/UI 테스트용. */
    @Volatile
    var captionDelayMillis: Long? = null

    /** true면 저장된 프로필/프리셋을 초기화한 뒤 스토어를 만든다(최초 실행 마이그레이션 재현). */
    @Volatile
    var resetStateRequested: Boolean = false

    const val EXTRA_ZERO_DELAY = "imanagerai.fixture.zeroDelay"
    const val EXTRA_RESET_STATE = "imanagerai.fixture.resetState"
    const val EXTRA_ALIAS_ZERO_DELAY = "imanager.fixture.zeroDelay"
    const val EXTRA_ALIAS_RESET_STATE = "imanager.fixture.resetState"
    const val EXTRA_LEGACY_ZERO_DELAY = "starmanager.fixture.zeroDelay"
    const val EXTRA_LEGACY_RESET_STATE = "starmanager.fixture.resetState"
}
