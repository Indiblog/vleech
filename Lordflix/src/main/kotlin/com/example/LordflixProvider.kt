package com.example

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LordflixProvider : MainAPI() {
    override var mainUrl = DomainConfig.LORDFLIX

    private var domainChecked = false
    private suspend fun ensureDomain() {
        if (domainChecked) return
        domainChecked = true
        withContext(Dispatchers.IO) {
            mainUrl = DomainResolver.resolveBlocking("LORDFLIX", DomainConfig.LORDFLIX)
        }
    }

    override var name              = "LordFlix"
    override val hasMainPage        = true
    override var lang               = "en"
    override val hasDownloadSupport = false
    override val supportedTypes     = setOf(TvType.Movie, TvType.TvSeries)

    companion object {
        private const val TMDB_KEY        = "1711e8bc666246e1eb3a5c95546c8f4a"
        private const val TMDB_API        = "https://api.themoviedb.org/3"
        private const val TMDB_IMG        = "https://image.tmdb.org/t/p/w500"
        private const val TMDB_ORIG       = "https://image.tmdb.org/t/p/original"
        private const val VIDSRC_MOVIE    = "https://vidsrc.cc/v2/embed/movie/"
        private const val VIDSRC_TV       = "https://vidsrc.cc/v2/embed/tv/"
        private const val AUTOEMBED_MOVIE = "https://autoembed.co/movie/tmdb-"
        private const val AUTOEMBED_TV    = "https://autoembed.co/tv/tmdb-"
    }

    override val mainPage = mainPageOf(
        "movie:trending:day" to "Trending Movies",
        "tv:trending:day"    to "Trending Shows",
        "movie:popular"      to "Popular Movies",
        "tv:popular"         to "Popular Shows",
        "movie:top_rated"    to "Top Rated Movies",
        "tv:top_rated"       to "Top Rated Shows",
        "movie:now_playing"  to "Now Playing",
        "tv:on_the_air"      to "On The Air"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        ensureDomain()
        val (mediaType, endpoint) = request.data.split(":", limit = 2).let { it[0] to it[1] }
        val apiUrl = when {
            endpoint.startsWith("trending") ->
                "$TMDB_API/trending/$mediaType/${endpoint.substringAfter(":")}"
            else ->
                "$TMDB_API/$mediaType/$endpoint"
        } + "?api_key=$TMDB_KEY&page=$page"

        val resp  = app.get(apiUrl).parsedSafe<LFTmdbPaged>()
            ?: return newHomePageResponse(request.name, emptyList())
        val items = resp.results.mapNotNull { it.toSearchResponse(mediaType) }
        return newHomePageResponse(request.name, items, hasNext = page < (resp.totalPages ?: 1))
    }

    override suspend fun search(query: String): List<SearchResponse> {
        ensureDomain()
        val url  = "$TMDB_API/search/multi?api_key=$TMDB_KEY&query=${query.encodeUrl()}&include_adult=false"
        val resp = app.get(url).parsedSafe<LFTmdbPaged>() ?: return emptyList()
        return resp.results.mapNotNull {
            val mt = it.mediaType ?: return@mapNotNull null
            if (mt == "person") null else it.toSearchResponse(mt)
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        ensureDomain()
        val parts     = url.split(":")
        if (parts.size < 3) return null
        val mediaType = parts[1]
        val tmdbId    = parts[2]

        val detail = app.get(
            "$TMDB_API/$mediaType/$tmdbId?api_key=$TMDB_KEY&append_to_response=credits,videos,external_ids"
        ).parsedSafe<LFTmdbDetail>() ?: return null

        val title    = detail.title ?: detail.name ?: "Unknown"
        val poster   = detail.posterPath?.let   { TMDB_IMG  + it }
        val backdrop = detail.backdropPath?.let { TMDB_ORIG + it }
        val year     = (detail.releaseDate ?: detail.firstAirDate)?.take(4)?.toIntOrNull()
        val tags     = detail.genres?.map { it.name } ?: emptyList()
        val actors   = detail.credits?.cast?.take(15)?.map {
            ActorData(Actor(it.name, it.profilePath?.let { p -> TMDB_IMG + p }), roleString = it.character)
        } ?: emptyList()
        val trailer  = detail.videos?.results
            ?.firstOrNull { it.type == "Trailer" && it.site == "YouTube" }
            ?.let { "https://www.youtube.com/watch?v=${it.key}" }

        return if (mediaType == "movie") {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl           = poster
                this.backgroundPosterUrl = backdrop
                this.plot                = detail.overview
                this.year                = year
                this.tags                = tags
                this.score               = Score.from10(detail.voteAverage?.toString())
                this.actors              = actors
                safeAddTrailer(trailer)
            }
        } else {
            val episodes = (detail.seasons ?: emptyList())
                .filter { (it.seasonNumber ?: 0) > 0 }
                .flatMap { season ->
                    val sNum = season.seasonNumber ?: return@flatMap emptyList()
                    val seasonData = app.get("$TMDB_API/tv/$tmdbId/season/$sNum?api_key=$TMDB_KEY")
                        .parsedSafe<LFTmdbSeason>()
                    (seasonData?.episodes ?: emptyList()).map { ep ->
                        val epToken = "lf:tv:$tmdbId:s${sNum}e${ep.episodeNumber}"
                        newEpisode(epToken) {
                            this.name        = ep.name
                            this.season      = sNum
                            this.episode     = ep.episodeNumber
                            this.posterUrl   = ep.stillPath?.let { TMDB_IMG + it }
                            this.description = ep.overview
                        }
                    }
                }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl           = poster
                this.backgroundPosterUrl = backdrop
                this.plot                = detail.overview
                this.year                = year
                this.tags                = tags
                this.score               = Score.from10(detail.voteAverage?.toString())
                this.actors              = actors
                safeAddTrailer(trailer)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val parts      = data.split(":")
        if (parts.size < 3) return false
        val mediaType  = parts[1]
        val tmdbId     = parts[2]
        val seasonNum  = if (parts.size > 3) Regex("s(\\d+)").find(parts[3])?.groupValues?.get(1)?.toIntOrNull() else null
        val episodeNum = if (parts.size > 3) Regex("e(\\d+)").find(parts[3])?.groupValues?.get(1)?.toIntOrNull() else null

        val watchUrl = if (mediaType == "movie") "$mainUrl/movie/$tmdbId"
                       else "$mainUrl/series/$tmdbId/$seasonNum/$episodeNum"
        try {
            val doc      = app.get(watchUrl, timeout = 10000L).document
            val nextData = doc.selectFirst("script#__NEXT_DATA__")?.data()
            if (!nextData.isNullOrBlank()) {
                Regex("""https?://[^\s"'\\]+\.(m3u8|mp4)[^\s"'\\]*""").findAll(nextData).forEach { m ->
                    loadExtractor(m.value, mainUrl, subtitleCallback, callback)
                }
            }
            doc.select("iframe[src], iframe[data-src]").forEach { el ->
                val src = el.attr("src").ifBlank { el.attr("data-src") }
                if (src.isNotBlank()) loadExtractor(src, mainUrl, subtitleCallback, callback)
            }
        } catch (_: Exception) { }

        val vidsrcUrl = if (mediaType == "movie") "$VIDSRC_MOVIE$tmdbId"
                        else "$VIDSRC_TV$tmdbId/$seasonNum/$episodeNum"
        loadExtractor(vidsrcUrl, mainUrl, subtitleCallback, callback)

        val autoembedUrl = if (mediaType == "movie") "$AUTOEMBED_MOVIE$tmdbId"
                           else "$AUTOEMBED_TV$tmdbId-$seasonNum-$episodeNum"
        loadExtractor(autoembedUrl, mainUrl, subtitleCallback, callback)

        return true
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private fun LFTmdbResult.toSearchResponse(mediaType: String): SearchResponse? {
        val id    = this.id ?: return null
        val title = this.title ?: this.name ?: return null
        val thumb = this.posterPath?.let { TMDB_IMG + it }
        val token = "lf:$mediaType:$id"
        return if (mediaType == "movie") {
            newMovieSearchResponse(title, token, TvType.Movie) { this.posterUrl = thumb }
        } else {
            newTvSeriesSearchResponse(title, token, TvType.TvSeries) { this.posterUrl = thumb }
        }
    }

    // addTrailer is not available in this pre-release build; trailers skipped
    @Suppress("UNUSED_PARAMETER")
    private fun LoadResponse.safeAddTrailer(url: String?) = Unit

    private fun String.encodeUrl() = java.net.URLEncoder.encode(this, "UTF-8")

    // ─── TMDB data classes ─────────────────────────────────────────────────────

    data class LFTmdbPaged(
        @JsonProperty("results")     val results: List<LFTmdbResult> = emptyList(),
        @JsonProperty("total_pages") val totalPages: Int? = null
    )

    data class LFTmdbResult(
        @JsonProperty("id")          val id: Int?,
        @JsonProperty("title")       val title: String?,
        @JsonProperty("name")        val name: String?,
        @JsonProperty("poster_path") val posterPath: String?,
        @JsonProperty("media_type")  val mediaType: String?
    )

    data class LFTmdbDetail(
        @JsonProperty("id")             val id: Int?,
        @JsonProperty("title")          val title: String?,
        @JsonProperty("name")           val name: String?,
        @JsonProperty("overview")       val overview: String?,
        @JsonProperty("poster_path")    val posterPath: String?,
        @JsonProperty("backdrop_path")  val backdropPath: String?,
        @JsonProperty("release_date")   val releaseDate: String?,
        @JsonProperty("first_air_date") val firstAirDate: String?,
        @JsonProperty("vote_average")   val voteAverage: Double?,
        @JsonProperty("genres")         val genres: List<LFGenre>?,
        @JsonProperty("seasons")        val seasons: List<LFSeasonSummary>?,
        @JsonProperty("credits")        val credits: LFCredits?,
        @JsonProperty("videos")         val videos: LFVideos?,
        @JsonProperty("external_ids")   val externalIds: Map<String, Any?>?
    )

    data class LFGenre(@JsonProperty("name") val name: String)

    data class LFSeasonSummary(
        @JsonProperty("season_number") val seasonNumber: Int?,
        @JsonProperty("episode_count") val episodeCount: Int?
    )

    data class LFTmdbSeason(
        @JsonProperty("episodes") val episodes: List<LFEpisode> = emptyList()
    )

    data class LFEpisode(
        @JsonProperty("name")           val name: String?,
        @JsonProperty("episode_number") val episodeNumber: Int?,
        @JsonProperty("still_path")     val stillPath: String?,
        @JsonProperty("overview")       val overview: String?
    )

    data class LFCredits(@JsonProperty("cast") val cast: List<LFCastMember>?)

    data class LFCastMember(
        @JsonProperty("name")         val name: String,
        @JsonProperty("character")    val character: String?,
        @JsonProperty("profile_path") val profilePath: String?
    )

    data class LFVideos(@JsonProperty("results") val results: List<LFVideo>?)

    data class LFVideo(
        @JsonProperty("key")  val key: String,
        @JsonProperty("type") val type: String,
        @JsonProperty("site") val site: String
    )
}
