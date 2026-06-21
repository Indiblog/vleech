// Ported faithfully from:
// https://github.com/phisher98/cloudstream-extensions-phisher/blob/master/HDhub4u/src/main/kotlin/com/hdhub4u/Utils.kt
// Changes: package com.hdhub4u → com.example. All logic identical.

package com.example

import android.annotation.SuppressLint
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.lagradost.api.Log
import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.jsoup.nodes.Document

// Extension properties used by HDHub4UProvider.toSearchResult()
val Document.postTitle: String     get() = selectFirst("title")?.text() ?: ""
val Document.permalink: String     get() = selectFirst("link[rel=canonical]")?.attr("href") ?: ""
val Document.postThumbnail: String get() = selectFirst("meta[property=og:image]")?.attr("content") ?: ""

suspend fun getRedirectLinks(url: String): String? {
    val doc    = app.get(url).toString()
    val regex  = "s\\('o','([A-Za-z0-9+/=]+)'|ck\\('_wp_http_\\d+','([^']+)'".toRegex()
    val combined = buildString {
        regex.findAll(doc).forEach { m ->
            val v = m.groups[1]?.value ?: m.groups[2]?.value
            if (!v.isNullOrEmpty()) append(v)
        }
    }
    return try {
        val decoded  = base64Decode(pen(base64Decode(base64Decode(combined))))
        val json     = JSONObject(decoded)
        val encoded  = base64Decode(json.optString("o", "")).trim()
        val data     = encode(json.optString("data", "")).trim()
        val blogUrl  = json.optString("blog_url", "").trim()
        val direct   = runCatching {
            app.get("$blogUrl?re=$data".trim()).document.select("body").text().trim()
        }.getOrDefault("").trim()
        encoded.ifEmpty { direct }
    } catch (e: Exception) {
        Log.e("Error:", "Error processing links $e")
        url
    }
}

@SuppressLint("NewApi")
fun encode(value: String): String =
    String(android.util.Base64.decode(value, android.util.Base64.DEFAULT))

fun pen(value: String): String =
    value.map {
        when (it) {
            in 'A'..'Z' -> ((it - 'A' + 13) % 26 + 'A'.code).toChar()
            in 'a'..'z' -> ((it - 'a' + 13) % 26 + 'a'.code).toChar()
            else -> it
        }
    }.joinToString("")

suspend fun loadSourceNameExtractor(
    source: String, url: String, referer: String? = null, quality: Int? = null,
    subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit
) {
    loadExtractor(url, referer, subtitleCallback) { link ->
        CoroutineScope(Dispatchers.IO).launch {
            callback(newExtractorLink("${link.source} $source", "${link.source} $source", link.url) {
                this.quality       = quality ?: link.quality
                this.type          = link.type
                this.referer       = link.referer
                this.headers       = link.headers
                this.extractorData = link.extractorData
            })
        }
    }
}

data class IMDB(
    @SerializedName("imdb_id") val imdbId: String? = null
)

fun cleanTitle(raw: String): String {
    val name   = raw.substringBefore("(").trim()
        .replace(Regex("""\s+"""), " ")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    val season = Regex("""Season\s*\d+""", RegexOption.IGNORE_CASE).find(raw)?.value?.replaceFirstChar { it.uppercase() }
    val year   = Regex("""\b(19|20)\d{2}\b""").find(raw)?.value
    val parts  = mutableListOf<String>()
    if (season != null) parts += season
    if (year   != null) parts += year
    return if (parts.isEmpty()) name else name + parts.joinToString("") { " ($it)" }
}

data class ResponseDataLocal(val meta: MetaLocal?)

data class MetaLocal(
    val name: String?        = null,
    val description: String? = null,
    val actorsData: List<ActorData>? = null,
    val year: String?        = null,
    val background: String?  = null,
    val genres: List<String>? = null,
    val videos: List<VideoLocal>? = null,
    val rating: Score?,
    val logo: String?
)

data class VideoLocal(
    val title: String?     = null,
    val season: Int?       = null,
    val episode: Int?      = null,
    val overview: String?  = null,
    val thumbnail: String? = null,
    val released: String?  = null,
    val rating: Score?
)

data class Search(
    @param:JsonProperty("facet_counts") val facetCounts: List<Any?>,
    val found: Long,
    val hits: List<Hit>,
    @param:JsonProperty("out_of") val outOf: Long,
    val page: Long,
    @param:JsonProperty("request_params") val requestParams: RequestParams,
    @param:JsonProperty("search_cutoff") val searchCutoff: Boolean,
    @param:JsonProperty("search_time_ms") val searchTimeMs: Long
)

data class Hit(
    val document: com.example.Document,
    val highlight: Map<String, Any>,
    val highlights: List<Any?>,
    @param:JsonProperty("text_match") val textMatch: Long,
    @param:JsonProperty("text_match_info") val textMatchInfo: TextMatchInfo
)

data class Document(
    val category: List<String>,
    val id: String,
    val permalink: String,
    @param:JsonProperty("post_date") val postDate: String,
    @param:JsonProperty("post_thumbnail") val postThumbnail: String,
    @param:JsonProperty("post_title") val postTitle: String,
    @param:JsonProperty("post_type") val postType: String,
    @param:JsonProperty("sort_by_date") val sortByDate: Long
)

data class TextMatchInfo(
    @param:JsonProperty("best_field_score")  val bestFieldScore: String,
    @param:JsonProperty("best_field_weight") val bestFieldWeight: Long,
    @param:JsonProperty("fields_matched")    val fieldsMatched: Long,
    @param:JsonProperty("num_tokens_dropped") val numTokensDropped: Long,
    val score: String,
    @param:JsonProperty("tokens_matched")    val tokensMatched: Long,
    @param:JsonProperty("typo_prefix_score") val typoPrefixScore: Long
)

data class RequestParams(
    @param:JsonProperty("collection_name") val collectionName: String,
    @param:JsonProperty("first_q") val firstQ: String,
    @param:JsonProperty("per_page") val perPage: Long,
    val q: String
)
