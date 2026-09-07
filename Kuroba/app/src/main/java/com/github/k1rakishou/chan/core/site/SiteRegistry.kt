package com.github.k1rakishou.chan.core.site

import com.github.k1rakishou.chan.core.site.sites.CompositeCatalogSite
import com.github.k1rakishou.chan.core.site.sites.chan4.Chan4
import com.github.k1rakishou.chan.core.site.sites.vichan.soyjakst.SoyjakSt
import com.github.k1rakishou.model.data.descriptor.SiteDescriptor

/**
 * Registered imageboards. Sharter includes soyjak.st and 4chan; other Kuroba sites stay unregistered.
 */
object SiteRegistry {

  val SITE_CLASSES_MAP: Map<SiteDescriptor, Class<out Site>> by lazy {
    val siteClasses = mutableMapOf<SiteDescriptor, Class<out Site>>()

    siteClasses.addSiteToSiteClassesMap(SoyjakSt.SITE_NAME, SoyjakSt::class.java)
    siteClasses.addSiteToSiteClassesMap(Chan4.SITE_NAME, Chan4::class.java)

    // Synthetic site used for composed-catalog ordering on the board picker.
    siteClasses.addSiteToSiteClassesMap(CompositeCatalogSite.SITE_NAME, CompositeCatalogSite::class.java)

    return@lazy siteClasses
  }

  private fun MutableMap<SiteDescriptor, Class<out Site>>.addSiteToSiteClassesMap(
    siteName: String,
    siteClass: Class<out Site>
  ) {
    val siteDescriptor = SiteDescriptor.create(siteName)

    require(!this.contains(siteDescriptor)) {
      "Site $siteName already added! Make sure that no sites share the same name!"
    }

    this[siteDescriptor] = siteClass
  }
}
