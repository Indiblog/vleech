// Ported faithfully from:
// https://github.com/phisher98/cloudstream-extensions-phisher/blob/master/Piratexplay/src/main/kotlin/com/piratexplay/PiratexplayProvider.kt
// Changes: package com.piratexplay → com.example; class name aligned to Plugin suffix.

package com.example

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class PiratexplayPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(PiratexplayProvider())
        registerExtractorAPI(Techinmind())
        registerExtractorAPI(Pixdrive())
        registerExtractorAPI(Ghbrisk())
        registerExtractorAPI(AWSStream())
        registerExtractorAPI(MyAnimeworld())
        registerExtractorAPI(ascdn21())
        registerExtractorAPI(PiratexplayExtractor())
        registerExtractorAPI(Iqsmartgamesstreams())
        registerExtractorAPI(Iqsmartgamespro())
        registerExtractorAPI(Cloudy())
    }
}
