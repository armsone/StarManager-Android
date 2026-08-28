# Android authenticated-login trace — 2026-08-28

Device: Samsung SM-F968N (`R3KYB061JTZ`)

1. Replaced the installed app with the freshly built debug APK using `adb install -r`; installation returned `Success`.
2. Opened `나의 취향` and scrolled to `외부 AI 로그인 관리`.
3. Within eight seconds, the attached off-screen provider probes resolved Gemini, ChatGPT, and Claude from `확인 중` to `로그인됨`.
4. Tapped the ChatGPT row while its existing authenticated web session was present.
5. The login surface detected positive authenticated DOM evidence, dismissed automatically, and returned to Settings within three seconds.
6. The ChatGPT row remained `로그인됨`; the success callback did not reset the confirmed state to `확인 중`.
7. Screen timeout was verified as `2147483647`, and `dumpsys power` reported `mStayOn=true`.

Evidence:

- `android_postchange_all_logged_in.png`
- `android_postchange_auto_dismiss.png`
