# Sharter

Android imageboard client for **[soyjak.st](https://soyjak.st)** (the sharty) and **[4chan](https://4chan.org)**. Forked from [Kuroba-Experimental](https://github.com/K1rakishou/Kuroba-Experimental), GPLv3.

## Features

- Board list, catalog, thread view, media viewer, bookmarks and filters from Kuroba
- soyjak.st support, including its vichan quirks (webp thumbs, `/thread/{no}.json`, spoiler placeholder)
- Cloudflare challenge handling for both sites (WebView bypass)
- Deep links for both sites

## Install

Every push to `develop` publishes a signed **release** build as a GitHub Release: **[latest](https://github.com/clankerabuse/Sharter/releases/tag/latest)**.

1. Download **`Sharter-arm64-v8a.apk`** (phones). `Sharter.apk` is a universal fallback.
2. Sideload it.

Builds published before v1.3.48 were debug-signed; Android will refuse to update
over one of those. Uninstall the old `Sharter-dev` app once, then install this.

## Build

Open the `Kuroba/` directory in Android Studio, or:

```bash
cd Kuroba
./gradlew :app:assembleDebug          # Sharter-dev, debug key, no ABI splits
./gradlew :app:assembleRelease -PbuildType=0
```

A release build needs a keystore. Either drop `Kuroba/app/release.properties`
(`keystoreFile`, `keystorePass`, `keyAlias`, `keyPass`; gitignored) next to the app
module, or pass AGP's injected signing properties:

```bash
./gradlew :app:assembleRelease -PbuildType=0 \
  -Pandroid.injected.signing.store.file=/path/to/release.jks \
  -Pandroid.injected.signing.store.password=… \
  -Pandroid.injected.signing.key.alias=… \
  -Pandroid.injected.signing.key.password=…
```

CI reads the keystore from the `KEYSTORE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and
`KEY_PASSWORD` repo secrets.

## Versioning and releases

- `versionName` (`v1.3.48`) is bumped by hand in `Kuroba/app/build.gradle.kts` when
  something is published.
- `versionCode` is derived from the clock (minutes since epoch) so every build is
  newer than the last and sideloaded updates never hit Android's downgrade check.
  Pass `-PversionCodeOverride=<n>` for a reproducible build.
- Only `.github/workflows/publish-apk.yml` publishes. `apk/` and `*.apk` are
  gitignored — no binaries in the tree.
- `debug-apk` is retired; use the `latest` prerelease.

## Upstream

`develop` tracks `K1rakishou/Kuroba-Experimental`. `.github/workflows/upstream-sync.yml`
runs weekly: it merges upstream `develop` into an `upstream-sync` branch and opens a
PR when the merge is clean, or an issue listing the conflicted files when it is not.
Resolve conflicts in favour of upstream unless the change touches Sharter's site
registry, branding, or the soyjak.st adapter.

## License

GPLv3 (inherited from Kuroba-Experimental). See [COPYING.txt](COPYING.txt).
