# StarManager 외부 AI 컴포저 자동화 — Gemini 타임아웃 초안 이어받기 보고

- 시작: 2026-08-28 00:28 KST
- 종료: 2026-08-28 (본 보고 작성 시점, 세션 내)
- 그룹: `starmanager` — Android `/Users/armsone/git/StarManager-Android`
- 실행 담당: Gemini가 시간 초과로 중단한 초안(커밋되지 않은 작업 트리 변경분)을 검토·수정. 팀장(Claude) 검증 및 오류 수정. 실기기/에뮬레이터 실행, 빌드/테스트 실행, Git/네트워크 작업은 이번 세션 권한 범위 밖이라 수행하지 않았다.

**본 보고는 "동기화 완료(synchronized)"를 주장하지 않는다.** 소스 수준 검토·수정과 결정적 단위 테스트 저작만 수행했으며, 빌드·런타임·시각적 검증은 전혀 수행되지 않았다.

## 배경 — Gemini 타임아웃

이전 세션에서 Gemini가 iOS 패리티 "숨겨진 외부 AI 자동화" 기능(일반 생성은 Composer에 머물며 보이지 않는 WebView가 자동 입력·제출·관찰·가져오기를 수행하고, 전체 화면 `ExternalAISurface`는 로그인/캡차/수동 개입이 필요할 때만 사유 배너로 나타나는 폴백)을 구현하는 도중 응답 시간 초과로 중단되었다. 작업 트리에는 다음 변경분이 커밋되지 않은 채 남아 있었다:

- 수정: `.parity/ledger.json`, `.sync/product-contract.yaml`
- 수정: `ComposerScreen.kt`, `ComposerViewModel.kt`, `ExternalAIModels.kt`, `ExternalAIScripts.kt`, `ExternalAISurface.kt`
- 신규: `ExternalAIErrorSanitizer.kt`, `ExternalAIFallbackClassifier.kt`, `ExternalAITimerFormatter.kt`

이 초안은 컴파일 여부나 계약 충족 여부가 검증되지 않은 상태였다.

## 검토 범위와 방법

허용된 범위(`ComposerScreen.kt`, `ComposerViewModel.kt`, `ui/externalai/**`, `.sync/product-contract.yaml`, `.parity/ledger.json`, 신규 `.sync/reports/*.md`, 관련 `app/src/test/**`) 내 모든 변경/신규 파일과 그 파일이 참조하는 기존 baseline 심볼(`ExternalAIAnswerCleaner`, `ExternalAISecurityPolicy`, `DirectAIProvider`, `CaptionSource` 등)의 시그니처를 전부 읽고 대조했다. 셸/빌드/테스트 실행 권한이 없어 컴파일 검증은 수동 정적 추적(임포트, 시그니처, 타입, Compose 주석/중괄호 짝)으로 대체했다.

## 영향받은 기능(capability) 표

