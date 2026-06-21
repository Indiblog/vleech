// Ported faithfully from:
// https://github.com/phisher98/cloudstream-extensions-phisher/blob/master/MultiMoviesProvider/src/main/kotlin/com/phisher98/Extractor.kt
// Changes:
//   - package: com.phisher98 → com.example
//   - Class names suffixed/prefixed where needed to avoid clashes with Piratexplay extractors in the same package:
//       Techinmind → MMTechinmind
//       Iqsmartgames → MMIqsmartgames
//       server1 → MultimoviesServer1
//       server2 → MultimoviesServer2
//   - GDMirrorbot is imported from cloudstream3 (built-in); not redeclared here.
//   - All extraction logic is IDENTICAL to the original.

package com.example

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.JsonParser
import com.lagradost.api.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.extractors.StreamWishExtractor
import com.lagradost.cloudstream3.extractors.VidHidePro
import com.lagradost.cloudstream3.extractors.VidStack
import com.lagradost.cloudstream3.extractors.VidhideExtractor
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URI
import java.security.MessageDigest

class MultimoviesAIO : StreamWishExtractor() {
    override var name            = "Multimovies Cloud AIO"
    override var mainUrl         = "https://allinonedownloader.fun"
    override var requiresReferer = true
}

class MMTechinmind : ExtractorApi() {
    override var name            = "Techinmind Cloud AIO"
    override var mainUrl         = "https://stream.techinmind.space"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        loadExtractor(url, referer ?: mainUrl, subtitleCallback, callback)
    }
}

class MMIqsmartgames : ExtractorApi() {
    override var name            = "Iqsmartgames"
    override var mainUrl         = "https://streams.iqsmartgames.com"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        loadExtractor(url, referer ?: mainUrl, subtitleCallback, callback)
    }
}

class Dhcplay : VidHidePro() {
    override var name            = "DHC Play"
    override var mainUrl         = "https://dhcplay.com"
    override var requiresReferer = true
}

class MultimoviesCloud : StreamWishExtractor() {
    override var name            = "Multimovies Cloud"
    override var mainUrl         = "https://multimovies.cloud"
    override var requiresReferer = true
}

class Animezia : VidhideExtractor() {
    override var name            = "Animezia"
    override var mainUrl         = "https://animezia.cloud"
    override var requiresReferer = true
}

class MultimoviesServer1 : VidStack() {
    override var name            = "MultimoviesVidstack"
    override var mainUrl         = "https://server1.uns.bio"
    override var requiresReferer = true
}

class MultimoviesServer2 : VidhideExtractor() {
    override var name            = "Multimovies Vidhide"
    override var mainUrl         = "https://server2.shop"
    override var requiresReferer = true
}

class Asnwish : StreamWishExtractor() {
    override val name            = "Streanwish Asn"
    override val mainUrl         = "https://asnwish.com"
    override val requiresReferer = true
}

class CdnwishCom : StreamWishExtractor() {
    override val name            = "Cdnwish"
    override val mainUrl         = "https://cdnwish.com"
    override val requiresReferer = true
}

class Strwishcom : StreamWishExtractor() {
    override val name            = "Strwish"
    override val mainUrl         = "https://strwish.com"
    override val requiresReferer = true
}

class Streamcasthub : StreamWishExtractor() {
    override val name            = "Streamcasthub"
    override val mainUrl         = "https://streamcasthub.com"
    override val requiresReferer = true
}

class Gofile : ExtractorApi() {
    override val name            = "Gofile"
    override val mainUrl         = "https://gofile.io"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val fileId   = url.substringAfterLast("/")
        val token    = getToken()
        val apiUrl   = "https://api.gofile.io/contents/$fileId?wt=4fd6sg89d7s6&cache=300&password=&page=1&foldersFirst=false"
        val headers  = mapOf("Authorization" to "Bearer $token", "User-Agent" to USER_AGENT)
        val response = app.get(apiUrl, headers = headers).parsedSafe<GofileResponse>() ?: return
        response.data?.children?.values?.forEach { child ->
            val link = child.link ?: return@forEach
            callback(newExtractorLink(name, "${child.name ?: name} [Gofile]", link) { this.quality = Qualities.Unknown.value })
        }
    }

    private suspend fun getToken(): String {
        val resp = app.post("https://api.gofile.io/accounts", headers = mapOf("User-Agent" to USER_AGENT)).parsedSafe<GofileAccountResponse>()
        return resp?.data?.token ?: ""
    }

    data class GofileAccountResponse(
        @param:JsonProperty("data") val data: GofileAccountData?
    )
    data class GofileAccountData(
        @param:JsonProperty("token") val token: String?
    )
    data class GofileResponse(
        @param:JsonProperty("data") val data: GofileData?
    )
    data class GofileData(
        @param:JsonProperty("children") val children: Map<String, GofileChild>?
    )
    data class GofileChild(
        @param:JsonProperty("name") val name: String?,
        @param:JsonProperty("link") val link: String?
    )
}
