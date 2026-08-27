# Android settings and external-AI runtime trace

- Date: 2026-08-27 KST
- Device: Samsung SM-F968N (`192.168.0.103:44897`, hardware serial recorded by device operator)
- APK install: `adb install -r` returned `Success`; app data was not cleared.
- Launch: `com.armsone.starmanager/.BkIconAlias`; StarManager confirmed as resumed and focused.

## Settings route

1. Opened the bottom tab `나의 취향`.
2. Scrolled from top to bottom and captured UI XML/PNG.
3. Observed section order: `내 프리셋 → 기본 설정 → 글쓰기 취향 → 추가 옵션 → 프리셋 보관 → 외부 AI 로그인 관리 → 앱 업데이트 → 테마 관리`.
4. Observed external login provider order: `Gemini → ChatGPT → Claude`.
5. Observed composer AI choice order: `Gemini → ChatGPT → Claude → 기기 AI`.

## Login interaction

1. Tapped `로그인 열기` for ChatGPT.
2. Observed app-owned full-screen title `ChatGPT 로그인`, subtitle `공식 로그인 페이지`, and close action.
3. Observed the official ChatGPT page with `ChatGPT`, `로그인`, and its prompt surface rendered inside the WebView.
4. Closed the surface and returned to StarManager.
5. No credential entry or authentication was performed.

## Limits

- Gemini and Claude login navigation were not opened.
- Login persistence after authentication was not tested because no credentials were entered.
- Composer prompt injection, submission, stable-answer extraction, and import were not exercised.
- No paired post-change iOS captures exist, so visual parity remains open.
