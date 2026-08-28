# Composer first-screen parity review — 2026-08-28

## Reference evidence

- iPhone capture: `/Users/armsone/Library/Containers/cc.ffitch.shottr/Data/tmp/cc.ffitch.shottr/SCR-20260828-iztl.png`
- Dimensions: 1060 × 2064
- SHA-256: `0f2d4d10defcc10e0ee7fe58335fb90c3612c5a2d255d0c1b1b18dba51b7226d`
- Android pre-change capture: supplied by the user in the task conversation. The original local JPG was no longer present when evidence was assembled, so its dimensions and hash are not asserted here.
- Android post-change capture: `android_postfix.png`
- Dimensions: 822 × 1918
- SHA-256: `4b95aacce7eded238d9376d70ef5550f64a304ba7b4b8b214272d2ebd98e5236`
- Device: Samsung SM-F968N (`R3KYB061JTZ`), portrait
- Installed candidate: StarManager 2.2.0 (`versionCode=344612`)

## Reopened mismatches

| Area | iPhone reference | Android pre-change observation | Current implementation state |
|---|---|---|---|
| AIBI entry | Each provider card starts its task with one tap | Provider selection followed by a separate Run button | Direct-action wiring implemented; runtime trace pending |
| Top bar | Plain centered title and white cancel pill | Title icon, red text cancel, divider | Source corrected; post-change capture pending |
| Creation heading | Text-only heading with red underline | Leading black icon-well, no underline | Source corrected; post-change capture pending |
| Style summary | Summary row between idea and AI cards | Row absent | Source corrected; post-change capture pending |
| AI cards | Compact neutral cards; no selection outline | Taller cards with red selected outline | Source corrected; post-change capture pending |
| Preview empty row | Small gray document symbol | Second heavy black icon-well | Source corrected; post-change capture pending |
| Bottom navigation | Floating white pill and neutral selected capsule | Full-width bar with red selection | Source corrected; post-change capture pending |

## Verification status

- Focused JVM tests: passed.
- Debug APK assembly: passed.
- Post-change Android capture: recorded after data-preserving installation and app restart.
- Device display: `screen_off_timeout=2147483647`; stay-awake while powered enabled for AC, USB, and wireless charging.
- Runtime AIBI tap trace: pending. A live provider submission must not be triggered without an intentional test prompt and authorization.
- Matchup/AIBI parity completion: open.