| 기능 ID | 위치 | 구현 상태(수정 전) | 발견된 문제 | 조치 | 구현 상태(수정 후) |
|---|---|---|---|---|---|
| `composer_external_ai_background_browser_lifecycle` | `ComposerScreen.kt` `HiddenExternalAIWebView` | 초안 완성, 미검증 | 폴링 루프에서 DOM 오류 감지와 답변 추출 두 `evaluateJavascript` 호출이 동시에 실행되어 같은 틱에서 `onError`와 `onSuccess`가 동시에 발생할 수 있는 중복 종료 이벤트 경쟁 상태; `onPageFinished` 재진입 시 첫 시도의 콜백이 돌아오기 전에 두 번째 프롬프트 주입이 시작될 수 있는 이중 제출 경쟁 상태 | 두 `evaluateJavascript` 호출을 `suspendCancellableCoroutine`으로 감싸 순차 실행(오류 확인 → 답변 확인)하도록 직렬화; `isSubmitted` 플래그를 실제 전송 성공 확인 시점이 아니라 시도 시작 시점에 즉시 세우고 실패 시에만 되돌리도록 변경 | `implemented_source_only` (수정 반영, 미빌드) |
| `composer_external_ai_in_app_browser` / `composer_external_ai_visible_fallback` | `ExternalAISurface.kt` | 초안 완성, 미검증 | 폴백 진입 시 표시되어야 할 사유 배너(`fallbackReason.bannerText`, 예: "로그인이 필요해요")가 `statusDetail` 상태 변수를 공유해 페이지 로드 시작(`onPageStarted`)마다 즉시 `null`로 지워짐; `currentStatus` 초기화 조건문의 두 분기가 동일해 무의미; 응답 대기 폴링이 최대 시도 후 조용히 멈추고 사용자에게 아무 안내도 주지 않음 | 사유 배너를 `statusDetail`과 분리된 별도의 영속 상태(`reasonBanner`)로 두어 자동화 상태 갱신과 무관하게 유지되도록 UI를 분리된 행으로 재구성; 중복 조건 제거; 폴링 타임아웃 시 오류 상태와 "다시 넣기" 안내 문구 추가 | `implemented_source_only` (수정 반영, 미빌드) |
| `composer_external_ai_status_and_elapsed_timer` | `ComposerViewModel.kt` (변경 없음), `ExternalAITimerFormatter.kt` (신규) | 소스 완성, 테스트 없음 | 순수 포맷터에 대한 단위 테스트 부재 | `ExternalAIDraftUtilsTest.kt`에 60초 미만/이상, 음수 clamp, 대기 문구 래핑 테스트 추가 | `implemented_source_only`, 테스트 저작 완료·미실행 |
| `composer_external_ai_error_relay_and_sanitization` | `ExternalAIErrorSanitizer.kt` (신규) | 소스 완성, 테스트 없음 | 순수 정제기에 대한 단위 테스트 부재 | HTML/script/style 제거, URL·토큰·비밀 마스킹, 스택 트레이스 제거, 기본 메시지 폴백, 최대 길이 축약, 흔한 접두사 제거에 대한 테스트 추가 | `implemented_source_only`, 테스트 저작 완료·미실행 |
| `composer_external_ai_visible_fallback` (분류기 부분) | `ExternalAIFallbackClassifier.kt` (신규) | 소스 완성, 테스트 없음 | 순수 분류기에 대한 단위 테스트 부재 | URL 기반 로그인/보안 분류, DOM 상태 우선순위 분류, null/빈 문자열 처리에 대한 테스트 추가 | `implemented_source_only`, 테스트 저작 완료·미실행 |
| `composer_external_ai_provider_roster`, `composer_external_ai_single_spinner_and_static_preview`, `composer_external_ai_success_keyboard_focus` | `ComposerScreen.kt` (변경 없음, 검토만) | 소스 완성, 미검증 | 정적 추적 결과 별도 결함 없음 — 생성 중 미리보기가 결과 없음 상태를 유지하고 스피너가 상태 카드에만 존재하며, `lastImportSuccessToken`이 성공적인 가져오기(빈 값 제외)에서만 갱신되어 포커스/키보드 해제 조건을 충족함을 확인 | 변경 없음 | `implemented_source_only` (기존과 동일) |

기존 원장 행(`composer_posting_handoff_*`, `settings_*`, `direct_updates`, `compact_generation_prompt_parity`, `external_ai_answer_cleaner_parity`)은 이번 세션에서 수정하지 않았으며, 모든 이력이 그대로 보존되어 있음을 확인했다.

## 구현 상태 상세

### 수정한 소스 파일
- `app/src/main/java/com/armsone/starmanager/ui/composer/ComposerScreen.kt`
  - 사용되지 않던 `ExternalAIAutomationPhase` 임포트 제거, `ExternalAIDomErrorResult`/`ExternalAIPollResult`/`suspendCancellableCoroutine` 임포트 추가
  - `HiddenExternalAIWebView`의 폴링 루프를 순차 실행으로 재작성(중복 종료 이벤트 방지)
  - `onPageFinished` 내 프롬프트 재주입 가드(`isSubmitted`)를 시도 시작 시점 즉시 세팅 + 실패 시 롤백 방식으로 변경(이중 제출 방지)
- `app/src/main/java/com/armsone/starmanager/ui/externalai/ExternalAISurface.kt`
  - 폴백 사유 배너를 자동화 상태 텍스트와 분리된 영속 상태로 재구성, 별도 UI 행 추가(`testTag("externalai.fallbackBanner")`)
  - `currentStatus` 초기화의 중복 조건 분기 제거
  - 답변 대기 폴링이 60회(약 2분) 이내에 안정 답변을 얻지 못하면 오류 상태와 재시도 안내로 전환하도록 추가

### 신규 파일
- `app/src/test/java/com/armsone/starmanager/ExternalAIDraftUtilsTest.kt` — `ExternalAIErrorSanitizer`, `ExternalAITimerFormatter`, `ExternalAIFallbackClassifier`에 대한 순수 JVM 단위 테스트(WebView/네트워크 미사용, 결정적)

