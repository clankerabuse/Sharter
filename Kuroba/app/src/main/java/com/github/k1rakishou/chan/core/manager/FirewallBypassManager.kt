package com.github.k1rakishou.chan.core.manager

import androidx.annotation.GuardedBy
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.concurrency.RendezvousCoroutineExecutor
import com.github.k1rakishou.chan.features.webview.WebViewTaskResult
import com.github.k1rakishou.chan.features.webview.task.AbstractWebViewTask
import com.github.k1rakishou.chan.features.webview.task.Chan8MoePowBlockTask
import com.github.k1rakishou.chan.features.webview.task.CloudFlareTask
import com.github.k1rakishou.chan.features.webview.task.DvachAntispamTask
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils.getString
import com.github.k1rakishou.common.FirewallType
import com.github.k1rakishou.common.domainOrHost
import com.github.k1rakishou.common.errorMessageOrClassName
import com.github.k1rakishou.common.rethrowCancellationException
import com.github.k1rakishou.core_logger.Logger
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.atomic.AtomicBoolean

class FirewallBypassManager(
  private val appScope: CoroutineScope,
  private val siteManager: SiteManager,
  private val webViewTaskManager: WebViewTaskManager
) {
  @GuardedBy("itself")
  private val firewallSiteInfoMap = mutableMapOf<String, FirewallSiteInfo>()
  @GuardedBy("itself")
  private val hostLastTimeCheck = mutableMapOf<String, Long>()

  private val rendezvousCoroutineExecutor = RendezvousCoroutineExecutor(appScope)

  suspend fun removeHostTimeCheckByChanDescriptor(chanDescriptor: ChanDescriptor) {
    siteManager.awaitUntilInitialized()

    val domainOrHost = siteManager.bySiteDescriptorAndActive(chanDescriptor.siteDescriptor())
      ?.urlHandler
      ?.desktopUrl(chanDescriptor, null, null)
      ?.toHttpUrlOrNull()
      ?.domainOrHost()

    Logger.debug(TAG) { "removeHostTimeCheckByChanDescriptor(${chanDescriptor}) -> '${domainOrHost}'" }

    if (domainOrHost.isNullOrBlank()) {
      return
    }

    synchronized(hostLastTimeCheck) { hostLastTimeCheck.remove(domainOrHost) }
  }

  /**
   * The challenge screen reported success but the request that followed was rejected by Cloudflare again, so the
   * clearance we stored is not being honored. Without this the host stays "recently checked" for
   * [FIREWALL_CHECK_TIMEOUT_MS] and every request in that window is rejected instantly without ever re-opening the
   * screen, which is what makes the Retry button look like it does nothing.
   *
   * Shortens the cooldown to [RETRY_AFTER_FAILED_BYPASS_MS] instead of clearing it outright, so a genuinely
   * unsolvable challenge cannot spam the screen once per request.
   * */
  fun onBypassDidNotWork(urlToOpen: HttpUrl) {
    val domainOrHost = urlToOpen.domainOrHost()

    synchronized(hostLastTimeCheck) {
      hostLastTimeCheck[domainOrHost] =
        System.currentTimeMillis() - (FIREWALL_CHECK_TIMEOUT_MS - RETRY_AFTER_FAILED_BYPASS_MS)
    }

    Logger.debug(TAG) {
      "onBypassDidNotWork('${urlToOpen}') retry allowed in ${RETRY_AFTER_FAILED_BYPASS_MS}ms for '${domainOrHost}'"
    }
  }

  fun onFirewallDetected(
    firewallType: FirewallType,
    urlToOpen: HttpUrl,
    onFinished: (success: Boolean) -> Unit
  ) {
    val domainOrHost = urlToOpen.domainOrHost()

    val showShowFirewallBypassScreen = synchronized(firewallSiteInfoMap) {
      val isCurrentlyShowing = firewallSiteInfoMap.get(domainOrHost)?.currentlyShowing == true

      if (isCurrentlyShowing) {
        firewallSiteInfoMap.get(domainOrHost)?.addWaiter(onFinished)
        return@synchronized ShowShowFirewallBypassScreen.WaitForExistingOne
      }

      val firewallSiteInfo = FirewallSiteInfo(onFinished)
      firewallSiteInfoMap[domainOrHost] = firewallSiteInfo

      val now = System.currentTimeMillis()
      val lastTimeChecked = synchronized(hostLastTimeCheck) {
        hostLastTimeCheck[domainOrHost] ?: 0L
      }

      if (now - lastTimeChecked < FIREWALL_CHECK_TIMEOUT_MS) {
        Logger.verbose(TAG) {
          "onFirewallDetected(${firewallType}, ${urlToOpen}) skipping because screen was shown not long ago " +
                  "(timeDelta: ${now - lastTimeChecked})"
        }

        firewallSiteInfoMap.remove(domainOrHost)
        return@synchronized ShowShowFirewallBypassScreen.DoNotShow
      }

      return@synchronized ShowShowFirewallBypassScreen.Show
    }

    when (showShowFirewallBypassScreen) {
      ShowShowFirewallBypassScreen.WaitForExistingOne -> {
        return
      }
      ShowShowFirewallBypassScreen.DoNotShow -> {
        onFinished.invoke(false)
        return
      }
      ShowShowFirewallBypassScreen.Show -> {
        // no-op
      }
    }

    Logger.debug(TAG) {
      "onFirewallDetected(${firewallType}, '${urlToOpen}') Sending event to show SiteFirewallBypassController"
    }

    rendezvousCoroutineExecutor.post {
      var success = false
      var canceledByUser = false

      try {
        synchronized(firewallSiteInfoMap) {
          firewallSiteInfoMap[domainOrHost]?.onStarted()
        }

        Logger.debug(TAG) {
          "onFirewallDetected(${firewallType}, '${urlToOpen}') Waiting for result from SiteFirewallBypassController..."
        }

        val resultWaiter = CompletableDeferred<WebViewTaskResult>()

        val webViewTask = when (firewallType) {
          FirewallType.Cloudflare -> {
            CloudFlareTask(
              headerTitleText = getString(R.string.firewall_check_header_title, firewallType.name),
              loadable = AbstractWebViewTask.Loadable.Url(urlToOpen),
              invokerWaiter = resultWaiter
            )
          }
          FirewallType.DvachAntiSpam -> {
            DvachAntispamTask(
              headerTitleText = getString(R.string.firewall_check_header_title, firewallType.name),
              loadable = AbstractWebViewTask.Loadable.Url(urlToOpen),
              invokerWaiter = resultWaiter
            )
          }
          FirewallType.Chan8MoePowBlock -> {
            Chan8MoePowBlockTask(
              headerTitleText = getString(R.string.firewall_check_header_title, firewallType.name),
              loadable = AbstractWebViewTask.Loadable.Url(urlToOpen),
              invokerWaiter = resultWaiter
            )
          }
          FirewallType.YandexSmartCaptcha -> {
            error("Handled in ImageSearchController")
          }
        }

        val taskResult = webViewTaskManager.performWebViewTask(webViewTask)
        success = taskResult.isSuccess
        // Backing out of the screen is not a failed check: do not hold the next attempt off for the whole cooldown,
        // or pressing the Retry button right after a cancel does nothing at all.
        canceledByUser = taskResult is WebViewTaskResult.Canceled

        Logger.debug(TAG) {
          "onFirewallDetected(${firewallType}, '${urlToOpen}') Waiting for result from " +
                  "SiteFirewallBypassController... done, success: ${success}, canceledByUser: ${canceledByUser}"
        }
      } catch (error: Throwable) {
        Logger.error(TAG) {
          "onFirewallDetected(${firewallType}, '${urlToOpen}') Waiting for result from " +
                  "SiteFirewallBypassController... done, error: ${error.errorMessageOrClassName()}"
        }

        error.rethrowCancellationException()
      } finally {
        synchronized(firewallSiteInfoMap) {
          firewallSiteInfoMap.remove(domainOrHost)
            ?.onFinished(success)

          synchronized(hostLastTimeCheck) {
            if (canceledByUser) {
              hostLastTimeCheck.remove(domainOrHost)
            } else {
              hostLastTimeCheck[domainOrHost] = System.currentTimeMillis()
            }
          }
        }
      }
    }
  }

  enum class ShowShowFirewallBypassScreen {
    Show,
    WaitForExistingOne,
    DoNotShow
  }

  class FirewallSiteInfo(
    waiter: (Boolean) -> Unit
  ) {
    @GuardedBy("this")
    private val waiters = mutableListOf<(Boolean) -> Unit>()

    private val _currentlyShowing = AtomicBoolean(false)
    val currentlyShowing: Boolean
      get() = _currentlyShowing.get()

    init {
      waiters += waiter
    }

    @Synchronized
    fun addWaiter(waiter: (Boolean) -> Unit) {
      waiters += waiter
    }

    @Synchronized
    fun onStarted() {
      _currentlyShowing.set(true)
    }

    @Synchronized
    fun onFinished(success: Boolean) {
      waiters.forEach { waiter -> waiter.invoke(success) }
      waiters.clear()
      _currentlyShowing.set(false)
    }

  }

  companion object {
    private const val TAG = "FirewallBypassManager"
    private const val FIREWALL_CHECK_TIMEOUT_MS = 10_000L
    private const val RETRY_AFTER_FAILED_BYPASS_MS = 3_000L
  }

}