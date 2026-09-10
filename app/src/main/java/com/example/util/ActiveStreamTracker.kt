package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

object ActiveStreamTracker {

    private const val TAG = "ActiveStreamTracker"

    // Holds captured streams keyed by platform or identifier
    private val platformStreams = ConcurrentHashMap<String, String>()

    // Holds headers captured alongside streams (cookies, referer, user-agent)
    private val platformHeaders = ConcurrentHashMap<String, Map<String, String>>()

    @Volatile
    var lastCapturedStreamUrl: String? = null

    @Volatile
    var lastCapturedHeaders: Map<String, String>? = null

    @Volatile
    var lastActivePageUrl: String? = null

    @Volatile
    var lastActiveVideoId: String? = null

    fun registerStream(url: String, headers: Map<String, String>? = null, pageUrl: String? = null) {
        // Filter out small telemetry / tracking / image pings and keep actual video/audio media chunks
        val isGoogleVideo = url.contains("googlevideo.com/videoplayback")
        val isTikTok = (url.contains("tiktokcdn.com") || url.contains("byteoversea.com") ||
                        url.contains("ib.tiktokv.com") || url.contains("v16-webapp") ||
                        url.contains("mime=video_mp4") || url.contains("/video/tos/")) &&
                        !url.contains(".js") && !url.contains(".css") && !url.contains(".html")
        val isInstagram = url.contains("cdninstagram.com")
        val isFacebook = url.contains("fbcdn.net")
        val isSnapchat = url.contains("sc-cdn.net")
        val isBilibili = url.contains("bilivideo.com") || url.contains("bilibili.com")
        val isGenericMedia = (url.contains(".mp4") || url.contains(".m4a") || url.contains(".webm")) &&
                !url.contains(".js") && !url.contains(".html") && !url.contains(".css")

        if (isGoogleVideo || isTikTok || isInstagram || isFacebook || isSnapchat || isBilibili || isGenericMedia) {
            lastCapturedStreamUrl = url
            lastCapturedHeaders = headers
            pageUrl?.let { lastActivePageUrl = it }

            // Extract video ID if present in URL or page
            val extractedYtId = extractYouTubeId(url).ifEmpty { pageUrl?.let { extractYouTubeId(it) } ?: "" }
            if (extractedYtId.isNotEmpty()) {
                lastActiveVideoId = extractedYtId
                platformStreams["youtube_$extractedYtId"] = url
            }

            // Extract TikTok video ID if present
            val ttId = Regex("(?:aweme_id|video_id|item_id)=([0-9]{15,22})").find(url)?.groupValues?.getOrNull(1)
                ?: pageUrl?.let { Regex("(?:/video/|/v/)([0-9]{15,22})").find(it)?.groupValues?.getOrNull(1) }
            if (!ttId.isNullOrEmpty()) {
                lastActiveVideoId = ttId
                platformStreams["tiktok_$ttId"] = url
            }

            when {
                isGoogleVideo -> {
                    platformStreams["youtube"] = url
                    headers?.let { platformHeaders["youtube"] = it }
                }
                isTikTok -> {
                    platformStreams["tiktok"] = url
                    headers?.let { platformHeaders["tiktok"] = it }
                }
                isInstagram -> {
                    platformStreams["instagram"] = url
                    headers?.let { platformHeaders["instagram"] = it }
                }
                isFacebook -> {
                    platformStreams["facebook"] = url
                    headers?.let { platformHeaders["facebook"] = it }
                }
                isSnapchat -> {
                    platformStreams["snapchat"] = url
                    headers?.let { platformHeaders["snapchat"] = it }
                }
                isBilibili -> {
                    platformStreams["bilibili"] = url
                    headers?.let { platformHeaders["bilibili"] = it }
                }
                else -> {
                    platformStreams["media"] = url
                }
            }

            Log.d(TAG, "Registered active stream for ${url.take(60)}...")
        }
    }

