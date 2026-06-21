// Ported faithfully from:
// https://github.com/phisher98/cloudstream-extensions-phisher/blob/master/MultiMoviesProvider/src/main/kotlin/com/phisher98/MultiMoviesProviderPlugin.kt
// Changes:
//   - package: com.phisher98 → com.example
//   - Removed getDomains() / companion object (domain now in DomainConfig.MULTIMOVIES)
//   - All extractor registrations are IDENTICAL.

package com.example

import com.lagradost.cloudstream3.extractors.DoodLaExtractor
import com.lagradost.cloudstream3.extractors.GDMirrorbot
import com.lagradost.cloudstream3.extractors.MixDrop
import com.lagradost.cloudstream3.extractors.VidHidePro5
import com.lagradost.cloudstream3.extractors.VidHidePro6
import com.lagradost.cloudstream3.extractors.XStreamCdn
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class MultimoviesPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(MultimoviesProvider())
        registerExtractorAPI(VidHidePro5())
        registerExtractorAPI(MixDrop())
        registerExtractorAPI(MultimoviesCloud())
        registerExtractorAPI(XStreamCdn())
        registerExtractorAPI(DoodLaExtractor())
        registerExtractorAPI(Animezia())
        registerExtractorAPI(MultimoviesServer2())
        registerExtractorAPI(MultimoviesAIO())
        registerExtractorAPI(GDMirrorbot())
        registerExtractorAPI(Asnwish())
        registerExtractorAPI(CdnwishCom())
        registerExtractorAPI(Strwishcom())
        registerExtractorAPI(VidHidePro6())
        registerExtractorAPI(Streamcasthub())
        registerExtractorAPI(Dhcplay())
        registerExtractorAPI(MultimoviesServer1())
        registerExtractorAPI(MMTechinmind())
        registerExtractorAPI(Gofile())
        registerExtractorAPI(MMIqsmartgames())
    }
}
