# Sharter

Android imageboard client for **[soyjak.st](https://soyjak.st)** (the sharty) and **[4chan](https://4chan.org)**. Forked from [Kuroba-Experimental](https://github.com/K1rakishou/Kuroba-Experimental).

Package id: `com.sharter.android`

This is not a general Kuroba rebuild. Only soyjak.st and 4chan are registered; lainchan, archives, and the rest of Kuroba’s site list stay out of the app.

## What works

**Both sites**
- Board picker, catalog, thread view, and media viewer
- Bookmarks, filters, and the rest of Kuroba’s browse UI
- **Settings → Sites** to enable, reorder, or add/remove boards

**soyjak.st**
- Vichan catalog/thread JSON (`/{board}/catalog.json`, `/{board}/thread/{no}.json`)
- Cloudflare challenge handling (WebView clearance cookies → OkHttp)
- Catalog thumbs, including PNG originals that the site serves as `.webp` previews
- Deep links: `https://soyjak.st/{board}/thread/{no}.html`
- Legacy host: `soyjak.party`

**4chan**
- Kuroba’s 4chan adapter (catalog, threads, posting, captcha, 4chan Pass)
- Board list from `boards.json` (fetched and added on first launch)
- Deep links: `https://boards.4chan.org/{board}/thread/{no}` and `4channel.org`

## Not yet

- soyjak.st posting / captcha / reply composer (`POST /post.php`)
- soybooru
- Other imageboards from upstream Kuroba

## Download

The APK is a **[GitHub Release](https://github.com/clankerabuse/Sharter/releases/tag/debug-apk)**, not a GitHub Package.

1. Open **[Releases](https://github.com/clankerabuse/Sharter/releases)** (log in if the repo is private).
2. Download **`Sharter-dev-arm64-v8a.apk`** (phones). `Sharter-dev.apk` is a universal fallback.
3. Sideload it. Uninstall an older `Sharter-dev` build if Android blocks the install (same application id, different signing).

Each push to `develop` rebuilds that prerelease. You can also run **Actions → Publish debug APK → Run workflow**.

Fallback copy in the tree: [apk/Sharter-dev-arm64-v8a.apk](apk/Sharter-dev-arm64-v8a.apk) (**Download raw file**).

## Build

Open the `Kuroba/` directory in Android Studio.

```bash
cd Kuroba
# Requires Android SDK (local.properties → sdk.dir)
./gradlew :app:assembleDebug -PbuildType=2
```

`buildType`: `0` = Stable, `1` = Beta, `2` = Dev (default in `gradle.properties`).

APKs land in `Kuroba/app/build/outputs/apk/debug/` (`Sharter-dev-arm64-v8a.apk` for phones).

## Cloudflare

soyjak.st is behind Cloudflare. Sharter reuses Kuroba’s `CloudFlareInterceptor` + WebView bypass:

1. First catalog/thread request may get a 403 challenge page.
2. The app opens a WebView, you pass the check, and it stores `cf_clearance` (and related) cookies.
3. OkHttp retries with those cookies; JSON should parse after that.

If catalog fails with “malformed JSON”, the response is still HTML — re-trigger the CF screen (or clear site cookies in site settings) and try again.

## Site adapters

| Site | Engine | Adapter |
|------|--------|---------|
| soyjak.st | Vichan (4chan-compatible JSON) | `Kuroba/app/src/main/java/.../sites/vichan/soyjakst/SoyjakSt.kt` |
| 4chan | Futaba / 4chan JSON | `Kuroba/app/src/main/java/.../sites/chan4/Chan4.kt` |

soyjak.st paths:

| Purpose | URL |
|--------|-----|
| Catalog | `/{board}/catalog.json` |
| Thread | `/{board}/thread/{no}.json` |
| Full media | `/{board}/src/{tim}{ext}` |
| Catalog thumb | `/{board}/thumb/{tim}.webp` (still images); `.jpg` posters for webm/mp4 |

## License

GPLv3 (inherited from Kuroba-Experimental). See [COPYING.txt](COPYING.txt).
