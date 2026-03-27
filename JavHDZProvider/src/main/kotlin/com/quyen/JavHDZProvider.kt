package com.quyen

import android.util.Base64
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import kotlinx.serialization.Serializable
import org.jsoup.nodes.Element

class JavHDZProvider : MainAPI() {
    override var mainUrl = "https://javhdz.hot"
    override var name = "JavHDZ"
    override val hasMainPage = true
    override var lang = "vi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie)

    // ───────────────────────────────────────────────────────────
    // Main page categories with pagination support
    // CloudStream calls getMainPage(page=1,2,3,...) and appends
    // the page number directly onto request.data
    // ───────────────────────────────────────────────────────────
    override val mainPage = mainPageOf(
        "$mainUrl/video/page/"              to "Mới nhất",
        "$mainUrl/trending/page/"           to "Trending",
        "$mainUrl/category/censored-2/page/"   to "Censored",
        "$mainUrl/category/uncensored-3/page/" to "Uncensored",
        "$mainUrl/category/beauty-4/page/"     to "Beauty & More",
    )

    // ───────────────────────────────────────────────────────────
    // Parse one movie card element → SearchResponse
    // HTML pattern:
    //   <a class="movie-item m-block" href="/slug-id.html" title="...">
    //     <div class="m-block-img"><img src="..."/></div>
    //     <div class="m-block-info"><h3 class="m-block-title">Title</h3></div>
    //   </a>
    // ───────────────────────────────────────────────────────────
    private fun Element.toSearchResult(): MovieSearchResponse? {
        val title = this.selectFirst("h3.m-block-title")?.text()?.trim()
            ?: this.attr("title").trim().takeIf { it.isNotEmpty() }
            ?: return null
        val href = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("src")
                ?: this.selectFirst("img")?.attr("data-src")
        )
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    // ───────────────────────────────────────────────────────────
    // Home page
    // URL pattern: /video/page/1/, /trending/page/2/, etc.
    // ───────────────────────────────────────────────────────────
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = "${request.data}$page/"
        val document = app.get(url).document
        val items = document.select("a.movie-item.m-block").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, items, hasNext = items.isNotEmpty())
    }

    // ───────────────────────────────────────────────────────────
    // Search
    // URL pattern: /search/{query}/
    // ───────────────────────────────────────────────────────────
    override suspend fun search(query: String): List<SearchResponse> {
        val encodedQuery = query.trim().replace(" ", "+")
        val document = app.get("$mainUrl/search/$encodedQuery/").document
        return document.select("a.movie-item.m-block").mapNotNull { it.toSearchResult() }
    }

    // ───────────────────────────────────────────────────────────
    // Load movie detail page
    // URL pattern: /{slug}-{id}.html  →  id is the integer at end
    // We store "$id|$serverCount" as loadLinks data
    // ───────────────────────────────────────────────────────────
    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim()
            ?: document.selectFirst("title")?.text()?.trim()
            ?: return null

        val poster = document.selectFirst("meta[property=og:image]")?.attr("content")
            ?: document.selectFirst(".thumb img, .movie-img img")?.attr("src")

        val description = document.selectFirst("meta[property=og:description]")?.attr("content")
            ?: document.selectFirst(".movie-desc, .description, .entry-content p")?.text()

        // Extract movie ID from URL: /some-movie-title-3831.html → 3831
        val movieId = Regex("""[/-](\d+)\.html$""").find(url)?.groupValues?.get(1)
            ?: return null

        // Count server buttons; fallback to 4 if none found
        val serverCount = document.select("div.server, [id^=server]").size
            .takeIf { it > 0 } ?: 4

        return newMovieLoadResponse(title, url, TvType.Movie, "$movieId|$serverCount") {
            this.posterUrl = poster
            this.plot = description
        }
    }

    // ───────────────────────────────────────────────────────────
    // Load video links
    // POST /ajax {"id": <movieId>, "server": <1..n>}
    // Response JSON: { "player": "<script>...window.atob('BASE64')...</script>" }
    // Decode base64 → actual video URL (.m3u8 or .mp4)
    // ───────────────────────────────────────────────────────────
    @Serializable
    data class AjaxResponse(val player: String = "")

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val parts = data.split("|")
        val movieId = parts[0].toLongOrNull() ?: return false
        val serverCount = parts.getOrNull(1)?.toIntOrNull() ?: 4

        var foundAny = false

        for (server in 1..serverCount) {
            try {
                val response = app.post(
                    url = "$mainUrl/ajax",
                    headers = mapOf(
                        "X-Requested-With" to "XMLHttpRequest",
                        "Accept"           to "application/json, text/javascript, */*; q=0.01",
                    ),
                    referer = mainUrl,
                    json = mapOf("id" to movieId, "server" to server),
                )

                val playerHtml = response.parsedSafe<AjaxResponse>()?.player
                    ?: continue

                // ── Attempt 1: window.atob("BASE64") ──────────────────
                val base64Str = Regex("""window\.atob\(["']([A-Za-z0-9+/=]+)["']\)""")
                    .find(playerHtml)?.groupValues?.get(1)

                val videoUrl = if (base64Str != null) {
                    try {
                        String(Base64.decode(base64Str, Base64.DEFAULT)).trim()
                    } catch (_: Exception) { null }
                } else {
                    // ── Attempt 2: plain file URL in sources array ─────
                    Regex("""file\s*:\s*["']([^"']+)["']""")
                        .find(playerHtml)?.groupValues?.get(1)
                }

                if (videoUrl.isNullOrBlank()) continue

                val isM3u8 = videoUrl.contains(".m3u8", ignoreCase = true)
                callback.invoke(
                    ExtractorLink(
                        source   = name,
                        name     = "$name – Server $server",
                        url      = videoUrl,
                        referer  = mainUrl,
                        quality  = Qualities.P1080.value,
                        isM3u8   = isM3u8,
                        headers  = mapOf("Referer" to mainUrl, "Origin" to mainUrl),
                    )
                )
                foundAny = true
            } catch (_: Exception) {
                // try next server
            }
        }

        return foundAny
    }
}
