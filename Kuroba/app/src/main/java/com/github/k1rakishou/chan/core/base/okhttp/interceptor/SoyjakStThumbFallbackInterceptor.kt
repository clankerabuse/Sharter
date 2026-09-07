package com.github.k1rakishou.chan.core.base.okhttp.interceptor

import com.github.k1rakishou.chan.core.site.sites.vichan.soyjakst.SoyjakSt
import com.github.k1rakishou.core_logger.Logger
import okhttp3.Interceptor
import okhttp3.Response

/**
 * soyjak.st thumbs are not one extension: new posts are `.webp`, older/small files keep
 * `.png`/`.jpg`/`.gif`. Retry 404s with the other extension so catalog cells and the
 * media-viewer placeholder don't toast "Image not found" while `/src/` still loads.
 */
class SoyjakStThumbFallbackInterceptor : KurobaOkHttpInterceptor() {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    var response = chain.proceed(request)

    if (response.code != 404) {
      return response
    }

    val alternates = SoyjakSt.alternateThumbnailUrls(request.url)
    if (alternates.isEmpty()) {
      return response
    }

    for (alternate in alternates) {
      response.close()
      val retry = chain.proceed(request.newBuilder().url(alternate).build())
      if (retry.code != 404) {
        Logger.d(TAG, "[$okHttpType] thumb 404 ${request.url} -> ${retry.code} $alternate")
        return retry
      }
      response = retry
    }

    return response
  }

  companion object {
    private const val TAG = "SoyjakStThumbFallback"
  }
}
