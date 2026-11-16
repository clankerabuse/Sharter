package com.github.k1rakishou.chan.features.webview.task

import android.webkit.CookieManager
import android.webkit.WebView
import com.github.k1rakishou.chan.core.base.okhttp.interceptor.CloudFlareInterceptor
import com.github.k1rakishou.chan.core.site.Site
import com.github.k1rakishou.chan.features.webview.WebViewTaskException
import com.github.k1rakishou.chan.features.webview.WebViewTaskResult
import com.github.k1rakishou.chan.features.webview.client.AbstractCookieWebViewClient
import com.github.k1rakishou.chan.features.webview.client.AbstractWebViewClient
import com.github.k1rakishou.common.CookieBuilder
import com.github.k1rakishou.common.StringUtils.asFormattedToken
import com.github.k1rakishou.common.domainOrHost
import com.github.k1rakishou.core_logger.Logger
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class CloudFlareTask(
  headerTitleText: String?,
  loadable: Loadable.Url,
  invokerWaiter: CompletableDeferred<WebViewTaskResult>
) : AbstractCookieWebViewTask(
  headerTitleText = headerTitleText,
  loadable = loadable,
  // Cloudflare might require user input. This depends on a lot of parameters.
  headlessMaxTime = 0L,
  // Cloudflare only fully passes the check when WebView is actually attached to the view hierarchy, for some reason.
  // Couldn't figure out why yet, so for now I will just display it invisibly for some time.
  invisibleMaxTime = 0L,
  invokerWaiter = invokerWaiter
) {
  override val tag: String = TAG

  override fun createWebClient(): AbstractWebViewClient {
    return CloudFlareTaskWebViewClient(
      webViewClientResultWaiter = this@CloudFlareTask.webViewClientResultWaiter,
      loadableUrl = loadable as Loadable.Url,
      cookieManager = cookieManager,
      initialCookies = initialCookies
    )
  }

  override suspend fun persistCookies(site: Site, cookies: String, userData: Any?) {
    val cloudFlareClearanceCookieSetting = site.commonSettings.cloudFlareClearanceCookieMap
    val urlToOpen = (loadable as Loadable.Url).url
    val key = urlToOpen.domainOrHost()

    Logger.debug(TAG) {
      "persistCookies() site: ${site.name}, key: '${key}', cookies: '${cookies.asFormattedToken()}'"
    }

    cloudFlareClearanceCookieSetting.put(
      key = key,
      value = cookies
    )

    val storedKeys = cloudFlareClearanceCookieSetting.read().keys
    Logger.debug(TAG) { "persistCookies() done, stored keys: ${storedKeys}" }
  }

  private class CloudFlareTaskWebViewClient(
    webViewClientResultWaiter: CompletableDeferred<WebViewTaskResult>,
    private val loadableUrl: Loadable.Url,
    private val cookieManager: CookieManager,
    private val initialCookies: AtomicReference<String>
  ) : AbstractCookieWebViewClient(webViewClientResultWaiter) {
    private val _requestId = AtomicLong(0)
    private val _recheckAttempts = AtomicInteger(0)

    override fun onPageFinished(view: WebView?, url: String?) {
      super.onPageFinished(view, url)

      if (view == null) {
        return
      }

      val currentRequestId = _requestId.getAndIncrement()

      view.postVisualStateCallback(currentRequestId, object : WebView.VisualStateCallback() {
        override fun onComplete(requestId: Long) {
          if (requestId != currentRequestId) {
            return
          }

          onPageVisible()
          checkForClearance(view)
        }
      })
    }

    /**
     * Cloudflare sets a `cf_clearance` cookie as soon as its challenge script starts running, not when the challenge
     * is passed, and an interactive challenge (the one with the "Verify you are human" checkbox) is not passed until
     * the user taps it. Finishing on the first cookie seen closes this screen before that tap can happen and stores a
     * clearance the origin refuses, so every following request comes back as "Rejected by CloudFlare." instead.
     *
     * So: while the WebView is still showing the interstitial, keep the screen open and re-check the cookie until the
     * page is not the interstitial anymore.
     * */
    private fun checkForClearance(view: WebView) {
      if (taskCompleted) {
        return
      }

      val newCookies = cookieManager.getCookie(loadableUrl.url.toString()) ?: ""
      val newCookiesBuilder = CookieBuilder(newCookies)
      val prevCookiesBuilder = CookieBuilder(initialCookies.get())

      val prevCfClearanceCookie = prevCookiesBuilder.get(CloudFlareInterceptor.COOKIE_CF_CLEARANCE)?.value
      val newCfClearanceCookie = newCookiesBuilder.get(CloudFlareInterceptor.COOKIE_CF_CLEARANCE)?.value
      val hasNewClearanceCookie =
        !newCfClearanceCookie.isNullOrBlank() && prevCfClearanceCookie != newCfClearanceCookie

      val challengePageShowing = isChallengePageShowing(view)

      // The clearance only counts once the interstitial is gone: while it is on screen the cookie it set belongs to
      // the challenge, not to the site. `null` means the page has no title at all yet (about:blank, an image), which
      // is normal for a non-captcha load, so after a few polls accept it rather than waiting for a title forever.
      val clearanceIsUsable = hasNewClearanceCookie && when (challengePageShowing) {
        true -> false
        false -> true
        null -> _recheckAttempts.get() >= TITLE_SETTLE_ATTEMPTS
      }

      Logger.verbose(TAG) {
        "checkForClearance() page title: '${view.title}', challenge page showing: ${challengePageShowing}, " +
          "has new cf_clearance: ${hasNewClearanceCookie}, usable: ${clearanceIsUsable}"
      }

      if (clearanceIsUsable) {
        newCookiesBuilder.retainAllIn(CloudFlareInterceptor.EXPECTED_CLOUDFLARE_COOKIES)
        success(newCookiesBuilder.build(), null)
        return
      }

      if (_recheckAttempts.getAndIncrement() < MAX_RECHECK_ATTEMPTS) {
        view.postDelayed({ checkForClearance(view) }, RECHECK_DELAY_MS)
        return
      }

      fail(WebViewTaskException("Cloudflare challenge was not passed in time"))
    }

    /**
     * `true` — Cloudflare's interstitial is on screen, `false` — the real page is, `null` — the WebView has no title
     * yet, so there is nothing to tell them apart by.
     * */
    private fun isChallengePageShowing(view: WebView): Boolean? {
      val title = view.title?.lowercase().orEmpty()
      if (title.isBlank()) {
        return null
      }

      return CLOUDFLARE_CHALLENGE_TITLES.any { challengeTitle -> title.contains(challengeTitle) }
    }

    companion object {
      private const val TAG = "CloudFlareTaskWebViewClient"
      private const val RECHECK_DELAY_MS = 700L
      private const val MAX_RECHECK_ATTEMPTS = 200

      // ~3 seconds of polling before a titleless page is accepted with whatever cookie it has
      private const val TITLE_SETTLE_ATTEMPTS = 4

      // Titles Cloudflare uses for the page it serves instead of the site while it wants a challenge passed. Kept in
      // sync with the needles in CloudFlareInterceptor.
      private val CLOUDFLARE_CHALLENGE_TITLES = listOf(
        "just a moment",
        "please wait",
        "checking your browser",
        "attention required",
        "verifying you are human",
        "one more step",
        "security check",
      )
    }
  }

  companion object {
    private const val TAG = "LoadCloudFlareTask"
  }
}