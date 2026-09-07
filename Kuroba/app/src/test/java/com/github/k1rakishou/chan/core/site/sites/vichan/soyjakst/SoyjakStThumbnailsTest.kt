package com.github.k1rakishou.chan.core.site.sites.vichan.soyjakst

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class SoyjakStThumbnailsTest {

  @Test
  fun `png originals use webp catalog thumbs`() {
    assertEquals(".webp", SoyjakSt.thumbnailFileExtension("png"))
    assertEquals(".webp", SoyjakSt.thumbnailFileExtension("PNG"))
  }

  @Test
  fun `other still images also use webp thumbs`() {
    assertEquals(".webp", SoyjakSt.thumbnailFileExtension("jpg"))
    assertEquals(".webp", SoyjakSt.thumbnailFileExtension("jpeg"))
    assertEquals(".webp", SoyjakSt.thumbnailFileExtension("gif"))
    assertEquals(".webp", SoyjakSt.thumbnailFileExtension("webp"))
  }

  @Test
  fun `video posters stay jpg`() {
    assertEquals(".jpg", SoyjakSt.thumbnailFileExtension("webm"))
    assertEquals(".jpg", SoyjakSt.thumbnailFileExtension("mp4"))
  }

  @Test
  fun `webp thumb 404 falls back to png then jpg`() {
    val url = "https://soyjak.st/soy/thumb/123abc.webp".toHttpUrl()
    val alts = SoyjakSt.alternateThumbnailUrls(url).map { it.toString() }

    assertEquals(
      listOf(
        "https://soyjak.st/soy/thumb/123abc.png",
        "https://soyjak.st/soy/thumb/123abc.jpg",
        "https://soyjak.st/soy/thumb/123abc.jpeg",
        "https://soyjak.st/soy/thumb/123abc.gif"
      ),
      alts
    )
  }

  @Test
  fun `cached png thumb 404 falls back to webp`() {
    val url = "https://soyjak.st/gem/thumb/oldfile.png".toHttpUrl()
    val alts = SoyjakSt.alternateThumbnailUrls(url).map { it.toString() }

    assertEquals(listOf("https://soyjak.st/gem/thumb/oldfile.webp"), alts)
  }

  @Test
  fun `src urls are not rewritten`() {
    val url = "https://soyjak.st/soy/src/123abc.png".toHttpUrl()
    assertEquals(emptyList<okhttp3.HttpUrl>(), SoyjakSt.alternateThumbnailUrls(url))
  }
}
