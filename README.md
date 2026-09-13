# Sharter

Android imageboard client for **[soyjak.st](https://soyjak.st)** (the sharty) and **[4chan](https://4chan.org)**. Forked from [Kuroba-Experimental](https://github.com/K1rakishou/Kuroba-Experimental).

## Features

- Board list, catalog, thread view, media viewer
- Bookmarks, filters, and Kuroba's browse UI
- Cloudflare challenge handling (soyjak.st)
- Deep links for both sites

## Install

The APK is published as a **[GitHub Release](https://github.com/clankerabuse/Sharter/releases/tag/debug-apk)**:

1. Download **`Sharter-dev-arm64-v8a.apk`** (phones). `Sharter-dev.apk` is a universal fallback.
2. Sideload it. Uninstall an older `Sharter-dev` build if Android blocks the install (same application id, different signing).

Each push to `develop` rebuilds that prerelease.

## Build

Open the `Kuroba/` directory in Android Studio, or run:

```bash
cd Kuroba
./gradlew :app:assembleDebug
```

## License

GPLv3 (inherited from Kuroba-Experimental). See [COPYING.txt](COPYING.txt).
