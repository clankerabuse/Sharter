package com.github.k1rakishou.chan.core.site.sites.vichan.soyjakst

import com.github.k1rakishou.chan.core.site.SiteEndpoints
import com.github.k1rakishou.chan.core.site.common.CommonSite.CommonSiteUrlHandler
import com.github.k1rakishou.chan.core.site.common.vichan.VichanEndpoints
import com.github.k1rakishou.chan.core.site.sites.vichan.BaseVichanSite
import com.github.k1rakishou.model.data.board.ChanBoard
import com.github.k1rakishou.model.data.descriptor.BoardDescriptor
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor.CatalogDescriptor
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor.ThreadDescriptor
import okhttp3.HttpUrl

/**
 * soyjak.st (the sharty) — Vichan engine with /thread/{no}.json paths (not /res/).
 */
class SoyjakSt : BaseVichanSite(
  defaultDomain = "https://soyjak.st"
) {
  private val boards by lazy {
    val siteName = descriptor.siteName
    fun board(code: String, name: String): ChanBoard {
      return ChanBoard.create(BoardDescriptor.create(siteName, code), name).also { it.active = true }
    }
    buildList {
      add(board("soy", "soyjaks"))
      add(board("qa", "question & answer"))
      add(board("raid", "raid"))
      add(board("r", "requests and soy art"))
      add(board("craft", "minecraft"))
      add(board("int", "international"))
      add(board("pol", "politics"))
      add(board("a", "anime"))
      add(board("an", "animals"))
      add(board("asp", "sports"))
      add(board("biz", "business"))
      add(board("mtv", "music, television, video games"))
      add(board("r9k", "r9k"))
      add(board("tech", "technology"))
      add(board("v", "video games"))
      add(board("sude", "suicide watch"))
      add(board("x", "paranormal"))
      add(board("q", "q"))
      add(board("news", "news"))
      add(board("chive", "chive"))
    }
  }

  override val enabled: Boolean = true
  override val name: String = SITE_NAME
  override val urlHandler by lazy { SoyjakStUrlHandler(this) }
  override val endpoints by lazy { SoyjakStEndpoints(this) }
  override val staticBoards = boards

  class SoyjakStEndpoints(
    soyjakSt: SoyjakSt
  ) : VichanEndpoints(soyjakSt) {
    override fun thumbnailUrl(
      boardDescriptor: BoardDescriptor,
      spoiler: Boolean,
      customSpoilers: Int,
      arg: Map<String, String>?
    ): HttpUrl {
      requireNotNull(arg)

      return root.builder()
        .s(boardDescriptor.boardCode)
        .s("thumb")
        .s(arg["tim"] + thumbnailFileExtension(arg["ext"]))
        .url()
    }

    override fun thread(
      threadDescriptor: ThreadDescriptor,
      contentType: SiteEndpoints.ContentType,
      archive: Boolean
    ): HttpUrl? {
      return root.builder()
        .s(threadDescriptor.boardCode())
        .s("thread")
        .s(threadDescriptor.threadNo.toString() + ".json")
        .url()
    }
  }

  class SoyjakStUrlHandler(soyjakSt: SoyjakSt) : CommonSiteUrlHandler(soyjakSt) {
    override fun respondsTo(url: HttpUrl): Boolean {
      val host = url.host.removePrefix("www.")
      return host == "soyjak.st" || host == "soyjak.party"
    }

    override fun desktopUrl(chanDescriptor: ChanDescriptor, postNo: Long?, postSubNo: Long?): String? {
      return when (chanDescriptor) {
        is CatalogDescriptor -> {
          rootUrl.newBuilder()
            .addPathSegment(chanDescriptor.boardCode())
            .toString()
        }
        is ThreadDescriptor -> {
          val builder = rootUrl.newBuilder()
            .addPathSegment(chanDescriptor.boardCode())
            .addPathSegment("thread")
            .addPathSegment(chanDescriptor.threadNo.toString() + ".html")

          if (postNo != null && postNo > 0L) {
            builder.fragment(postNo.toString())
          }

          builder.toString()
        }
        else -> null
      }
    }
  }

  companion object {
    const val SITE_NAME: String = "soyjak.st"

    /**
     * soyjak.st re-encodes still-image thumbs as webp.
     * Catalog HTML uses `/board/thumb/{tim}.webp` for png/jpg/gif/webp originals;
     * `/board/src/{tim}.{ext}` stays the original file (so the media viewer works).
     */
    fun thumbnailFileExtension(originalExt: String?): String {
      return when (originalExt?.lowercase()) {
        "webm", "mp4" -> ".jpg"
        "png", "jpg", "jpeg", "gif", "webp" -> ".webp"
        else -> ".png"
      }
    }

    fun isThumbnailUrl(url: HttpUrl): Boolean {
      val host = url.host.removePrefix("www.")
      if (host != "soyjak.st" && host != "soyjak.party") {
        return false
      }

      val segments = url.pathSegments
      return segments.size >= 2 && segments[segments.lastIndex - 1] == "thumb"
    }

    /**
     * Thumbs are mixed: newer posts are `{tim}.webp`, older/small copies keep the original ext.
     * Cached posts may still have a `.png` thumb URL from before the webp mapping.
     */
    fun alternateThumbnailUrls(url: HttpUrl): List<HttpUrl> {
      if (!isThumbnailUrl(url)) {
        return emptyList()
      }

      val filename = url.pathSegments.last()
      val dot = filename.lastIndexOf('.')
      if (dot <= 0) {
        return emptyList()
      }

      val stem = filename.substring(0, dot)
      val ext = filename.substring(dot + 1).lowercase()
      val fallbacks = when (ext) {
        "webp" -> listOf("png", "jpg", "jpeg", "gif")
        "png", "jpg", "jpeg", "gif" -> listOf("webp")
        else -> listOf("webp", "png", "jpg")
      }

      val lastIndex = url.pathSegments.lastIndex
      return fallbacks.map { newExt ->
        url.newBuilder()
          .removePathSegment(lastIndex)
          .addPathSegment("$stem.$newExt")
          .build()
      }
    }
  }
}