### 변경하지 않고 검토만 완료한 파일
- `ComposerViewModel.kt` — 생성/취소/리셋/가져오기 상태 전이, 타이머 잡·생성 잡 취소, 포커스 해제 트리거(`lastImportSuccessToken`)가 요구된 계약과 일치함을 확인. 별도 결함 없음.
- `ExternalAIModels.kt`, `ExternalAIScripts.kt` — 데이터 모델·스크립트·파서 시그니처가 호출부와 일치함을 확인. 별도 결함 없음.
- `ExternalAIAnswerCleaner.kt`, `ExternalAISecurityPolicy.kt` — 기존 baseline 파일(이번 초안에서 변경되지 않음), 참조 시그니처만 대조해 호출부와 일치함을 확인.
- `.sync/product-contract.yaml` — 기존에 추가된 `external_ai_provider_roster_and_composer_browser` 기능이 이미 정직하게 `implemented_source_only`(Android)/`reference_verified`(iOS)로 표기되어 있고 append 방식으로 추가되었음을 확인. 이번 세션에서 추가 수정 없음.

## 테스트 저작 여부 및 실행 여부

- 저작 완료: `ExternalAIDraftUtilsTest.kt` (17개 테스트 메서드 — 정제기 6, 타이머 5, 분류기 6)
- 실행: **수행하지 않음**. 이번 세션 권한에는 빌드·테스트 실행이 포함되지 않는다. 기존 `ExternalAIParityTest.kt`(안정성 리듀서, 스크립트, 보안 정책 등)도 이번 세션에서 재실행하지 않았다.
- 컴파일 여부: **확인되지 않음**. 독립적인 빌드 검증이 필요하다.

## 런타임/Matchup 근거 상태

- 실기기·에뮬레이터 실행 없음.
- 화면 캡처·Matchup 원자 장부 갱신 없음.
- 원장(`ledger.json`)의 관련 행은 모두 기존과 동일하게 `implemented_source_only`이며, 이번 세션에서 `matched`로 격상한 행은 없다. 각 행에 이번 세션 조치를 설명하는 attempt를 원자적으로 추가했다(기존 이력은 삭제·수정하지 않음).

## 오류와 해결

| 단계 | 오류/위험 | 원인 | 조치 | 결과 | 남음 |
|---|---|---|---|---|---|
| 초안 인수 | Gemini 응답 시간 초과로 작업 중단 | 외부 에이전트 세션 제한 | 커밋되지 않은 작업 트리 변경분을 전부 정적으로 재검토 | 문제 파악 및 수정 완료(아래 항목) | 빌드/런타임 검증 필요 |
| `HiddenExternalAIWebView` 폴링 | 동일 틱 내 `onError`/`onSuccess` 중복 발생 가능 | 오류 감지와 답변 추출 `evaluateJavascript`가 병렬로 실행되고 각각 독립적으로 상태를 종료시킴 | `suspendCancellableCoroutine`으로 순차화 | 소스 수정 완료 | 실기기 검증 필요 |
| `HiddenExternalAIWebView` 재진입 | `onPageFinished` 중복 호출 시 이중 프롬프트 제출 가능 | `isSubmitted` 플래그가 전송 성공 확인 후에만 세팅됨 | 시도 시작 시점 즉시 세팅, 실패 시 롤백 | 소스 수정 완료 | 실기기 검증 필요 |
| `ExternalAISurface` 폴백 배너 | 사유 배너가 즉시 사라짐 | `statusDetail` 단일 상태를 배너와 자동화 상태 양쪽에 재사용 | 배너를 별도 영속 상태로 분리 | 소스 수정 완료 | 실기기 검증 필요 |
| 신규 순수 유틸리티 | 단위 테스트 부재 | Gemini 초안이 유틸리티만 작성하고 테스트 전 시간 초과 | `ExternalAIDraftUtilsTest.kt` 저작 | 테스트 저작 완료, 미실행 | 테스트 실행 및 결과 확인 필요 |

## 미완료 항목 (다음 안전 작업)

