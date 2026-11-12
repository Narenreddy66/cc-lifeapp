# LIFE CC — standalone Call Centre app (Android WebView wrapper)

A tiny Android app that opens **only** the CC Lite page
(`https://portal.lifescc.com/cc-lite`) full-screen, like a separate app.
It does not contain the other LIFE modules, so it starts fast.

The actual CC app (screens, queue, dialer, reports) is the **web page**
`cc-lite` on the portal. This wrapper just shows that page as an app,
handles the phone dialer / WhatsApp links, the hardware Back button,
and pull-to-refresh.

---

## Three ways to get the installable APK

### A) Build in the cloud with GitHub Actions (no Android Studio) — easiest
1. Put this folder in a GitHub repo (private is fine).
2. Open the repo → **Actions** tab → run **"Build CC Lite APK"**
   (it also runs automatically on every push).
3. When it finishes, open the run → **Artifacts** → download **LIFE-CC-apk**.
4. Unzip → `app-debug.apk` → send to the agents' phones and install
   (enable "Install unknown apps" once).

### B) Build locally (Android Studio or command line)
- Android Studio: **Open** this folder → wait for Gradle sync →
  **Build ▸ Build APK(s)** → locate `app/build/outputs/apk/debug/app-debug.apk`.
- Command line (with Android SDK + JDK 17 installed):
  ```
  ./gradlew assembleDebug
  ```
  APK appears at `app/build/outputs/apk/debug/app-debug.apk`.

### C) Reuse Naren's existing LIFE APK wrapper
If Naren already builds the main LIFE APK, the CC app is the same wrapper
with four changes:
- start URL → `https://portal.lifescc.com/cc-lite`
- app name → **LIFE CC**
- package / applicationId → `com.lifescc.cc`
- app icon → the green "L" icon

---

## Change the page it opens
Edit one line in `app/src/main/res/values/strings.xml`:
```xml
<string name="start_url">https://portal.lifescc.com/cc-lite</string>
```

## Notes
- The APK is **debug-signed** → perfect for sideloading inside the company,
  not for the Play Store (which you said you don't need). For a Play Store
  build later, add a release `signingConfig` with your keystore.
- minSdk 24 (Android 7.0) → covers virtually every current phone.
- The web app already calls `window.LifeApp.dial()` / `openExternal()`;
  this wrapper implements both, so Call and WhatsApp open the native apps.
