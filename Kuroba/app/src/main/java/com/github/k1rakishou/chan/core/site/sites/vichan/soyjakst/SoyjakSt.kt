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
  }
}
