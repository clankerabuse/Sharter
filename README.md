# Sharter

Android imageboard client for **[soyjak.st](https://soyjak.st)** and **[4chan](https://4chan.org)**. Fork of [Kuroba-Experimental](https://github.com/K1rakishou/Kuroba-Experimental).

## Install

Every push to `develop` publishes a signed release to the **[latest](https://github.com/clankerabuse/Sharter/releases/tag/latest)** release.

1. Download `Sharter-arm64-v8a.apk` (phones). `Sharter.apk` is a universal fallback.
2. Sideload it.

Builds before v1.3.48 were debug-signed and Android refuses to update over them —
uninstall the old `Sharter-dev` once.

## Build

Open `Kuroba/` in Android Studio, or:

```bash
cd Kuroba
./gradlew :app:assembleDebug                  # Sharter-dev, debug key
./gradlew :app:assembleRelease -PbuildType=0  # needs a keystore
```

A release build reads `Kuroba/app/release.properties` (gitignored) or AGP's
`-Pandroid.injected.signing.*` properties. CI takes the keystore from the
`KEYSTORE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` secrets.

`versionCode` is minutes since epoch, so a sideloaded update always outranks the
build before it. Only `.github/workflows/publish-apk.yml` publishes, and `*.apk`
stays out of the tree.

## Upstream

`develop` tracks Kuroba-Experimental. `upstream-sync.yml` merges upstream weekly
into a PR (or files an issue listing the conflicts). Resolve in favour of
upstream unless the change touches the site registry, branding, or the soyjak.st
adapter.

## License

GPLv3, inherited from Kuroba-Experimental. See [COPYING.txt](COPYING.txt).