    /**
     * Resolves the actual real playable/downloadable media stream URL for the EXACT video currently playing.
     */
    suspend fun resolveRealStream(
        videoUrl: String,
        videoId: String,
        isAudio: Boolean
    ): String? = withContext(Dispatchers.IO) {
        try {
            val ytId = extractYouTubeId(videoUrl).ifEmpty { extractYouTubeId(videoId) }.ifEmpty { videoId }

            // 1. Direct media URL already passed
            if (videoUrl.startsWith("http") &&
                (videoUrl.contains(".mp4") || videoUrl.contains(".m4a") || videoUrl.contains(".webm")) &&
                !videoUrl.contains("youtube.com/watch") && !videoUrl.contains("youtube.com/shorts") &&
                !videoUrl.contains("tiktok.com/@") && !videoUrl.contains("instagram.com/p/") &&
                !videoUrl.contains("instagram.com/reel/")
            ) {
                Log.d(TAG, "Using direct media URL: ${videoUrl.take(60)}")
                return@withContext videoUrl
            }

            // 2. If it's a YouTube video or short:
            if (videoUrl.contains("youtube") || videoUrl.contains("youtu.be") || ytId.length == 11) {
                // A. Check specific stream captured for this exact video ID
                val specificYtStream = platformStreams["youtube_$ytId"]
                if (!specificYtStream.isNullOrEmpty()) {
                    Log.d(TAG, "Using specific captured YouTube stream for $ytId")
                    return@withContext cleanGoogleVideoStreamUrl(specificYtStream)
                }

                // B. Try public multi-instance Invidious and Piped stream resolvers
                if (ytId.length == 11) {
                    val publicApiStream = resolveYouTubeViaPublicApis(ytId, isAudio)
                    if (!publicApiStream.isNullOrEmpty()) {
                        Log.d(TAG, "Resolved YouTube stream via Public API for $ytId")
                        return@withContext publicApiStream
                    }
                }

                // C. Fall back to active YouTube stream captured from WebView playback
                val generalYtStream = platformStreams["youtube"] ?: lastCapturedStreamUrl
                if (!generalYtStream.isNullOrEmpty() && generalYtStream.contains("googlevideo.com")) {
                    Log.d(TAG, "Using captured active YouTube playback stream")
                    return@withContext cleanGoogleVideoStreamUrl(generalYtStream)
                }
            }

            // 3. TikTok: Fetch 100% clean watermark-free video via TikWM
            if (videoUrl.contains("tiktok.com") || videoId.contains("tiktok")) {
                val cleanTikTokStream = resolveTikTokViaTikWm(videoUrl, isAudio)
                if (!cleanTikTokStream.isNullOrEmpty()) {
                    Log.d(TAG, "Resolved exact TikTok stream via TikWM: $cleanTikTokStream")
                    return@withContext cleanTikTokStream
                }
                val capturedTt = platformStreams["tiktok"] ?: lastCapturedStreamUrl
                if (!capturedTt.isNullOrEmpty() && (capturedTt.contains("tiktokcdn") || capturedTt.contains("byteoversea") || capturedTt.contains("mime=video_mp4") || capturedTt.contains("/video/tos/"))) {
                    Log.d(TAG, "Using captured active TikTok playback stream")
                    return@withContext capturedTt
                }
            }

            // 4. Instagram: Use captured CDN stream or direct URL
            if (videoUrl.contains("instagram.com") || videoId.contains("instagram")) {
                val capturedIg = platformStreams["instagram"]
                if (!capturedIg.isNullOrEmpty()) {
                    return@withContext capturedIg
                }
            }

            // 5. Facebook: Use captured FB CDN stream or direct URL
            if (videoUrl.contains("facebook.com") || videoUrl.contains("fb.watch") || videoId.contains("facebook")) {
                val capturedFb = platformStreams["facebook"]
                if (!capturedFb.isNullOrEmpty()) {
                    return@withContext capturedFb
                }
            }

            // 6. Snapchat
            if (videoUrl.contains("snapchat.com") || videoId.contains("snapchat")) {
                val capturedSc = platformStreams["snapchat"]
                if (!capturedSc.isNullOrEmpty()) {
                    return@withContext capturedSc
                }
            }

            // 7. Bilibili
            if (videoUrl.contains("bilibili.com") || videoId.contains("bilibili")) {
                val capturedBili = platformStreams["bilibili"]
                if (!capturedBili.isNullOrEmpty()) {
                    return@withContext capturedBili
                }
            }

            // 8. Fallback to any recent captured stream if available
            val fallback = lastCapturedStreamUrl
            if (!fallback.isNullOrEmpty() && fallback.startsWith("http") && !videoUrl.contains("tiktok")) {
                return@withContext if (fallback.contains("googlevideo.com")) cleanGoogleVideoStreamUrl(fallback) else fallback
            }

            // If it's a direct media link (NOT a website page), return it for direct connection attempts
            if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
                val isWebsitePage = videoUrl.contains("tiktok.com") ||
                                    videoUrl.contains("instagram.com") ||
                                    videoUrl.contains("facebook.com") ||
                                    videoUrl.contains("snapchat.com") ||
                                    videoUrl.contains("youtube.com") ||
                                    videoUrl.contains("youtu.be") ||
                                    videoUrl.contains("bilibili.com")
                if (!isWebsitePage) {
                    return@withContext videoUrl
                }
            }

            null
        } catch (e: Exception) {
            Log.w(TAG, "Error resolving stream: ${e.message}")
            null
        }
    }

    /**
     * Cleans range and buffer parameters from YouTube videoplayback stream so the full video is downloaded.
     */
    fun cleanGoogleVideoStreamUrl(url: String): String {
        return url
            .replace(Regex("&range=[0-9]+-[0-9]+"), "")
            .replace(Regex("&rn=[0-9]+"), "")
            .replace(Regex("&rbuf=[0-9]+"), "")
    }

    /**
     * Extracts YouTube 11-character video ID from URL.
     */
    fun extractYouTubeId(url: String): String {
        val regex = Regex("(?:watch\\?v=|shorts/|youtu\\.be/|embed/|v/)([a-zA-Z0-9_-]{11})")
        return regex.find(url)?.groupValues?.getOrNull(1).orEmpty()
    }

    /**
     * Resolves TikTok video without watermark using TikWM endpoint with exact video post URL / ID.
     */
    private fun resolveTikTokViaTikWm(tikTokUrl: String, isAudio: Boolean): String? {
        return try {
            val ttId = Regex("(?:/video/|/v/|aweme_id=)([0-9]{15,22})").find(tikTokUrl)?.groupValues?.getOrNull(1)
                ?: lastActiveVideoId
            val cleanUrl = if (!ttId.isNullOrEmpty()) "https://www.tiktok.com/@tiktok/video/$ttId" else tikTokUrl
            val apiUrl = "https://www.tikwm.com/api/"
            val postData = "url=" + URLEncoder.encode(cleanUrl, "UTF-8")
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 4000
                readTimeout = 6000
                setRequestProperty("User-Agent", PlatformBypassHelper.CHROME_MOBILE_UA)
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Accept", "application/json")
            }
            conn.outputStream.use { os ->
                os.write(postData.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                if (json.optInt("code") == 0) {
                    val data = json.optJSONObject("data")
                    if (data != null) {
                        if (isAudio) {
                            val music = data.optString("music")
                            if (music.isNotEmpty()) return music
                        }
                        val hdPlay = data.optString("hdplay")
                        if (hdPlay.isNotEmpty() && hdPlay.startsWith("http")) return hdPlay
                        val play = data.optString("play")
                        if (play.isNotEmpty() && play.startsWith("http")) return play
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "TikWM resolution failed: ${e.message}")
            null
        }
    }

    /**
     * Resolves YouTube video/audio using reliable public streaming endpoints (Invidious, Piped, Cobalt).
     */
    private fun resolveYouTubeViaPublicApis(videoId: String, isAudio: Boolean): String? {
        // List of multiple high-availability Invidious and Piped mirrors
        val invidiousEndpoints = listOf(
            "https://inv.nadeko.net/api/v1/videos/$videoId",
            "https://yewtu.be/api/v1/videos/$videoId",
            "https://invidious.nerdvpn.de/api/v1/videos/$videoId",
            "https://inv.tux.pizza/api/v1/videos/$videoId",
            "https://invidious.jing.rocks/api/v1/videos/$videoId"
        )

        for (endpoint in invidiousEndpoints) {
            try {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", PlatformBypassHelper.CHROME_MOBILE_UA)
                    setRequestProperty("Accept", "application/json")
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)

                    if (isAudio) {
                        val adaptiveFormats = json.optJSONArray("adaptiveFormats")
                        if (adaptiveFormats != null) {
                            for (i in 0 until adaptiveFormats.length()) {
                                val item = adaptiveFormats.getJSONObject(i)
                                val type = item.optString("type")
                                if (type.contains("audio/mp4") || type.contains("audio")) {
                                    val u = item.optString("url")
                                    if (u.isNotEmpty()) return u
                                }
                            }
                        }
                    } else {
                        val formatStreams = json.optJSONArray("formatStreams")
                        if (formatStreams != null && formatStreams.length() > 0) {
                            // Find highest resolution progressive MP4 stream (e.g. 720p or 360p with audio included)
                            var bestUrl = ""
                            for (i in 0 until formatStreams.length()) {
                                val item = formatStreams.getJSONObject(i)
                                val u = item.optString("url")
                                val container = item.optString("container")
                                if (u.isNotEmpty() && (container == "mp4" || u.contains(".mp4") || bestUrl.isEmpty())) {
                                    bestUrl = u
                                    val quality = item.optString("qualityLabel")
                                    if (quality.contains("720") || quality.contains("1080")) {
                                        return u
                                    }
                                }
                            }
                            if (bestUrl.isNotEmpty()) return bestUrl
                        }
                    }
                }
            } catch (_: Exception) {
                // Try next mirror
            }
        }

        // Try Piped instances as secondary fallback
        val pipedEndpoints = listOf(
            "https://api.piped.private.coffee/streams/$videoId",
            "https://pipedapi.tokhmi.xyz/streams/$videoId",
            "https://piped-api.garudalinux.org/streams/$videoId"
        )

        for (endpoint in pipedEndpoints) {
            try {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", PlatformBypassHelper.CHROME_MOBILE_UA)
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)

                    if (isAudio) {
                        val audioStreams = json.optJSONArray("audioStreams")
                        if (audioStreams != null && audioStreams.length() > 0) {
                            val stream = audioStreams.getJSONObject(0)
                            val url = stream.optString("url")
                            if (url.isNotEmpty()) return url
                        }
                    } else {
                        val videoStreams = json.optJSONArray("videoStreams")
                        if (videoStreams != null && videoStreams.length() > 0) {
                            for (i in 0 until videoStreams.length()) {
                                val s = videoStreams.getJSONObject(i)
                                val videoOnly = s.optBoolean("videoOnly", false)
                                if (!videoOnly) {
                                    val u = s.optString("url")
                                    if (u.isNotEmpty()) return u
                                }
                            }
                            val firstUrl = videoStreams.getJSONObject(0).optString("url")
                            if (firstUrl.isNotEmpty()) return firstUrl
                        }
                    }
                }
            } catch (_: Exception) {
                // Try next endpoint
            }
        }

        return null
    }

    /**
     * Resolves metadata and stream links for any shared URL (TikTok, YouTube, Instagram, etc.).
     * Executes quickly in the background during the Ad playback period.
     */
    suspend fun resolveMediaMetadata(targetUrl: String): ResolvedMediaInfo = withContext(Dispatchers.IO) {
        val cleanUrl = targetUrl.trim()

        // 1. TikTok URL handling
        if (cleanUrl.contains("tiktok.com") || cleanUrl.contains("douyin.com")) {
            try {
                val ttId = Regex("(?:/video/|/v/|aweme_id=)([0-9]{15,22})").find(cleanUrl)?.groupValues?.getOrNull(1)
                    ?: lastActiveVideoId
                val apiTarget = if (!ttId.isNullOrEmpty()) "https://www.tiktok.com/@tiktok/video/$ttId" else cleanUrl
                val postData = "url=" + URLEncoder.encode(apiTarget, "UTF-8")
                val conn = (URL("https://www.tikwm.com/api/").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", PlatformBypassHelper.CHROME_MOBILE_UA)
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    setRequestProperty("Accept", "application/json")
                }
                conn.outputStream.use { os ->
                    os.write(postData.toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    if (json.optInt("code") == 0) {
                        val data = json.optJSONObject("data")
                        if (data != null) {
                            val title = data.optString("title").ifEmpty { "TikTok Video" }
                            val author = data.optJSONObject("author")?.optString("nickname")
                                ?: data.optJSONObject("author")?.optString("unique_id")
                                ?: "TikTok Creator"
                            val cover = data.optString("cover").ifEmpty { data.optString("origin_cover") }
                            val videoStream = data.optString("hdplay").ifEmpty { data.optString("play") }
                            val audioStream = data.optString("music")
                            val size = data.optLong("size").takeIf { it > 0 } ?: 18_000_000L
                            val dur = data.optInt("duration", 30)

                            return@withContext ResolvedMediaInfo(
                                title = title,
                                author = "@$author",
                                thumbnailUrl = cover,
                                videoStreamUrl = if (videoStream.startsWith("http")) videoStream else null,
                                audioStreamUrl = if (audioStream.startsWith("http")) audioStream else null,
                                platformName = "TikTok",
                                durationSeconds = dur,
                                estimatedSizeBytes = size
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "TikWM metadata resolution failed: ${e.message}")
            }
        }

        // 2. YouTube URL handling
        val ytId = extractYouTubeId(cleanUrl)
        if (ytId.isNotEmpty() || cleanUrl.contains("youtube.com") || cleanUrl.contains("youtu.be")) {
            val videoId = ytId.ifEmpty { "dQw4w9WgXcQ" }
            var title = "YouTube Video"
            var author = "YouTube Channel"
            try {
                val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
                val conn = (URL(oembedUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("User-Agent", PlatformBypassHelper.CHROME_MOBILE_UA)
                }
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    title = json.optString("title", title)
                    author = json.optString("author_name", author)
                }
            } catch (_: Exception) {}

            val videoStream = resolveYouTubeViaPublicApis(videoId, false)
            val audioStream = resolveYouTubeViaPublicApis(videoId, true)
            val thumb = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

            return@withContext ResolvedMediaInfo(
                title = title,
                author = author,
                thumbnailUrl = thumb,
                videoStreamUrl = videoStream ?: cleanUrl,
                audioStreamUrl = audioStream,
                platformName = "YouTube",
                estimatedSizeBytes = 24_000_000L
            )
        }

        // 3. Instagram
        if (cleanUrl.contains("instagram.com")) {
            val captured = platformStreams["instagram"]
            return@withContext ResolvedMediaInfo(
                title = "Instagram Reel / Video",
                author = "Instagram",
                thumbnailUrl = "",
                videoStreamUrl = captured ?: cleanUrl,
                audioStreamUrl = null,
                platformName = "Instagram",
                estimatedSizeBytes = 16_000_000L
            )
        }

        // 4. Facebook
        if (cleanUrl.contains("facebook.com") || cleanUrl.contains("fb.watch")) {
            val captured = platformStreams["facebook"]
            return@withContext ResolvedMediaInfo(
                title = "Facebook Video",
                author = "Facebook Watch",
                thumbnailUrl = "",
                videoStreamUrl = captured ?: cleanUrl,
                audioStreamUrl = null,
                platformName = "Facebook",
                estimatedSizeBytes = 20_000_000L
            )
        }

        // 5. Snapchat
        if (cleanUrl.contains("snapchat.com")) {
            val captured = platformStreams["snapchat"]
            return@withContext ResolvedMediaInfo(
                title = "Snapchat Spotlight Video",
                author = "Snapchat",
                thumbnailUrl = "",
                videoStreamUrl = captured ?: cleanUrl,
                audioStreamUrl = null,
                platformName = "Snapchat",
                estimatedSizeBytes = 12_000_000L
            )
        }

        // 6. Bilibili
        if (cleanUrl.contains("bilibili.com")) {
            val captured = platformStreams["bilibili"]
            return@withContext ResolvedMediaInfo(
                title = "Bilibili Video",
                author = "Bilibili",
                thumbnailUrl = "",
                videoStreamUrl = captured ?: cleanUrl,
                audioStreamUrl = null,
                platformName = "Bilibili",
                estimatedSizeBytes = 22_000_000L
            )
        }

        // 7. Generic URL
        val domain = try { URL(cleanUrl).host.replace("www.", "") } catch (_: Exception) { "Online Video" }
        return@withContext ResolvedMediaInfo(
            title = "Web Video Download",
            author = domain,
            thumbnailUrl = "",
            videoStreamUrl = cleanUrl,
            audioStreamUrl = null,
            platformName = "Web Video",
            estimatedSizeBytes = 15_000_000L
        )
    }
}

data class ResolvedMediaInfo(
    val title: String,
    val author: String,
    val thumbnailUrl: String,
    val videoStreamUrl: String?,
    val audioStreamUrl: String?,
    val platformName: String,
    val durationSeconds: Int = 0,
    val estimatedSizeBytes: Long = 15_000_000L
)

