# RoraFreeze

Put unused apps on ice.

RoraFreeze lets you pause (freeze) the apps you rarely use to save battery, cut down on distractions, and make your phone feel faster. When you need an app again, just thaw (unfreeze) it with one tap.

Made by [Prince Vic Lacson Mayordo (PVLM)](https://pvlm.site).

## What it does

- Freeze and unfreeze apps using Shizuku or Root.
- Group apps into your own custom modes (for example "Gaming", "Work Mode").
- Built-in app picker shows apps by safety level.
- Detect active apps to help you choose what to freeze.
- Export and import your modes as `.prrf` files.

### Important

You use this app at your own risk. Freezing system apps can break your device or make it unusable. Do not freeze apps you do not understand. Research an app first before freezing it.

## Requirements

- Android 8.0 or higher (API 26+)
- Shizuku (recommended) or Root for the freeze/unfreeze commands
- Download the latest APK from the [Releases](https://github.com/pvlmgit/RORA-PREEZE/releases) page

## How to install

1. Download the latest **.apk** file from the [Releases](https://github.com/pvlmgit/RORA-PREEZE/releases) page.
2. Open the file on your phone. If Android asks "Install unknown apps", allow it for your file manager or browser.
3. Tap **Install** and open the app after it finishes.
4. In the app, open **Settings** and grant Shizuku (recommended) or Root permission.
5. Add a mode, pick the apps you want to freeze, and tap **Freeze**.

## Permissions

- **Shizuku** or **Root** — Used to run the freeze/unfreeze commands.
- **Usage access** — Used to see which apps are active, so we can protect them from freezing.
- **Query all packages** — Used only to list the apps installed on your phone.

None of your personal data is collected or shared. Freeze/unfreeze commands run only on your own device.

## How to build from source

You need Android Studio (or a Gradle setup) and JDK 17+.

```bash
git clone https://github.com/pvlmgit/RORA-PREEZE.git
cd FreezeApps
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Credits

RoraFreeze is built on the open-source **Essentials** app by [Sameera Sandakelum](https://sameerasw.com/essentials):

- Copyright (c) 2025 Sameera Sandakelum
- Copyright (c) 2026 Prince Vic Lacson Mayordo (PVLM)

We stripped Essentials down to the App Freezing feature and rebuilt it as RoraFreeze. Everything here is under the [MIT License](LICENSE).

- Homepage: https://pvlm.site
- Feedback &amp; bug reports: [pvlm.contact@gmail.com](mailto:pvlm.contact@gmail.com)

## License

[MIT](LICENSE) — see the LICENSE file for the full text. In simple words: you can use, copy, change, and share this code freely, even in commercial projects, as long as you keep the copyright notice and license text. The code comes "as is", with no warranty.

Third-party libraries used:

- [Shizuku](https://shizuku.rikka.app/) — API for running commands without root.
- [Hidden API Bypass](https://github.com/LSPosed/AndroidHiddenApiBypass) — for advanced Android API access.
- [Jetpack Compose](https://developer.android.com/jetpack/compose), [AndroidX](https://developer.android.com/jetpack), [Gson](https://github.com/google/gson) — standard Android libraries.