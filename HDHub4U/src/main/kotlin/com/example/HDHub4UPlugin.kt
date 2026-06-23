// Ported faithfully from:
// https://github.com/phisher98/cloudstream-extensions-phisher/blob/master/HDhub4u/src/main/kotlin/com/hdhub4u/HDhub4uPlugin.kt
// Changes:
//   - package: com.hdhub4u → com.example
//   - Removed getDomains() / companion object (domain now in DomainConfig.HDHUB4U)
//   - Registered all extractors unchanged

package com.example

import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class HDHub4UPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(HDHub4UProvider())
        registerExtractorAPI(HdStream4u())
        registerExtractorAPI(StreamTape())
        registerExtractorAPI(Hblinks())
        registerExtractorAPI(HDHubCloud())
        registerExtractorAPI(Hubstream())
        registerExtractorAPI(Hubcdnn())
        registerExtractorAPI(Hubdrive())
        registerExtractorAPI(Hubstreamdad())
        registerExtractorAPI(HUBCDN())
        registerExtractorAPI(PixelDrainDev())
    }
}
