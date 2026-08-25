# StarManager — Android

iOS SwiftUI 앱 `/StarManager`의 도달 가능한 두 탭(**만들기**, **내 설정**)을 충실히 이식한
단일 모듈 Kotlin/Jetpack Compose 앱입니다.

- applicationId: `com.armsone.starmanager` · minSdk 26 · target/compileSdk 37 · Java/Kotlin 17
- 앱 버전: `2.0.1` · versionCode `340540` · 표시 빌드 `202608251140`
- 폰/태블릿 지원, 화면 회전 잠금 없음, 라이트 테마 강제(iOS `preferredColorScheme(.light)` 대응)
- 설정에서 GitHub Releases의 서명 APK 업데이트를 자동 또는 수동으로 확인·다운로드하고 시스템 설치자로 넘깁니다.
- iOS의 아카이브(README 전용)와 도달 불가능한 레거시 백엔드/Image Playground 표면은 이식하지 않음

## 빌드

Gradle wrapper 9.5가 포함되어 있습니다.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

버전: AGP 9.3.0 · Kotlin Compose plugin 2.3.21 · Compose BOM 2024.12.01

## iOS 원본 자산

브랜드 이미지(ChatGPT/Gemini/Grok)와 앱 아이콘 PNG는 iOS 원본 바이트를 그대로 포함합니다.
원본을 다시 동기화하려면 다음을 실행하세요.

```sh
sh scripts/copy-ios-assets.sh ../StarManager
```

- 앱이 `brand_chatgpt`/`brand_gemini`/`brand_grok` drawable을 직접 표시합니다.
- 런처 아이콘도 iOS 원본 `starmanager_app_icon.png`를 사용합니다.

## 충실도 메모 (iOS 원본 사실)

- 색상: accent(0.42/0.33/0.67), paper(0.99/0.95/0.86), canvas(0.98/0.97/0.94),
  surface(white 96%), border(0.81/0.78/0.72 @42%)
- 카드: padding 18 / radius 20 / border 1 · 칼럼 최대 540dp
- 레이아웃: <600dp 패딩 16/12·간격 16·최대 680dp / ≥600dp 패딩 24/20·간격 20·최대 1120dp 2열
- 초기 표시 스타일 386, 최초 실행 마이그레이션은 386 적용 + 글자 수 200 유지
- 스타일 4종(MZ/X/386/꼰대) · 분위기 3종 · 이야기 비중 3종
- 글자 수 슬라이더 50..500(10 단위) · 톤 슬라이더 0..100(5 단위) · 설정 콘텐츠 최대 760dp
- 프로필/프리셋 즉시 저장(SharedPreferences JSON, iOS UserDefaults 키와 동일:
  `creatorProfile`, `writingPresets`, `defaultGenerationStyleVersion`)
- 글자 수는 Swift Character(확장 문자소 클러스터) 단위: 기기에서는 android.icu
  BreakIterator, JVM 테스트에서는 `\X` 정규식 폴백
- 결정적 생성기의 시드/난수는 Swift UInt64 오버플로 연산(&*, &+, >>33)을 ULong으로 재현
- 기기 AI(FoundationModels)는 Android에 없으므로 iOS 미지원 기기 경로와 동일하게
  항상 결정적 "기본 생성"으로 폴백
- 미디어: 최대 10개, 촬영 추가, 대표 배지, 길게 눌러 순서 변경, 1:1/4:5/9:16 비율,
  공유 전 문구 자동 복사 + 미디어 없음/조건 변경 가드

## 디버그 픽스처 훅

릴리스 동작에 영향 없이 디버그 빌드에서만 인텐트 엑스트라로 활성화됩니다.

```sh
adb shell am start -n com.armsone.starmanager/.MainActivity \
  --ez starmanager.fixture.zeroDelay true \
  --ez starmanager.fixture.resetState true
```

주요 컴포저블에는 `composer.*` / `preview.*` / `settings.*` / `tab.*` testTag가 있습니다.

## 알려진 차이·미검증 항목

- SF Symbols/SF 폰트는 사용하지 않고 Material 벡터 아이콘(Apache-2.0)과 시스템 폰트로
  근사했습니다. 아이콘·타이포그래피의 픽셀 단위 일치는 보장하지 않습니다.
- iOS의 apple.logo(AI 버튼)는 상표 자산이라 스파클 아이콘으로 대체했습니다.
- Android 공유 시트는 완료/취소 콜백이 없어 iOS의 "공유 완료/취소" 상태 메시지 분기는
  생략되고, 공유 직전 상태 메시지("문구 복사됨" 등)만 표시합니다.
- 프리셋 삭제는 iOS 스와이프 대신 명시적 삭제 버튼입니다.
- 페어 캡처가 없어 픽셀 패리티는 아직 검증되지 않았습니다. 현재 소스 상태에서 JVM 단위 테스트와
  `assembleDebug`는 통과했습니다. 자세한 증거 상태는 `parity/matrix/parity.csv`에 기록합니다.
