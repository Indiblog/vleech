// Ported faithfully from:
// https://github.com/phisher98/cloudstream-extensions-phisher/blob/master/Piratexplay/src/main/kotlin/com/piratexplay/Extractor.kt
// Changes: package com.piratexplay → com.example. All logic is identical.

package com.example

import com.google.gson.JsonParser
import com.lagradost.api.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.extractors.VidHidePro
import com.lagradost.cloudstream3.extractors.VidStack
import com.lagradost.cloudstream3.newSubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.JsUnpacker
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URI

class Pixdrive : Filesim() {
    override var mainUrl = "https://pixdrive.cfd"
}

class Ghbrisk : Filesim() {
    override val name            = "Streamwish"
    override val mainUrl         = "https://ghbrisk.com"
    override val requiresReferer = true
}

class Techinmind : GDMirrorbotPX() {
    override var name            = "Techinmind"
    override var mainUrl         = "https://dlx.techinmind.space"
    override val requiresReferer = true
}

// Renamed from GDMirrorbot to GDMirrorbotPX to avoid package clash with MultiMovies' GDMirrorbot.
open class GDMirrorbotPX : ExtractorApi() {
    override var name            = "GDMirrorbot"
    override var mainUrl         = "https://gdmirrorbot.nl"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val (sids, host) = extractSidsAndHost(url) ?: return
        if (host.isNullOrBlank()) { Log.e("Error:", "Host is null"); return }

