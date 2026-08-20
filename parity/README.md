# Matchup evidence

`fixtures/catalog-v1.json`은 동일 상태를 재생하기 위한 결정적 상태·프로필 목록이고,
`matrix/parity.csv`는 원자 단위 패리티 판정표입니다.

현재 iOS 저장소에는 UI test target, 안정 identifier, 상태 fixture 및 최신 lossless capture가 없습니다.
따라서 해시가 같은 원본 이미지와 단위 테스트로 확인한 동작만 `matched`로 표시했고,
나머지는 `implemented from source; visual parity unverified` 또는 `확인 필요`로 남겼습니다.

Android 앱은 디버그 인텐트 extra `starmanager.fixture.zeroDelay=true`와
`starmanager.fixture.resetState=true`, 그리고 `composer.*`, `preview.*`, `settings.*`, `tab.*`
semantics tag를 제공합니다. 실제 픽셀 판정은 같은 locale/timezone/theme/font scale과 앱 bounds로
iOS·Android PNG를 각각 두 번 캡처해 반복 SHA-256을 확인한 뒤 수행해야 합니다.
