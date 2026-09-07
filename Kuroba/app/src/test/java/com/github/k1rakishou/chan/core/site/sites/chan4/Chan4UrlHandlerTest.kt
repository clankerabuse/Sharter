package com.github.k1rakishou.chan.core.site.sites.chan4

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Chan4UrlHandlerTest {
  private val handler = Chan4UrlHandler()

  @Test
  fun `responds to 4chan and 4channel hosts`() {
    assertTrue(handler.respondsTo("https://4chan.org/g/".toHttpUrl()))
    assertTrue(handler.respondsTo("https://www.4chan.org/g/".toHttpUrl()))
    assertTrue(handler.respondsTo("https://boards.4chan.org/g/thread/1".toHttpUrl()))
    assertTrue(handler.respondsTo("https://4channel.org/g/".toHttpUrl()))
    assertTrue(handler.respondsTo("https://boards.4channel.org/g/".toHttpUrl()))
  }

  @Test
  fun `does not respond to other imageboards`() {
    assertFalse(handler.respondsTo("https://soyjak.st/soy/".toHttpUrl()))
  }
}
