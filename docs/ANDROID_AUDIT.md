# Android Audit

This doc captures the invariants the PlausiDen Android app MUST hold, and
how to verify them. Three layers:

1. **Static lint** (`scripts/android-lint.sh`) — grep audit of Kotlin
   sources + `AndroidManifest.xml` + `res/**` + `gradle.properties`. Fast,
   CI-safe.
2. **Gradle linter** — `./gradlew :app:lint`. Runs Android's built-in
   Lint against the full source tree. CI should fail on any `Error`-level
   issue.
3. **Manual checklist** (below) — device gates the human runs on a real
   or emulated phone / tablet before a release build.

---

## Invariants

### Layout
- **Tap targets ≥ 48 × 48 dp** for every interactive element (Material
  spec). Icon-only `@dimen/touch_target_min` wraps them to 48dp
  regardless of drawable size.
- **Minimum text size ≥ 12sp** for body content; navigation captions can
  be 10sp with an accessibility-scalable marker (`android:textSize`
  expressed in sp, never px/dp).
- **Responsive layouts**: `res/layout/` uses `ConstraintLayout` or Compose
  with `Modifier.fillMaxWidth()` + proportional weight — no hardcoded
  widths > 320dp on containers.
- **Dark + Light theme parity**: `res/values-night/` overrides colors; every
  string/drawable tested in both modes.
- **Landscape + portrait**: no layout crashes under orientation change;
  scroll position preserved.

### Accessibility
- **TalkBack**: every Activity announces via `android:contentDescription`
  or `setContentDescription(...)`. List items expose their item text.
- **Focus order** follows visual order (Material guidance). `nextFocus*`
  attributes set where the default DFS is wrong.
- **High-contrast mode** honored — `colorOnPrimary` / `colorOnSurface`
  meet AA 4.5:1 against their backgrounds.
- **Font scale** up to 200% without content clipping or horizontal scroll.
- **Disable animations** honored (`Settings.Global.ANIMATOR_DURATION_SCALE`).

### Security (see OPSEC.md + CLAUDE.md)
- **No plaintext HTTP** to the backend — `networkSecurityConfig` enforces
  TLS with pinned cert. Emulator debug build allows `10.0.2.2:3000` via
  a debug-only override.
- **Exported components** in `AndroidManifest.xml` minimal: only
  `MainActivity` with `android:exported="true"`. Everything else
  (services, receivers) is `exported="false"`.
- **Secrets** never in `strings.xml` or `BuildConfig` — injected from the
  keystore at build time.
- **WebView** (if used) with JS interface: zero `@JavascriptInterface`
  exports; content loaded from `https://` only, never `file://`.
- **Deep links**: `plausiden://fact/{key}` routes to `MainActivity`. The
  intent filter's `android:autoVerify="true"` so Android App Links are
  preferred over the browser.
- **Dangerous permissions**: explicit runtime request at point of use, not
  on launch. Only `POST_NOTIFICATIONS` needed currently.

### Performance
- **Cold start** < 1200ms on Pixel 6 equivalent.
- **APK size** < 20MB (base) — vectors, no per-density bitmaps.
- **ANR-free**: no blocking I/O on the main thread. `ProfileManager`'s
  disk reads use `Dispatchers.IO`.
- **Memory** < 60MB baseline heap on a mid-range device.
- **Battery**: `PollutionService` uses `WorkManager` periodic tasks, not
  a long-running foreground service, unless state actively requires one.

### Offline behaviour
- **Cached fact library** on device (10MB LRU SQLite via Room) served when
  backend WS unreachable.
- **Outbox** for chat turns when offline; drained on reconnect (parity
  with web dashboard outbox).
- **Clear offline indicator** in toolbar when WS is down.

---

## Manual pre-release checklist

Run on BOTH a physical Android 13+ device AND an emulator at API 26
(minSdk floor) before publishing. Tick each:

- [ ] **First launch**: splash → MainActivity in < 1.5s, no ANR.
- [ ] **Permissions prompt**: `POST_NOTIFICATIONS` requested ONLY at point
      of first notification, not on boot.
- [ ] **Back button**: from any tab → previous tab → MainActivity root →
      confirms exit (doesn't insta-kill).
- [ ] **Configuration change**: rotate portrait/landscape during chat;
      scroll position + unsent input preserved.
- [ ] **System font scale**: System Settings → Display → Large → relaunch;
      no text clipped, all tap targets still reachable.
- [ ] **TalkBack**: swipe-through every Activity; all interactive elements
      announced, navigation order matches visual.
- [ ] **Deep link**: `adb shell am start -W -a android.intent.action.VIEW
      -d "plausiden://fact/volcano" com.plausiden.app` opens the fact view.
- [ ] **Offline**: airplane mode → chat still usable in outbox; toolbar
      shows offline indicator; data fresh on reconnect.
- [ ] **Low-memory**: `adb shell am send-trim-memory com.plausiden.app
      MODERATE` — app survives, loses nothing except caches.
- [ ] **Background**: put app in background for 10 min; notifications
      arrive for chat replies; tapping launches the correct conversation.
- [ ] **Uninstall**: all app data cleared (`adb shell pm clear` then
      uninstall); no secrets linger in logcat.
- [ ] **Android Lint**: `./gradlew :app:lint` reports 0 `Error` issues
      (warnings OK to triage).

---

## Known-good Kotlin patterns

```kotlin
// ✅ Coroutine-scoped I/O off main thread
viewModelScope.launch(Dispatchers.IO) {
    val fact = repo.fetchFact(key)
    withContext(Dispatchers.Main) { _fact.value = fact }
}

// ✅ Localized strings via res/values/strings.xml
context.getString(R.string.fact_copied_toast, key)

// ✅ Tap target meets 48dp via layout wrapper
<ImageButton
    android:layout_width="48dp"
    android:layout_height="48dp"
    android:src="@drawable/ic_close_24dp"
    android:contentDescription="@string/close" />
```

## Anti-patterns caught by `android-lint.sh`

```kotlin
// ❌ Hardcoded strings in Activity code
textView.text = "Copied!"  // should be getString(R.string.copied)

// ❌ Blocking network on main thread
val body = URL(url).readText()  // move to Dispatchers.IO

// ❌ `android:exported="true"` on services/receivers
<service android:exported="true" .../>

// ❌ Plaintext http in network_security_config
<domain includeSubdomains="true">example.com</domain>  // cleartext default

// ❌ `@JavascriptInterface` without origin check
webView.addJavascriptInterface(NativeBridge(), "plausiden")

// ❌ `px` / `pt` for text size
android:textSize="14px"  // must be sp
```
