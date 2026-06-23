package com.example

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink

// Handles: https://vidsrc.to/embed/movie/{id}  and  /embed/tv/{id}/{s}/{e}
class VidSrcTo : ExtractorApi() {
    override val name            = "VidSrc.to"
    override val mainUrl         = "https://vidsrc.to"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val doc = app.get(url, referer = referer ?: mainUrl).document
        // vidsrc.to embeds iframes pointing to actual stream providers
        doc.select("iframe[src]").forEach { iframe ->
            val src = iframe.attr("src").let { if (it.startsWith("//")) "https:$it" else it }
            if (src.isNotBlank()) {
                loadExtractor(src, url, subtitleCallback, callback)
            }
        }
        // Also check for direct source tags
        doc.select("source[src]").forEach { source ->
            val src = source.attr("src")
            if (src.isNotBlank() && (src.contains(".m3u8") || src.contains(".mp4"))) {
                callback(
                    newExtractorLink(name, name, src) {
                        this.referer  = url
                        this.quality  = Qualities.Unknown.value
                    }
                )
            }
        }
    }
}

// Handles: https://vidsrc.xyz/embed/movie?tmdb={id}  and  /embed/tv?tmdb={id}&season={s}&episode={e}
class VidSrcXyz : ExtractorApi() {
    override val name            = "VidSrc.xyz"
    override val mainUrl         = "https://vidsrc.xyz"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val doc = app.get(url, referer = referer ?: mainUrl).document
        doc.select("iframe[src]").forEach { iframe ->
            val src = iframe.attr("src").let { if (it.startsWith("//")) "https:$it" else it }
            if (src.isNotBlank()) {
                loadExtractor(src, url, subtitleCallback, callback)
            }
        }
    }
}

// Handles: https://multiembed.mov/?video_id={tmdb_id}&tmdb=1
class MultiEmbed : ExtractorApi() {
    override val name            = "MultiEmbed"
    override val mainUrl         = "https://multiembed.mov"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val doc = app.get(url, referer = referer ?: mainUrl).document
        doc.select("iframe[src]").forEach { iframe ->
            val src = iframe.attr("src").let { if (it.startsWith("//")) "https:$it" else it }
            if (src.isNotBlank()) {
                loadExtractor(src, url, subtitleCallback, callback)
            }
        }
    }
}