- 독립 빌드(`./gradlew assembleDebug` 또는 동등 작업)로 컴파일 여부를 확인해야 한다. 이번 세션은 셸/빌드 권한이 없어 정적 추적으로만 검증했다.
- `ExternalAIDraftUtilsTest.kt`와 기존 `ExternalAIParityTest.kt`를 실행해 회귀 여부를 확인해야 한다.
- 실기기에서 Gemini/ChatGPT/Claude 각각에 대해 숨겨진 자동화(입력·제출·관찰·가져오기)와 폴백 배너(로그인/캡차/수동 입력) 경로를 모두 시연하고 화면 캡처를 확보해야 한다.
- 성공적인 비어있지 않은 가져오기 시 포커스/키보드가 실제 기기에서 해제되고 사용자가 다시 탭하기 전까지 재포커스되지 않는지 실기기에서 확인해야 한다.
- 생성 중 회전 스피너가 정확히 하나만 존재하는지(상태 카드 외 다른 곳에서 우발적으로 나타나지 않는지) 실기기 캡처로 확인해야 한다.
- 확인 후 원자 Matchup 근거를 생성하고 관련 원장 행을 `implemented_source_only` → 검증된 상태로 갱신해야 한다. 이번 보고는 동기화 완료를 주장하지 않는다.

## 후속 변경 — 브라우저 보기 설정

대표님의 후속 요청에 따라 Android 설정의 `외부 AI 로그인 관리` 섹션에 `브라우저 보기` 스위치를 추가했다. `CreatorProfileStore`가 기존 `SharedPreferences` 추상화에 이 선택을 0/1로 저장하며, 저장값이 없을 때는 OFF다.

- OFF: 기존 `HiddenExternalAIWebView`가 Composer 안에서 보이지 않게 자동화한다.
- ON: 생성 시작부터 전체 화면 `ExternalAISurface`를 표시한다. 프롬프트 자동 입력·제출, `정보를 보냈어요`와 초 단위 대기 상태, DOM 오류 정제, 로그인/CAPTCHA/수동 입력 안내, 안정 답변 자동 가져오기를 유지한다.
- 성공적인 자동 가져오기 뒤에는 기존 `lastImportSuccessToken` 흐름이 Composer 입력 포커스를 강제로 해제하고 키보드를 숨긴다.
- `CreatorProfileStoreTest`에 기본 OFF와 ON/OFF 재실행 영속성 테스트를 추가했다.

정적 점검 중 `:app:compileDebugKotlin`을 한 차례 실행했으나 `ComposerScreen.kt`의 `ExternalAISurface` 및 `ExternalAISurfaceMode` 임포트 누락으로 실패했다. 누락 임포트는 즉시 보완했다. 후속 지시의 검증 분리 범위에 맞춰 수정 후 컴파일과 테스트는 다시 실행하지 않았으므로, 최종 상태는 계속 `implemented_source_only`이며 빌드·런타임·시각 검증을 주장하지 않는다.

## 후속 변경 — Composer 스크롤 시 키보드 숨김

Composer 최상위 세로 스크롤 컨테이너에 `NestedScrollConnection`을 연결했다. 사용자의 실제 세로 스크롤 입력(`NestedScrollSource.UserInput`)에서 첫 non-zero 이동량이 들어오면 `FocusManager.clearFocus(force = true)`와 `SoftwareKeyboardController.hide()`를 즉시 호출한다. 연결은 `Offset.Zero`를 반환하므로 스크롤 입력을 소비하지 않으며, 이후 사용자가 이야기 입력창을 다시 탭하면 `BasicTextField`가 평소대로 포커스를 얻어 키보드를 다시 표시할 수 있다. 결과 가져오기 성공 시 `lastImportSuccessToken`으로 강제 숨기는 기존 경로도 그대로 유지했다.

이 후속 변경은 `git diff --check`, 패리티 JSON 파싱, 제품 계약 YAML 파싱으로만 정적 확인했다. 빌드·테스트·실기기 키보드 동작 검증은 수행하지 않았으며 `composer_scroll_keyboard_dismissal` 원장 행을 `implemented_source_only`로 추가했다.

## 팀장 최종 후보 검증

- 종료: 2026-08-28 00:58 KST
- 릴리즈 후보: `2.2.0`, versionCode `344215`, Build-Number `202608280055`
- `testDebugUnitTest`: 80개 전부 통과
- `assembleDebug`: 통과
- `lintDebug`: 통과
- APK SHA-256: `30dc3314ee9760ecde17b043c39e9f5fbab106fd9c3eba25b43259e373d30982`
- 공개 2.1.0 APK와 서명 인증서 SHA-256이 동일함을 확인해 업데이트 연속성을 검증했다.
- 연결된 Android 실기기가 없어 설치·실화면·Matchup 검증은 수행하지 않았다. 따라서 관련 패리티 행은 `implemented_source_only`를 유지한다.
