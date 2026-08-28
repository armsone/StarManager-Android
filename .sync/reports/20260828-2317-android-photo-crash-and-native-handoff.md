# Android photo orientation, responsiveness, crash, and native handoff correction

- Run start: 2026-08-28 22:54:16 KST (`monotonic=358564.278739583`)
- Report checkpoint: 2026-08-28 23:17:36 KST (`monotonic=359964.400029208`)
- Elapsed: 23m 20.1s
- Resolved group: StarManager iOS reference, StarManager Android target, independent AIBI package
- Scope: correct Android portrait orientation and photo-memory behavior, then replace the failing Gemini attachment-menu handoff with the public native multiple-file callback.

## Actual synchronization table

| Capability | iOS/reference state | Android implementation state | Technical/runtime evidence | Matchup evidence | Verdict |
|---|---|---|---|---|---|
| Selected-photo orientation | Runtime/user verified | EXIF-aware gallery and camera preparation plus bounded display decode installed | JVM policy tests, build, lint, phone/tablet install and launch passed | Fresh portrait capture pending | source-only |
| One-to-eight photo responsiveness | Runtime/user verified | Sources are ingested sequentially; thumbnails use 320px RGB_565 and active preview uses 1400px off-main decode | JVM tests, build, lint; phone PSS 160MB after corrected launch | Eight-photo interaction trace pending | source-only |
| Compose bitmap lifetime | Reference does not expose Android bitmap lifecycle | Explicit UI `Bitmap.recycle()` removed after physical-device crash proved a display-list race | Crash: `Canvas: trying to use a recycled bitmap` at 23:12:07; corrected build/install/relaunch; no new crash in immediate monitor | Repeated paging trace pending | source-only |
| Gemini native attachment handoff | User verified iPhone Gemini | Android uses a visible native-handoff surface, returns the ordered URI array according to the actual chooser mode, and verifies semantic attachment controls | Authenticated ADB run selected 5, delivered 5 URIs, confirmed 5 attachments, submitted, and entered generation without a crash | Fresh five-photo behavior trace recorded | runtime-matched |
| ChatGPT and Claude attachment handoff | User verified iPhone providers | Same native multiple-file bridge applies without Gemini menu nesting | Source/build/install verified | Authenticated reruns pending | source-only |
| Fail-closed recovery | iOS exact-count recovery exists | Prompt submission still requires the exact requested preview count; native preparation failure becomes explicit recovery | Existing user-observed six-photo banner plus regression tests | Positive corrected handoff and negative rerun pending | source-only |

## Corrections

- Gallery and camera photos are processed one source at a time away from the main thread.
- Every EXIF orientation variant is applied before the composer retains a JPEG copy.
- Retained composer copies are bounded to a 4,096px long edge and 8MB; AIBI copies retain the portable 2,048px/2MB ceiling.
- Thumbnail and pager images no longer decode at full source resolution.
- Gemini's visible `파일` action is discovered semantically, then the provider input opens Android's public file-chooser callback.
- Live sanitized inspection showed Gemini Android's photo input is single-file. AIBI now returns one URI per callback in selection order, reopens the input, and requires the preview count to increase before advancing. Providers with a multiple input still receive the full array at once.
- A first attempted bitmap-lifetime optimization explicitly recycled display bitmaps. The user-reported crash and device crash buffer proved Compose could still draw the previous display list. Manual recycling was removed; bounded bitmaps are now owned by Compose/GC.

## Per-project validation

- StarManager Android: 48 JVM tests, `assembleDebug`, and `lintDebug` passed on the final build.
- Exact installed APK: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `93592894cbb30d03e146c33d14b33c1a711ac47cbebc8cf9d7bb423d552123c3`.
- Data-preserving replacement install and relaunch passed on Samsung SM-F968N (`192.168.0.142:5555`), SM-T500 (`192.168.0.152:5555`), and the newly connected SM-F956N (`192.168.0.166:5555`).
- Corrected phone process remained alive with no new `AndroidRuntime` crash during the immediate monitor; observed PSS was approximately 160MB.
- Independent AIBI: 18 Python contract tests and JavaScript media regression passed; consumer sync status is current; canonical and installed Android engine/media assets are byte-identical.
- Diff hygiene: `git diff --check` passed in both repositories.

## Matchup and open evidence

- Ledger structure: 12 rows total; selected-image provider handoff is now matched by a fresh sanitized five-photo behavior trace.
- Completion gate: correctly failed. Open rows are selected-image provider handoff, video exclusion, failure recovery, portrait orientation, eight-photo responsiveness, and bitmap lifecycle.
- The user's provider screenshot was not copied into either repository because it contains account-visible content. Only the sanitized state `Gemini Android menu open; six requested photos unattached` was recorded.

## Intentional differences

- iOS uses its WKWebView file-panel URL contract. Android uses `FileProvider` content URIs through `WebChromeClient`, with a bounded WebView `DataTransfer` fallback.
- Android host previews use platform-specific sampled bitmap decoding; the common outcome remains upright, responsive ordered media.

## Repository and release state

- Both worktrees remain dirty with the user's existing and current local changes.
- No commit, push, release, website update, or external publication was performed.
- The exact runtime-matched APK is installed and launched on all three connected Android devices. Remaining open evidence is limited to the separately tracked portrait, paging/memory, mixed-media, failure-recovery, and non-Gemini provider rows.
