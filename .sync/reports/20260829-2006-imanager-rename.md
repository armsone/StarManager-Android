# iManagerAI 전체 이름 변경 동기화 보고서

- 시작: 2026-08-29 19:35 KST
- 종료: 2026-08-29 20:06 KST
- 경과: 약 31분
- 작업자: TM, C, G
- 범위: Apple, Android, Backend, AIBI 소비자/배포 스냅샷, 프로젝트 동기화 계약, Matchup 원장

## 작업자 사용량 스냅샷

- C: 5시간 잔여 98% → 94%, 주간 잔여 38% → 37%
- G: 5시간 잔여 약 71.76% → 55.02%, 주간 잔여 약 24.70% → 21.91%
- Codex: 주간 잔여 61% → 60%

## 결과

| 영역 | 결과 |
|---|---|
| Apple 프로젝트 | 저장소/프로젝트/타깃/소스/공유 확장/아이콘 파일명을 iManagerAI 규칙으로 변경 |
| Android 프로젝트 | 저장소/Gradle 프로젝트(`iManagerAI-Android`)/namespace(`com.armsone.imanagerai`)/Kotlin 패키지/리소스 파일명(`imanagerai`)을 iManagerAI 규칙으로 변경 |
| 사용자 노출 이름 | `iManagerAI` (한국어 문맥도 `iManagerAI`로 통일) |
| 딥링크·바로가기 | `imanagerai` 및 새 camera action(`com.armsone.imanagerai.action.CAMERA_CAPTURE`)을 기본값으로 추가, 기존 `imanager` 및 `starmanager` 값은 호환 입력으로 유지 |
| Backend | `IMANAGERAI_APP_TOKEN`, `X-iManagerAI-Token`, `imanagerai`를 기본값으로 변경하고 이전 인증 이름을 폴백으로 유지 |
| AIBI | 소비자 `imanagerai`, 프로필 `profiles/imanagerai`, 새 저장소·패키지 경로와 잠금 파일로 동기화 |
| 동기화 계약 | product id와 구현 참조를 `imanagerai`로 갱신하고 제품 정체성 capability 추가 |

## 의도적으로 유지한 이전 식별자

설치 데이터와 기존 호출의 연속성을 위해 다음 값은 변경하지 않았다.

- Apple bundle id: `com.armsone.StarManager`, `com.armsone.StarManager.ShareExtension`
- Apple App Group: `group.com.armsone.starmanager`
- Apple Keychain service: `com.armsone.StarManager`
- Android applicationId, FileProvider authority, SharedPreferences: `com.armsone.starmanager`, `com.armsone.starmanager.fileprovider`, `starmanager`
- 이전 URL scheme (`imanager`, `starmanager`), camera action, fixture keys, 서버 token header/env 이름
- GitHub 원격 저장소 URL과 Android 릴리스 저장소/자산 이름 폴백(`iManager-Android`, `StarManager-Android`)

## 검증

- Apple plist/entitlements `plutil -lint`: 통과
- `xcodebuild -list -project iManagerAI.xcodeproj`: 프로젝트, 타깃, 스킴 `iManagerAI`/`iManagerAIShare` 확인
- iOS Simulator Debug build: 통과
- Backend `node Backend/worker.test.mjs`: `BACKEND_CHECK_OK`
- Android `testDebugUnitTest assembleDebug`: 통과
- AIBI 단위 테스트 25개: 통과
- `aibi_sync.py check imanagerai`: 모든 배포 파일 current
- product contract YAML 및 parity ledger JSON 구조: 유효
- Matchup gate: 구조는 정상(20개 행)이나 11개 runtime/visual 항목이 열려 있어 완료 gate는 실패. 이번 이름 변경에서 추가한 `product_identity.visible_brand`, `product_identity.deep_link_compatibility`는 빌드 검증 후에도 paired capture/runtime trace가 없어 `needs_verification` 유지

## 발견한 오류와 해결

- 첫 C 실행은 셸 기능을 사용할 수 없어 중단: 콘텐츠 전용 작업으로 재실행하고 TM이 물리 이동을 수행했다.
- iOS Info.plist 자동 치환 한 건이 정확한 문자열을 찾지 못함: TM이 plist 구조를 직접 확인하고 최종 lint/build로 검증했다.
- Android 첫 빌드에서 `IManagerAITheme` 중복 선언과 존재하지 않는 `updateIdea` 호출로 컴파일 실패: 중복 호환 래퍼를 제거하고 기존 API `setIdea`로 연결한 뒤 전체 테스트·빌드 통과.
- Python 환경에 PyYAML이 없어 계약 검증 명령이 한 번 실패: Ruby 표준 YAML 파서로 재검증해 통과.

## 미완료·외부 작업

- GitHub 원격 저장소 자체 이름은 변경하지 않았고 원격 URL도 이전 이름을 유지한다. 원격 rename은 별도 승인과 GitHub 작업이 필요하다.
- 실제 기기 설치, 실행, 딥링크 runtime trace, 양 플랫폼 post-change 화면 캡처는 수행하지 않았다.
- 커밋, push, Backend 배포, 앱 릴리스는 수행하지 않았다.
