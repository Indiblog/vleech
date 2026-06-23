// Provider for: https://www.rivestream.app
//
// ARCHITECTURE NOTE — WHY TMDB API IS USED HERE:
//   Rivestream is a React/Next.js single-page application. Its listing and detail pages
//   are rendered entirely client-side; server responses contain only skeleton HTML with
//   zero-width placeholder characters. No content is accessible via plain HTTP + Jsoup.
//
//   Strategy:
//     1. Browse / search  →  TMDB API (same data source the site itself uses)
//     2. Detail pages      →  TMDB API for metadata; URL stored as "tmdb:{type}:{id}"
//     3. Video links       →  Attempt iframe extraction from Rivestream's watch page,
//                             then fall back to vidsrc.cc and autoembed.co embed URLs,
//                             both of which accept raw TMDB IDs and are known to serve
//                             content for aggregator sites of this type.
//
//   If Rivestream's internal embed server is identified in future, replace the embed
//   logic in loadLinks() — the browse/search code above it needs no changes.

package com.example

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor

class RivestreamProvider : MainAPI() {
    // ► Fallback domain; overridden at runtime by DomainResolver.
    override var mainUrl = DomainConfig.RIVESTREAM

    private var domainChecked = false
    private suspend fun ensureDomain() {
        if (domainChecked) return
        domainChecked = true
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            mainUrl = DomainResolver.resolveBlocking("RIVESTREAM", DomainConfig.RIVESTREAM)
        }
    }
    override var name    = "Rivestream"
    override val hasMainPage        = true
    override var lang               = "en"
    override val hasDownloadSupport = false
    override val supportedTypes     = setOf(TvType.Movie, TvType.TvSeries, TvType.Anime)

    companion object {
        private const val TMDB_KEY  = "1865f43a0549ca50d341dd9ab8b29f49"
        private const val TMDB_API  = "https://api.themoviedb.org/3"
        private const val TMDB_IMG  = "https://image.tmdb.org/t/p/w500"
        private const val TMDB_ORIG = "https://image.tmdb.org/t/p/original"

        // vidsrc.cc accepts TMDB IDs directly — widely supported for aggregator sites.
        private const val VIDSRC_MOVIE = "https://vidsrc.cc/v2/embed/movie/"
        private const val VIDSRC_TV    = "https://vidsrc.cc/v2/embed/tv/"
        // autoembed.co is a secondary fallback also keyed on TMDB IDs.
        private const val AUTOEMBED_MOVIE = "https://autoembed.co/movie/tmdb-"
        private const val AUTOEMBED_TV    = "https://autoembed.co/tv/tmdb-"
    }

    // ─── Main page — TMDB trending lists ──────────────────────────────────────

    override val mainPage = mainPageOf(
        "movie:trending:day"         to "Trending Movies Today",
        "tv:trending:day"            to "Trending TV Today",
        "movie:trending:week"        to "Trending Movies This Week",
        "tv:trending:week"           to "Trending TV This Week",
        "movie:now_playing"          to "Now Playing",
        "movie:popular"              to "Popular Movies",
        "tv:popular"                 to "Popular TV Shows",
        "movie:top_rated"            to "Top Rated Movies",
        "tv:top_rated"               to "Top Rated TV"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        ensureDomain()
        val (mediaType, endpoint) = request.data.split(":", limit = 2).let { it[0] to it[1] }
        val url = when {
            endpoint.startsWith("trending") -> "$TMDB_API/trending/$mediaType/${endpoint.substringAfter(":")}"
            else                            -> "$TMDB_API/$mediaType/$endpoint"
        } + "?api_key=$TMDB_KEY&page=$page"

        val resp = app.get(url).parsedSafe<TmdbPagedResult>() ?: return newHomePageResponse(request.name, emptyList())
        val results = resp.results.mapNotNull { it.toSearchResponse(mediaType) }
        return newHomePageResponse(request.name, results, hasNext = page < (resp.totalPages ?: 1))
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    override suspend fun search(query: String): List<SearchResponse> {
        ensureDomain()
        val url  = "$TMDB_API/search/multi?api_key=$TMDB_KEY&query=${query.encodeUrl()}&include_adult=false"
        val resp = app.get(url).parsedSafe<TmdbPagedResult>() ?: return emptyList()
        return resp.results.mapNotNull {
            val mt = it.mediaType ?: return@mapNotNull null
            if (mt == "person") return@mapNotNull null
            it.toSearchResponse(mt)
        }
    }

    // ─── Load detail ──────────────────────────────────────────────────────────

    override suspend fun load(url: String): LoadResponse? {
        ensureDomain()
        // url stored as "tmdb:{type}:{id}" by toSearchResponse()
        val parts = url.split(":")
        if (parts.size < 3) return null
        val mediaType = parts[1]
        val tmdbId    = parts[2]

        val apiUrl    = "$TMDB_API/$mediaType/$tmdbId?api_key=$TMDB_KEY&append_to_response=credits,videos,external_ids"
        val detail    = app.get(apiUrl).parsedSafe<TmdbDetail>() ?: return null

        val title     = detail.title ?: detail.name ?: "Unknown"
        val poster    = detail.posterPath?.let { TMDB_IMG + it }
        val backdrop  = detail.backdropPath?.let { TMDB_ORIG + it }
        val plot      = detail.overview
        val year      = (detail.releaseDate ?: detail.firstAirDate)?.take(4)?.toIntOrNull()
        val tags      = detail.genres?.map { it.name } ?: emptyList()
        val rating    = detail.voteAverage?.toString()
        val actors    = detail.credits?.cast?.take(15)?.map {
            ActorData(Actor(it.name, it.profilePath?.let { p -> TMDB_IMG + p }), roleString = it.character)
        } ?: emptyList()
        val trailer   = detail.videos?.results?.firstOrNull { it.type == "Trailer" && it.site == "YouTube" }
            ?.let { "https://www.youtube.com/watch?v=${it.key}" }

        // Data token: "tmdb:{type}:{id}" — carries all we need for loadLinks
        val dataToken = url

        return if (mediaType == "movie") {
            newMovieLoadResponse(title, dataToken, TvType.Movie, dataToken) {
                this.posterUrl           = poster
                this.backgroundPosterUrl = backdrop
                this.plot                = plot
                this.year                = year
                this.tags                = tags
                this.score               = Score.from10(rating)
                this.actors              = actors
                addTrailerUrl(trailer)
            }
        } else {
            val seasons   = detail.seasons?.filter { (it.seasonNumber ?: 0) > 0 } ?: emptyList()
            val episodes  = seasons.flatMap { season ->
                val sNum     = season.seasonNumber ?: return@flatMap emptyList()
                val seasonDetail = app.get("$TMDB_API/tv/$tmdbId/season/$sNum?api_key=$TMDB_KEY").parsedSafe<TmdbSeason>()
                (seasonDetail?.episodes ?: emptyList()).map { ep ->
                    val epToken = "tmdb:tv:$tmdbId:s${sNum}e${ep.episodeNumber}"
                    newEpisode(epToken) {
                        this.name        = ep.name
                        this.season      = sNum
                        this.episode     = ep.episodeNumber
                        this.posterUrl   = ep.stillPath?.let { TMDB_IMG + it }
                        this.description = ep.overview
                    }
                }
            }
            newTvSeriesLoadResponse(title, dataToken, TvType.TvSeries, episodes) {
                this.posterUrl           = poster
                this.backgroundPosterUrl = backdrop
                this.plot                = plot
                this.year                = year
                this.tags                = tags
                this.score               = Score.from10(rating)
                this.actors              = actors
                addTrailerUrl(trailer)
            }
        }
    }

    // ─── Load links ───────────────────────────────────────────────────────────

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val parts = data.split(":")
        if (parts.size < 3) return false
        val mediaType = parts[1]
        val tmdbId    = parts[2]

        // Episode token format: "tmdb:tv:{id}:s{n}e{n}"
        val seasonNum  = if (parts.size > 3) Regex("s(\\d+)").find(parts[3])?.groupValues?.get(1)?.toIntOrNull() else null
        val episodeNum = if (parts.size > 3) Regex("e(\\d+)").find(parts[3])?.groupValues?.get(1)?.toIntOrNull() else null

        // Step 1 — attempt to extract iframes from Rivestream's own watch page.
        // The watch page is at: /watch/movie/{tmdb_id} or /watch/tv/{tmdb_id}/{s}/{e}
        // Even though the page is a SPA, some Next.js sites embed data in __NEXT_DATA__.
        val watchUrl = if (mediaType == "movie") {
            "$mainUrl/watch/movie/$tmdbId"
        } else {
            "$mainUrl/watch/tv/$tmdbId/$seasonNum/$episodeNum"
        }
        try {
            val doc = app.get(watchUrl, timeout = 10000L).document
            // Check for __NEXT_DATA__ JSON blob (Next.js SSR)
            val nextData = doc.selectFirst("script#__NEXT_DATA__")?.data()
            if (!nextData.isNullOrBlank()) {
                // Try to extract any embed URL from the JSON
                Regex("""https?://[^\s"'\\]+\.(m3u8|mp4)[^\s"'\\]*""").findAll(nextData).forEach { m ->
                    loadExtractor(m.value, mainUrl, subtitleCallback, callback)
                }
            }
            // Also try iframes
            doc.select("iframe[src]").forEach { iframe ->
                val src = iframe.attr("src").ifBlank { iframe.attr("data-src") }
                if (src.isNotBlank()) loadExtractor(src, mainUrl, subtitleCallback, callback)
            }
        } catch (_: Exception) { }

        // Step 2 — vidsrc.cc embed (primary fallback; accepts raw TMDB IDs)
        val vidsrcUrl = if (mediaType == "movie") {
            "$VIDSRC_MOVIE$tmdbId"
        } else {
            "$VIDSRC_TV$tmdbId/$seasonNum/$episodeNum"
        }
        loadExtractor(vidsrcUrl, mainUrl, subtitleCallback, callback)

        // Step 3 — autoembed.co (secondary fallback)
        val autoembedUrl = if (mediaType == "movie") {
            "$AUTOEMBED_MOVIE$tmdbId"
        } else {
            "$AUTOEMBED_TV$tmdbId-$seasonNum-$episodeNum"
        }
        loadExtractor(autoembedUrl, mainUrl, subtitleCallback, callback)

        return true
    }

    // ─── Helper — build SearchResponse from a TMDB result object ──────────────

    private fun TmdbResult.toSearchResponse(mediaType: String): SearchResponse? {
        val id    = this.id ?: return null
        val title = this.title ?: this.name ?: return null
        val thumb = this.posterPath?.let { TMDB_IMG + it }
        // Store token instead of a real URL — parsed in load() and loadLinks()
        val token = "tmdb:$mediaType:$id"
        return if (mediaType == "movie") {
            newMovieSearchResponse(title, token, TvType.Movie) { this.posterUrl = thumb }
        } else {
            newTvSeriesSearchResponse(title, token, TvType.TvSeries) { this.posterUrl = thumb }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun LoadResponse.addTrailerUrl(url: String?) = Unit

    private fun String.encodeUrl() = java.net.URLEncoder.encode(this, "UTF-8")

    // ─── TMDB data classes ────────────────────────────────────────────────────

    data class TmdbPagedResult(
        @JsonProperty("results")      val results: List<TmdbResult> = emptyList(),
        @JsonProperty("total_pages")  val totalPages: Int? = null
    )

    data class TmdbResult(
        @JsonProperty("id")           val id: Int?,
        @JsonProperty("title")        val title: String?,
        @JsonProperty("name")         val name: String?,
        @JsonProperty("poster_path")  val posterPath: String?,
        @JsonProperty("media_type")   val mediaType: String?
    )

    data class TmdbDetail(
        @JsonProperty("id")                 val id: Int?,
        @JsonProperty("title")              val title: String?,
        @JsonProperty("name")               val name: String?,
        @JsonProperty("overview")           val overview: String?,
        @JsonProperty("poster_path")        val posterPath: String?,
        @JsonProperty("backdrop_path")      val backdropPath: String?,
        @JsonProperty("release_date")       val releaseDate: String?,
        @JsonProperty("first_air_date")     val firstAirDate: String?,
        @JsonProperty("vote_average")       val voteAverage: Double?,
        @JsonProperty("genres")             val genres: List<TmdbGenre>?,
        @JsonProperty("seasons")            val seasons: List<TmdbSeasonSummary>?,
        @JsonProperty("credits")            val credits: TmdbCredits?,
        @JsonProperty("videos")             val videos: TmdbVideos?,
        @JsonProperty("external_ids")       val externalIds: TmdbExternalIds?
    )

    data class TmdbGenre(@JsonProperty("name") val name: String)

    data class TmdbSeasonSummary(
        @JsonProperty("season_number") val seasonNumber: Int?,
        @JsonProperty("episode_count") val episodeCount: Int?
    )

    data class TmdbSeason(
        @JsonProperty("episodes") val episodes: List<TmdbEpisode> = emptyList()
    )

    data class TmdbEpisode(
        @JsonProperty("name")           val name: String?,
        @JsonProperty("episode_number") val episodeNumber: Int?,
        @JsonProperty("still_path")     val stillPath: String?,
        @JsonProperty("overview")       val overview: String?
    )

    data class TmdbCredits(
        @JsonProperty("cast") val cast: List<TmdbCastMember>?
    )

    data class TmdbCastMember(
        @JsonProperty("name")         val name: String,
        @JsonProperty("character")    val character: String?,
        @JsonProperty("profile_path") val profilePath: String?
    )

    data class TmdbVideos(
        @JsonProperty("results") val results: List<TmdbVideo>?
    )

    data class TmdbVideo(
        @JsonProperty("key")  val key: String,
        @JsonProperty("type") val type: String,
        @JsonProperty("site") val site: String
    )

    data class TmdbExternalIds(
        @JsonProperty("imdb_id") val imdbId: String?
    )
}
