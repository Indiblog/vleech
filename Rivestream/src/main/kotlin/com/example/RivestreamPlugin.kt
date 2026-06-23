package com.example

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class RivestreamPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(RivestreamProvider())
        registerExtractorAPI(VidSrcTo())
        registerExtractorAPI(VidSrcXyz())
        registerExtractorAPI(MultiEmbed())
    }
}
