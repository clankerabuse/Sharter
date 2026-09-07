package com.github.k1rakishou.chan.core.site.sites.vichan.soyjakst

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
}
