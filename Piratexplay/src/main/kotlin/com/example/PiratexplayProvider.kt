// Ported faithfully from:
// https://github.com/phisher98/cloudstream-extensions-phisher/blob/master/Piratexplay/src/main/kotlin/com/piratexplay/Piratexplay.kt
// Changes:
//   - package: com.piratexplay → com.example
//   - mainUrl: hardcoded "https://piratexplay.cc" → DomainConfig.PIRATEXPLAY
//   - class name: Piratexplay → PiratexplayProvider (matches Plugin registration)
//   - All selectors and logic are IDENTICAL.

package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class PiratexplayProvider : MainAPI() {
    // ► Fallback domain; overridden at runtime by DomainResolver.
    override var mainUrl = DomainConfig.PIRATEXPLAY

    private var domainChecked = false
    private suspend fun ensureDomain() {
        if (domainChecked) return
        domainChecked = true
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            mainUrl = DomainResolver.resolveBlocking("PIRATEXPLAY", DomainConfig.PIRATEXPLAY)
        }
    }
    override var name    = "Piratexplay"
    override val hasMainPage        = true
    override var lang               = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes     = setOf(TvType.AnimeMovie, TvType.Anime, TvType.Cartoon)

    override val mainPage = mainPageOf(
        "category/popular"    to "Popular",
        "category/top-airing" to "Top Airing",
        "category/ongoing"    to "OnGoing",
        "category/series"     to "Series",
        "category/movies"     to "Movies",
        "category/anime"      to "Anime",
        "category/cartoon"    to "Cartoon"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        ensureDomain()
        return
        newHomePageResponse(
            request.name,
            listOf(page, page + 1).flatMap { p ->
                app.get("$mainUrl/${request.data}?page=$p").document
                    .select("#movies-a ul li")
                    .mapNotNull { it.toSearchResult() }
            }
        )

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("header h2")?.text()?.trim() ?: return null
        val href      = fixUrl(this.selectFirst("a")?.attr("href").toString())
        val posterUrl = fixUrlNull(this.selectFirst("img")?.getImageAttr())
        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        ensureDomain()
        val document = app.get("$mainUrl/?s=$query&page=$page").document
        return document.select("#movies-a ul li").mapNotNull { it.toSearchResult() }.toNewSearchResponseList()
    }

    override suspend fun load(url: String): LoadResponse {
        ensureDomain()
        val document = app.get(url).document
        val title    = document.selectFirst("div.dfxb h1.entry-title")?.text()
            ?: document.selectFirst("meta[property=og:title]")?.attr("content")?.trim()
            ?: throw NotImplementedError("Unable to find title")
        val poster           = fixUrlNull(document.selectFirst("div.dfxb img")?.attr("src"))
        val backgroundposter = fixUrlNull(document.selectFirst("div.bghd img")?.attr("src"))
        val tags             = document.select("header.entry-header ul li:contains(Genres) p a").map { it.text() }
        val year             = document.select("span.year span").text().trim().toIntOrNull()
        val tvType           = if (url.contains("movie")) TvType.Movie else TvType.TvSeries
        val description      = document.selectFirst("div.description p")?.text()?.trim()
        val recommendations  = document.select("section.section.episodes div.owl-carousel article").mapNotNull { it.toSearchResult() }

        return if (tvType == TvType.TvSeries) {
            val seasonLinks = document.select("div.season-swiper a.season-btn").map { it.attr("href") }
            val episodes    = seasonLinks.flatMap { seasonUrl ->
                val seasonDoc = app.get(mainUrl + seasonUrl).document
                seasonDoc.select("#episode_by_temp li").map { ep ->
                    val headerSpan = ep.selectFirst("header.entry-header span")?.text().orEmpty()
                    val (season, episode) = headerSpan.split("x", limit = 2)
                        .map { it.toIntOrNull() }
                        .let { it.getOrNull(0) to it.getOrNull(1) }
                    newEpisode(ep.selectFirst("a")?.attr("href").orEmpty()) {
                        this.name      = "Episode ${episode?.toString().orEmpty()}"
                        this.season    = season
                        this.episode   = episode
                        this.posterUrl = ep.selectFirst("div.post-thumbnail img")?.getImageAttr()
                    }
                }
            }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.backgroundPosterUrl = backgroundposter
                this.posterUrl           = poster
                this.year                = year
                this.plot                = description
                this.tags                = tags
                this.recommendations     = recommendations
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.backgroundPosterUrl = backgroundposter
                this.posterUrl           = poster
                this.year                = year
                this.plot                = description
                this.tags                = tags
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        document.select("iframe").amap {
            val link = it.attr("src").ifBlank { it.attr("data-src") }.substringAfterLast("url=")
            loadExtractor(link, mainUrl, subtitleCallback, callback)
        }
        return true
    }

    private fun Element.getImageAttr(): String? =
        when {
            this.hasAttr("src")      -> this.attr("src")
            this.hasAttr("data-src") -> this.attr("data-src")
            else                     -> this.attr("src")
        }
}
