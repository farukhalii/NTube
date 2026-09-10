package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.util.PlatformBypassHelper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.VideoItem
import com.example.ui.components.TopDownloadBlue
import com.example.ui.components.YouTubeTopBar
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NTubeRed

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeWebViewScreen(
    initialUrl: String = "https://m.youtube.com",
    onOpenDownloads: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmitted: (String) -> Unit,
    isSearchExpanded: Boolean,
    onToggleSearch: () -> Unit,
    downloadsCount: Int,
    onOpenBrowser: () -> Unit,
    onDownloadDetectedVideo: (VideoItem) -> Unit,
    onOpenWebSearch: () -> Unit = onToggleSearch,
    onBackToFeed: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf("YouTube") }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }

    // Intercept back navigation to go back in WebView history if possible
    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onBackToFeed?.invoke()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header Bar: New Tube Logo on Left, Center Blue Download Button, Earth/Globe Browser & Search on Right
            YouTubeTopBar(
                onOpenDownloads = onOpenDownloads,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onSearchSubmitted = { query ->
                    webViewInstance?.loadUrl(
                        if (query.startsWith("http")) query else "https://m.youtube.com/results?search_query=${android.net.Uri.encode(query)}"
                    )
                    onSearchSubmitted(query)
                },
                isSearchExpanded = isSearchExpanded,
                onToggleSearch = onToggleSearch,
                downloadsCount = downloadsCount,
                onOpenBrowser = onOpenBrowser,
                onOpenWebSearch = onOpenWebSearch,
                onBackToFeed = onBackToFeed
            )

            // Web Loading Progress Indicator
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { loadingProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = TopDownloadBlue,
                    trackColor = Color.Transparent
                )
            }

            // Real YouTube Mobile Web View
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        PlatformBypassHelper.configureWebView(this)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            javaScriptCanOpenWindowsAutomatically = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            cacheMode = WebSettings.LOAD_DEFAULT

                            userAgentString = PlatformBypassHelper.CHROME_MOBILE_UA
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                url?.let { currentUrl = it }
                                PlatformBypassHelper.injectBypass(view)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                url?.let { currentUrl = it }
                                view?.title?.let { if (it.isNotEmpty()) pageTitle = it }
                                PlatformBypassHelper.injectBypass(view)
                                view?.postDelayed({ PlatformBypassHelper.injectBypass(view) }, 600)
                                view?.postDelayed({ PlatformBypassHelper.injectBypass(view) }, 1500)
                                view?.postDelayed({ PlatformBypassHelper.injectBypass(view) }, 3000)
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val reqUrl = request?.url?.toString() ?: return false
                                
                                // Intercept non-http(s) schemes (e.g. intent:, vnd.youtube:) to keep playback in WebView
                                if (!reqUrl.startsWith("http://") && !reqUrl.startsWith("https://")) {
                                    if (reqUrl.startsWith("intent:")) {
                                        try {
                                            val intent = android.content.Intent.parseUri(reqUrl, android.content.Intent.URI_INTENT_SCHEME)
                                            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                            if (!fallbackUrl.isNullOrEmpty()) {
                                                view?.loadUrl(fallbackUrl)
                                                return true
                                            }
                                        } catch (_: Exception) {}
                                    }
                                    return true // Prevent ERR_UNKNOWN_URL_SCHEME
                                }

                                if (reqUrl.contains("youtube.com") || reqUrl.contains("youtu.be")) {
                                    currentUrl = reqUrl
                                    return false // Keep inside WebView
                                }
                                return false
                            }

                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                url?.let {
                                    currentUrl = it
                                    com.example.util.ActiveStreamTracker.lastActivePageUrl = it
                                }
                                view?.title?.let { if (it.isNotEmpty()) pageTitle = it }
                                PlatformBypassHelper.injectBypass(view)
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): android.webkit.WebResourceResponse? {
                                val reqUrl = request?.url?.toString()
                                if (reqUrl != null) {
                                    val referer = request.requestHeaders?.get("Referer") ?: request.requestHeaders?.get("referer")
                                    com.example.util.ActiveStreamTracker.registerStream(reqUrl, request.requestHeaders, referer)
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: android.webkit.SslErrorHandler?,
                                error: android.net.http.SslError?
                            ) {
                                handler?.proceed()
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadingProgress = newProgress / 100f
                                if (newProgress > 40) {
                                    PlatformBypassHelper.injectBypass(view)
                                }
                                if (newProgress >= 100) isLoading = false
                            }

                            override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                                request?.grant(request.resources)
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                title?.let { if (it.isNotEmpty()) pageTitle = it }
                            }
                        }

                        loadUrl(initialUrl)
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    if (initialUrl.isNotEmpty() && !webView.url.orEmpty().startsWith(initialUrl.substringBefore("?")) && webView.url.isNullOrEmpty()) {
                        webView.loadUrl(initialUrl)
                    }
                },
                onRelease = { webView ->
                    webView.stopLoading()
                    webView.removeAllViews()
                    webView.destroy()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        // Floating Blue Circular Download Button
        // (Positioned slightly up on the right side - matching the user's uploaded screenshot!)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
                .size(54.dp)
                .shadow(elevation = 10.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(TopDownloadBlue)
                .border(2.dp, Color(0xDDFFFFFF), CircleShape)
                .clickable {
                    // Extract active video / short from current page
                    extractActiveVideoFromPage(
                        webView = webViewInstance,
                        currentUrl = currentUrl,
                        fallbackTitle = pageTitle,
                        onResult = { videoItem ->
                            onDownloadDetectedVideo(videoItem)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download Active Video or Short",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

/**
 * Extracts active video or short details (title, thumbnail, video ID, channel) from current YouTube page.
 */
private fun extractActiveVideoFromPage(
    webView: WebView?,
    currentUrl: String,
    fallbackTitle: String,
    onResult: (VideoItem) -> Unit
) {
    val isShort = currentUrl.contains("/shorts/")
    val regex = Regex("(?:watch\\?v=|shorts/|youtu\\.be/)([a-zA-Z0-9_-]{11})")
    val videoIdInUrl = regex.find(currentUrl)?.groupValues?.getOrNull(1)

    val js = """
        (function() {
            var vid = '';
            var title = '';
            var channel = '';
            var videoSrc = '';
            var directStreamUrl = '';

            // 1. Try to get data from YouTube player instance (HTML5 player object)
            try {
                var p = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
                if (p) {
                    if (typeof p.getVideoData === 'function') {
                        var data = p.getVideoData();
                        if (data && data.video_id) {
                            vid = data.video_id;
                            if (data.title) title = data.title;
                            if (data.author) channel = data.author;
                        }
                    }
                    if (typeof p.getVideoUrl === 'function') {
                        var pUrl = p.getVideoUrl();
                        var pm = pUrl.match(/(?:watch\?v=|shorts\/)([a-zA-Z0-9_-]{11})/);
                        if (pm && !vid) vid = pm[1];
                    }
                }
            } catch(e) {}

            // 2. Check window.ytInitialPlayerResponse
            try {
                var resp = window.ytInitialPlayerResponse;
                if (!resp && window.ytplayer && window.ytplayer.config && window.ytplayer.config.args) {
                    resp = typeof window.ytplayer.config.args.player_response === 'string' ?
                        JSON.parse(window.ytplayer.config.args.player_response) : window.ytplayer.config.args.player_response;
                }
                if (resp && resp.videoDetails) {
                    if (!vid && resp.videoDetails.videoId) vid = resp.videoDetails.videoId;
                    if (!title && resp.videoDetails.title) title = resp.videoDetails.title;
                    if (!channel && resp.videoDetails.author) channel = resp.videoDetails.author;
                }
                if (resp && resp.streamingData && resp.streamingData.formats) {
                    for (var fi = 0; fi < resp.streamingData.formats.length; fi++) {
                        var fmt = resp.streamingData.formats[fi];
                        if (fmt.url && fmt.url.startsWith('http')) {
                            directStreamUrl = fmt.url;
                            break;
                        }
                    }
                }
            } catch(e) {}

            // 3. Check YouTube Shorts active container
            try {
                var activeReel = document.querySelector('ytm-reel-video-renderer[is-active], [is-active]');
                if (activeReel) {
                    var reelTitle = activeReel.querySelector('.ytm-shorts-video-title, .title, h2, h3');
                    if (reelTitle) title = reelTitle.innerText || reelTitle.textContent;
                    var reelChannel = activeReel.querySelector('.ytm-shorts-channel-name, .channel-name');
                    if (reelChannel) channel = reelChannel.innerText || reelChannel.textContent;
                }
            } catch(e) {}

            // 4. Check URL
            if (!vid) {
                var m = location.href.match(/(?:watch\?v=|shorts\/|youtu\.be\/)([a-zA-Z0-9_-]{11})/);
                if (m) vid = m[1];
            }

            // 5. Look at the currently playing <video> element centered in viewport
            var vids = document.querySelectorAll('video');
            var v = null;
            var maxVScore = -999999;
            var vh = window.innerHeight || 800;
            var centerY = vh / 2;
            for (var vi = 0; vi < vids.length; vi++) {
                var cVid = vids[vi];
                var cr = cVid.getBoundingClientRect();
                if (cr.width <= 0 || cr.height <= 0) continue;
                var visArea = Math.max(0, Math.min(cr.bottom, vh) - Math.max(cr.top, 0)) * cr.width;
                var vScore = visArea - Math.abs((cr.top + cr.bottom)/2 - centerY) * 50;
                if (!cVid.paused && cVid.currentTime > 0) vScore += 5000000;
                if (vScore > maxVScore) {
                    maxVScore = vScore;
                    v = cVid;
                }
            }
            if (v) {
                var s = v.currentSrc || v.src || '';
                if (s && s.startsWith('http')) {
                    videoSrc = s;
                }
            }

            // 6. Title and Channel fallback from DOM
            if (!title) {
                var titleElem = document.querySelector('h1.slim-video-metadata-title, h1.title, .ytm-shorts-title, ytm-single-column-watch-next-results-renderer h1, h1');
                if (titleElem) {
                    title = titleElem.innerText || titleElem.textContent;
                }
            }
            if (!title && document.title) {
                title = document.title.replace(/- YouTube/gi, '').trim();
            }

            if (!channel) {
                var chanElem = document.querySelector('.ytm-slim-owner-channel-name, .ytm-shorts-channel-name, .media-item-byline, .owner-name');
                if (chanElem) {
                    channel = chanElem.innerText || chanElem.textContent;
                }
            }

            return JSON.stringify({
                id: vid || '',
                title: (title || '').trim(),
                channel: (channel || '').trim(),
                videoSrc: videoSrc || '',
                directStreamUrl: directStreamUrl || '',
                url: location.href
            });
        })()
    """.trimIndent()

    webView?.evaluateJavascript(js) { jsonResult ->
        try {
            val unquoted = jsonResult?.trim()?.removeSurrounding("\"")?.replace("\\\"", "\"")?.replace("\\\\", "\\") ?: ""

            val idMatch = Regex("\"id\":\"([^\"]*)\"").find(unquoted)
            val titleMatch = Regex("\"title\":\"([^\"]*)\"").find(unquoted)
            val chanMatch = Regex("\"channel\":\"([^\"]*)\"").find(unquoted)
            val srcMatch = Regex("\"videoSrc\":\"([^\"]*)\"").find(unquoted)
            val directStreamMatch = Regex("\"directStreamUrl\":\"([^\"]*)\"").find(unquoted)

            val parsedId = idMatch?.groupValues?.getOrNull(1).orEmpty()
            val parsedTitle = titleMatch?.groupValues?.getOrNull(1).orEmpty()
            val parsedChannel = chanMatch?.groupValues?.getOrNull(1).orEmpty()
            val parsedSrc = srcMatch?.groupValues?.getOrNull(1).orEmpty()
            val parsedDirect = directStreamMatch?.groupValues?.getOrNull(1).orEmpty()

            val finalId = if (parsedId.isNotEmpty()) parsedId else (videoIdInUrl ?: "yt_${System.currentTimeMillis()}")

            val cleanTitle = when {
                parsedTitle.isNotEmpty() && !parsedTitle.equals("YouTube", ignoreCase = true) -> parsedTitle
                fallbackTitle.contains("- YouTube") -> fallbackTitle.substringBefore("- YouTube").trim()
                fallbackTitle.isNotEmpty() && !fallbackTitle.equals("YouTube", ignoreCase = true) -> fallbackTitle
                isShort -> "YouTube Shorts Video"
                finalId.length == 11 -> "YouTube Video ($finalId)"
                else -> "Active Playing Video"
            }

            val finalChannel = when {
                parsedChannel.isNotEmpty() -> parsedChannel
                isShort -> "YouTube Shorts"
                else -> "YouTube Creator"
            }

            val thumb = if (finalId.length == 11) {
                "https://img.youtube.com/vi/$finalId/hqdefault.jpg"
            } else {
                "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop&q=80"
            }

            val streamUrl = when {
                parsedDirect.isNotEmpty() && parsedDirect.startsWith("http") -> parsedDirect
                parsedSrc.isNotEmpty() && parsedSrc.startsWith("http") -> parsedSrc
                finalId.length == 11 -> "https://www.youtube.com/watch?v=$finalId"
                else -> currentUrl
            }

            onResult(
                VideoItem(
                    id = finalId,
                    title = cleanTitle,
                    channelName = finalChannel,
                    channelAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
                    views = "YouTube Live",
                    uploadTime = "Recent",
                    duration = if (isShort) "Shorts" else "Full HD",
                    thumbnailUrl = thumb,
                    videoUrl = streamUrl
                )
            )
        } catch (e: Exception) {
            val finalId = videoIdInUrl ?: "yt_${System.currentTimeMillis()}"
            onResult(
                VideoItem(
                    id = finalId,
                    title = fallbackTitle.substringBefore("- YouTube").trim().ifEmpty { "YouTube Video" },
                    channelName = "YouTube Web",
                    channelAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
                    views = "YouTube Live",
                    uploadTime = "Recent",
                    duration = "Full HD",
                    thumbnailUrl = "https://img.youtube.com/vi/$finalId/hqdefault.jpg",
                    videoUrl = if (finalId.length == 11) "https://www.youtube.com/watch?v=$finalId" else currentUrl
                )
            )
        }
    } ?: run {
        val finalId = videoIdInUrl ?: "yt_${System.currentTimeMillis()}"
        onResult(
            VideoItem(
                id = finalId,
                title = fallbackTitle.substringBefore("- YouTube").trim().ifEmpty { "YouTube Video" },
                channelName = "YouTube Web",
                channelAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
                views = "YouTube Live",
                uploadTime = "Recent",
                duration = "Full HD",
                thumbnailUrl = "https://img.youtube.com/vi/$finalId/hqdefault.jpg",
                videoUrl = if (finalId.length == 11) "https://www.youtube.com/watch?v=$finalId" else currentUrl
            )
        )
    }
}

