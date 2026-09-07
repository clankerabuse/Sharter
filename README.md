# Sharter

Android client for **[soyjak.st](https://soyjak.st)** (the sharty), forked from [Kuroba-Experimental](https://github.com/K1rakishou/Kuroba-Experimental).

This app is **soyjak.st-only** — other imageboards are not included.

## Features (browse MVP)

- Board list, catalog, thread view, media viewer
- Cloudflare challenge handling (WebView clearance cookies → OkHttp)
- Deep links: `https://soyjak.st/{board}/thread/{no}.html`
- Legacy host alias: `soyjak.party`

## Not yet

- Posting / captcha / reply composer (`POST /post.php`)
- Bookmarks polish, archives, soybooru

## Build

Open the `Kuroba/` directory in Android Studio (wait for SDK platform install to finish if Studio is still downloading components).

```bash
cd Kuroba
# Requires Android SDK (local.properties with sdk.dir — already points at ~/Android/Sdk)
./gradlew :app:assembleDebug -PbuildType=2
```

`buildType`: `0` = Stable, `1` = Beta, `2` = Dev (default in `gradle.properties`).

Application id: `com.sharter.android`

## Cloudflare

soyjak.st is behind Cloudflare. Sharter reuses Kuroba’s `CloudFlareInterceptor` + WebView bypass:

1. First catalog/thread request may get a 403 challenge page.
2. App opens a WebView to pass the check and stores `cf_clearance` (and related) cookies.
3. OkHttp retries with those cookies; JSON should parse after that.

If catalog fails with “malformed JSON”, the response is still HTML — re-trigger the CF screen (or clear site cookies in site settings) and try again.

## API notes

soyjak.st runs Vichan with 4chan-compatible JSON:

| Purpose | URL |
|--------|-----|
| Catalog | `/{board}/catalog.json` |
| Thread | `/{board}/thread/{no}.json` |
| Media | `/{board}/src/{tim}{ext}` |

Site adapter: `Kuroba/app/src/main/java/.../sites/vichan/soyjakst/SoyjakSt.kt`

## License

GPLv3 (inherited from Kuroba-Experimental). See [COPYING.txt](COPYING.txt).
