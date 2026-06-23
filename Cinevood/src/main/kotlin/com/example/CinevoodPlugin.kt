// Ported faithfully from:
// https://github.com/rockhero1234/cinephile/blob/master/Cinevood/src/main/kotlin/com/cinevood/CinevoodPlugin.kt
// Changes: package → com.example; HubCloud import → com.example

package com.example

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class CinevoodPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(CinevoodProvider())
        registerExtractorAPI(HubCloud())
    }
}
