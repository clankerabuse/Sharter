package com.github.k1rakishou.chan.core.site

import com.github.k1rakishou.chan.core.site.sites.chan4.Chan4
import com.github.k1rakishou.chan.core.site.sites.vichan.soyjakst.SoyjakSt
import com.github.k1rakishou.model.data.descriptor.SiteDescriptor
import org.junit.Assert.assertEquals
import org.junit.Test

class SiteRegistryTest {

  @Test
  fun `registers soyjak st and 4chan`() {
    val siteClasses = SiteRegistry.SITE_CLASSES_MAP

    assertEquals(SoyjakSt::class.java, siteClasses[SiteDescriptor.create(SoyjakSt.SITE_NAME)])
    assertEquals(Chan4::class.java, siteClasses[Chan4.SITE_DESCRIPTOR])
  }
}
