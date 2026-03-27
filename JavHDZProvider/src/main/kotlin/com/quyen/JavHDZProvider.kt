package com.quyen

import android.util.Base64
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import kotlinx.serialization.Serializable
import org.jsoup.nodes.Element

class JavHDZProvider : MainAPI() {
    override var mainUrl = "https://javhdz.hot"
    override var name = "JavHDZ"
    override val hasMainPage = true
    override var lang = "vi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.NSFW) // Standard for Jav

    override val mainPage = mainPageOf(
        "$mainUrl/video/page/"              to "Mới nhất",
        "$mainUrl/trending/page/"           to "Trending",
        "$mainUrl/category/censored-2/page/"   to "Censored",
        "$mainUrl/category/uncensored-3/page/" to "Uncensored",
        "$mainUrl/category/beauty-4/page/"     to "Beauty & More",
    )

    private fun Element.toSearchResult(): MovieSearchResponse? {
        val title = this.selectFirst("h3.m-block-title")?.text()?.trim()
            ?: this.attr("title").trim().takeIf { it.isNotEmpty() }
            ?: return null
        val href = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("src")
                ?: this.selectFirst("img")?.attr("data-src")
        )
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}$page/"
        val document = app.get(url).document
        val items = document.select("a.movie-item.m-block").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, items, hasNext = items.isNotEmpty())
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val encodedQuery = query.trim().replace(" ", "+")
        val document = app.get("$mainUrl/search/$encodedQuery/").document
        return document.select("a.movie-item.m-block").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim()
            ?: document.selectFirst("title")?.text()?.trim()
            ?: return null

        val poster = document.selectFirst("meta[property=og:image]")?.attr("content")
            ?: document.selectFirst(".thumb img, .movie-img img")?.attr("src")

        val description = document.selectFirst("meta[property=og:description]")?.attr("content")
            ?: document.selectFirst(".movie-desc, .description, .entry-content p")?.text()

        val movieId = Regex("""[/-](\d+)\.html$""").find(url)?.groupValues?.get(1)
            ?: return null

        val serverCount = document.select(".server-item").size
            .takeIf { it > 0 } ?: 4

        return newMovieLoadResponse(title, url, TvType.NSFW, "$movieId|$serverCount") {
            this.posterUrl = poster
            this.plot = description
        }
    }

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

                val base64Str = Regex("""window\.atob\(["']([A-Za-z0-9+/=]+)["']\)""")
                    .find(playerHtml)?.groupValues?.get(1)

                val videoUrl = if (base64Str != null) {
                    try {
                        String(Base64.decode(base64Str, Base64.DEFAULT), Charsets.UTF_8).trim()
                    } catch (_: Exception) { null }
                } else {
                    // ── Attempt 2: plain file URL in sources array ─────
                    Regex("""file\s*:\s*["']([^"']+)["']""")
                        .find(playerHtml)?.groupValues?.get(1)
                }

                if (videoUrl.isNullOrBlank()) continue

                val isM3u8 = videoUrl.contains(".m3u8", ignoreCase = true)
                callback.invoke(
                    newExtractorLink(
                        source = this.name,
                        name   = "${this.name} - Server $server",
                        url    = videoUrl,
                        type   = if (isM3u8) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO,
                    ) {
                        this.quality  = Qualities.P1080.value
                        this.referer  = mainUrl
                        this.headers  = mapOf("Referer" to mainUrl, "Origin" to mainUrl)
                    }
                )
                foundAny = true
            } catch (_: Exception) {
                // skip failed server
            }
        }

        return foundAny
    }
}