        sids.forEach { sid ->
            try {
                val responseText = app.post("$host/embedhelper.php", data = mapOf("sid" to sid),
                    headers = mapOf("Referer" to host, "X-Requested-With" to "XMLHttpRequest")).text
                val root         = JsonParser.parseString(responseText).takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
                val siteUrls     = root["siteUrls"]?.asJsonObject ?: return@forEach
                if (!siteUrls.has("gofs")) siteUrls.addProperty("GoFile", "https://gofile.io/d/")
                if (!siteUrls.has("buzzheavier")) siteUrls.addProperty("buzzheavier", "https://buzzheavier.com/")
                val siteFriendly = root["siteFriendlyNames"]?.asJsonObject
                val decodedMresult = when {
                    root["mresult"]?.isJsonObject == true -> root["mresult"].asJsonObject
                    root["mresult"]?.isJsonPrimitive == true -> {
                        try { base64Decode(root["mresult"].asString).let { JsonParser.parseString(it).asJsonObject } }
                        catch (e: Exception) { Log.e("Phisher", "Decode failed: $e"); return@forEach }
                    }
                    else -> return@forEach
                }
                siteUrls.keySet().intersect(decodedMresult.keySet()).forEach { key ->
                    val base   = siteUrls[key]?.asString?.trimEnd('/')  ?: return@forEach
                    val path   = decodedMresult[key]?.asString?.trimStart('/') ?: return@forEach
                    val fullUrl = "$base/$path"
                    val friendly = siteFriendly?.get(key)?.asString ?: key
                    try {
                        when (friendly) {
                            "StreamHG", "EarnVids" -> VidHidePro().getUrl(fullUrl, referer, subtitleCallback, callback)
                            "RpmShare", "UpnShare", "StreamP2p" -> VidStack().getUrl(fullUrl, referer, subtitleCallback, callback)
                            else -> loadExtractor(fullUrl, referer ?: mainUrl, subtitleCallback, callback)
                        }
                    } catch (e: Exception) { Log.e("Error:", "Extractor failed: $friendly $e") }
                }
            } catch (e: Exception) { Log.e("Error:", "SID failed: $sid $e") }
        }
    }

    private suspend fun extractSidsAndHost(url: String): Pair<List<String>, String?>? {
        return if (!url.contains("key=")) {
            val sid  = url.substringAfterLast("embed/")
            val host = getBaseUrl(app.get(url).url)
            Pair(listOf(sid), host)
        } else {
            var pageText = app.get(url).text
            val finalId  = Regex("""FinalID\s*=\s*"([^"]+)"""").find(pageText)?.groupValues?.get(1)
            val myKey    = Regex("""myKey\s*=\s*"([^"]+)"""").find(pageText)?.groupValues?.get(1)
            val idType   = Regex("""idType\s*=\s*"([^"]+)"""").find(pageText)?.groupValues?.get(1) ?: "imdbid"
            val baseUrl  = Regex("""let\s+baseUrl\s*=\s*"([^"]+)"""").find(pageText)?.groupValues?.get(1)?.takeIf { it.startsWith("http") }
                ?: Regex("""player_base\s*=\s*["']([^"']+)["']""").find(pageText)?.groupValues?.get(1)
            val host = baseUrl?.let { getBaseUrl(it) }
            if (finalId != null && myKey != null) {
                val apiUrl = if (url.contains("/tv/")) {
                    val season  = Regex("""/tv/\d+/(\d+)/""").find(url)?.groupValues?.get(1) ?: "1"
                    val episode = Regex("""/tv/\d+/\d+/(\d+)""").find(url)?.groupValues?.get(1) ?: "1"
                    "$mainUrl/myseriesapi?tmdbid=$finalId&season=$season&epname=$episode&key=$myKey"
                } else "$mainUrl/mymovieapi?$idType=$finalId&key=$myKey"
                pageText = app.get(apiUrl, referer = apiUrl).text
            }
            val json      = JsonParser.parseString(pageText).takeIf { it.isJsonObject }?.asJsonObject ?: return null
            val dataArray = json["data"]?.asJsonArray
            val sids      = dataArray?.mapNotNull { it.asJsonObject["fileslug"]?.asString }?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
                ?: listOf(url.substringAfterLast("/"))
            Pair(sids, host)
        }
    }

    private fun getBaseUrl(url: String): String = URI(url).let { "${it.scheme}://${it.host}" }
}

open class AWSStream : ExtractorApi() {
    override val name            = "AWSStream"
    override val mainUrl         = "https://z.awstream.net"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val hash    = url.substringAfterLast("/")
        val doc     = app.get(url).document
        val m3u8Url = "$mainUrl/player/index.php?data=$hash&do=getVideo"
        val header  = mapOf("x-requested-with" to "XMLHttpRequest")
        val form    = mapOf("hash" to hash, "r" to mainUrl)
        val response = app.post(m3u8Url, headers = header, data = form).parsedSafe<AWSResponse>()
        response?.videoSource?.let { m3u8 ->
            callback(newExtractorLink(name, name, m3u8, ExtractorLinkType.M3U8) { this.referer = ""; this.quality = Qualities.P1080.value })
            val pack = doc.selectFirst("script:containsData(function(p,a,c,k,e,d))")?.data().orEmpty()
            JsUnpacker(pack).unpack()?.let { unpacked ->
                Regex(""""kind":\s*"captions"\s*,\s*"file":\s*"(https.*?\.srt)"""").find(unpacked)?.groupValues?.get(1)?.let {
                    subtitleCallback(newSubtitleFile("English", it))
                }
            }
        }
    }

    data class AWSResponse(val hls: Boolean, val videoImage: String, val videoSource: String, val securedLink: String, val downloadLinks: List<Any?>, val attachmentLinks: List<Any?>, val ck: String)
}

class ascdn21 : AWSStream() {
    override val name    = "Zephyrflick"
    override val mainUrl = "https://as-cdn21.top"
    override val requiresReferer = true
}

class MyAnimeworld : ExtractorApi() {
    override val name            = "MyAnimeworld"
    override val mainUrl         = "https://myanimeworld.in"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val iframe = app.get(url).document.select("iframe").attr("src")
        loadExtractor(iframe, "", subtitleCallback, callback)
    }
}

class Iqsmartgamesstreams : GDMirrorbotPX() {
    override var name    = "Iqsmartgames"
    override var mainUrl = "https://streams.iqsmartgames.com"
    override var requiresReferer = true
}

class Iqsmartgamespro : GDMirrorbotPX() {
    override var name    = "Iqsmartgames"
    override var mainUrl = "https://pro.iqsmartgames.com"
    override var requiresReferer = true
}

class PiratexplayExtractor : ExtractorApi() {
    override val name            = "PiratexplayExtractor"
    // ► mainUrl sourced from DomainConfig via the constant — do NOT hardcode here.
    override val mainUrl         = DomainConfig.PIRATEXPLAY
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val iframe = app.get(url).document.select("#playerFrame").attr("src")
        loadExtractor(iframe, "", subtitleCallback, callback)
    }
}

class Cloudy : VidStack() {
    override var mainUrl = "https://cloudy.upns.one"
}
