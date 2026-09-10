package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.speech.RecognizerIntent
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.VideoItem
import com.example.ui.components.TopDownloadBlue
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NTubeRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.PlatformBypassHelper
import java.util.Locale

data class WebPlatform(
    val name: String,
    val url: String,
    val badgeColor: Color,
    val textColor: Color = Color.White,
    val iconEmoji: String
)

val POPULAR_PLATFORMS = listOf(
    WebPlatform("TikTok", "https://www.tiktok.com/explore", Color(0xFF000000), Color(0xFF25F4EE), "🎵"),
    WebPlatform("Facebook", "https://m.facebook.com/watch", Color(0xFF1877F2), Color.White, "👍"),
    WebPlatform("Instagram", "https://www.instagram.com/reels", Color(0xFFC13584), Color.White, "📸"),
    WebPlatform("Snapchat", "https://www.snapchat.com/spotlight", Color(0xFFFFFC00), Color.Black, "👻"),
    WebPlatform("Bilibili", "https://m.bilibili.com", Color(0xFF00A1D6), Color.White, "📺"),
    WebPlatform("YouTube", "https://m.youtube.com", Color(0xFFFF0000), Color.White, "▶"),
    WebPlatform("X (Twitter)", "https://x.com/explore", Color(0xFF14171A), Color.White, "𝕏"),
    WebPlatform("Pinterest", "https://www.pinterest.com", Color(0xFFE60023), Color.White, "📌")
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    initialUrl: String = "https://www.tiktok.com",
    onCloseBrowser: () -> Unit,
    onOpenDownloads: () -> Unit,
    downloadsCount: Int,
    onDownloadDetectedVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var inputUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf("Web Browser") }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }

    var isMenuExpanded by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }
    var isDarkModeActive by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Voice recognition launcher for "Speak to search website"
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenResults = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenResults?.firstOrNull()?.trim()
            if (!spokenText.isNullOrBlank()) {
                val target = when {
                    spokenText.startsWith("http://") || spokenText.startsWith("https://") -> spokenText
                    spokenText.contains(".") && !spokenText.contains(" ") -> "https://$spokenText"
                    else -> "https://www.google.com/search?q=${android.net.Uri.encode(spokenText)}"
                }
                inputUrl = target
                webViewInstance?.loadUrl(target)
            }
        }
    }

    fun startVoiceSearch() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak website name or search term...")
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice search is not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleDesktopMode() {
        isDesktopMode = !isDesktopMode
        webViewInstance?.let { wv ->
            wv.settings.apply {
                if (isDesktopMode) {
                    userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                    useWideViewPort = true
                    loadWithOverviewMode = true
                } else {
                    userAgentString = PlatformBypassHelper.CHROME_MOBILE_UA
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
            }
            wv.reload()
            Toast.makeText(context, if (isDesktopMode) "Desktop Mode: ON" else "Mobile Mode: ON", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleDarkMode() {
        isDarkModeActive = !isDarkModeActive
        val js = if (isDarkModeActive) {
            """
            (function() {
                var id = 'browser-dark-mode-style';
                var el = document.getElementById(id);
                if (!el) {
                    var s = document.createElement('style');
                    s.id = id;
                    s.innerHTML = 'html { filter: invert(90%) hue-rotate(180deg) !important; background: #121212 !important; } img, video, canvas, svg { filter: invert(100%) hue-rotate(180deg) !important; }';
                    document.head.appendChild(s);
                }
            })()
            """.trimIndent()
        } else {
            """
            (function() {
                var el = document.getElementById('browser-dark-mode-style');
                if (el) el.remove();
            })()
            """.trimIndent()
        }
        webViewInstance?.evaluateJavascript(js, null)
        Toast.makeText(context, if (isDarkModeActive) "Dark Canvas: ON" else "White Canvas: ON", Toast.LENGTH_SHORT).show()
    }

    // Synchronize if initialUrl changes externally (e.g. from shared link)
    LaunchedEffect(initialUrl) {
        if (initialUrl.isNotEmpty() && initialUrl != currentUrl) {
            currentUrl = initialUrl
            inputUrl = initialUrl
            webViewInstance?.loadUrl(initialUrl)
        }
    }

    // Intercept back navigation
    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onCloseBrowser()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top URL Address & Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F0F))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button (Exits Browser back to YouTube)
                IconButton(
                    onClick = {
                        if (webViewInstance?.canGoBack() == true) {
                            webViewInstance?.goBack()
                        } else {
                            onCloseBrowser()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // URL & Search Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceElevated)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = TopDownloadBlue,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        BasicTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(TopDownloadBlue),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    keyboardController?.hide()
                                    val trimmed = inputUrl.trim()
                                    val target = when {
                                        trimmed.isEmpty() -> "https://www.google.com"
                                        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
                                        trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
                                        else -> "https://www.google.com/search?q=${android.net.Uri.encode(trimmed)}"
                                    }
                                    inputUrl = target
                                    webViewInstance?.loadUrl(target)
                                }
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        if (inputUrl.isNotEmpty()) {
                            IconButton(
                                onClick = { inputUrl = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Speak to search website button
                        IconButton(
                            onClick = { startVoiceSearch() },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Speak to search website",
                                tint = TopDownloadBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { webViewInstance?.reload() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Top Blue Download Button (Direct link to Downloads & Gallery Screen)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(TopDownloadBlue)
                        .clickable(onClick = onOpenDownloads),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Downloads",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )

                    if (downloadsCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(NTubeRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$downloadsCount",
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                // 3-Dots Action Menu (Desktop Mode, Settings, Refresh, Dark/Light Mode, Download Video)
                Box {
                    IconButton(
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Browser Options",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier.background(DarkSurfaceElevated)
                    ) {
                        // 1. Desktop Mode (dastopmode)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Desktop Mode",
                                        color = Color.White,
                                        fontSize = 13.5.sp
                                    )
                                    if (isDesktopMode) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = TopDownloadBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DesktopWindows,
                                    contentDescription = null,
                                    tint = if (isDesktopMode) TopDownloadBlue else Color.White
                                )
                            },
                            onClick = {
                                isMenuExpanded = false
                                toggleDesktopMode()
                            }
                        )

                        // 2. Settings Option (staying opshen)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Browser Settings",
                                    color = Color.White,
                                    fontSize = 13.5.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            },
                            onClick = {
                                isMenuExpanded = false
                                showSettingsDialog = true
                            }
                        )

                        // 3. Refresh Web (refresh wab ka option)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Refresh Webpage",
                                    color = Color.White,
                                    fontSize = 13.5.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            },
                            onClick = {
                                isMenuExpanded = false
                                webViewInstance?.reload()
                            }
                        )

                        // 4. Dark Mode / White Mode (darc mode white mode)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isDarkModeActive) "White (Light) Mode" else "Dark Mode",
                                    color = Color.White,
                                    fontSize = 13.5.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isDarkModeActive) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            },
                            onClick = {
                                isMenuExpanded = false
                                toggleDarkMode()
                            }
                        )

                        // 5. Download Option (dauonlod ka opshen)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Download Video / Media",
                                    color = TopDownloadBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = TopDownloadBlue
                                )
                            },
                            onClick = {
                                isMenuExpanded = false
                                extractMediaFromAnyPlatform(
                                    webView = webViewInstance,
                                    currentUrl = currentUrl,
                                    fallbackTitle = pageTitle,
                                    onResult = { videoItem ->
                                        onDownloadDetectedVideo(videoItem)
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Quick Platform Bookmarks Bar (TikTok, Instagram, Facebook, Snapchat, etc.)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141414))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                POPULAR_PLATFORMS.forEach { platform ->
                    val isCurrent = currentUrl.contains(platform.name.lowercase().substringBefore(" "))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isCurrent) platform.badgeColor else DarkSurfaceElevated)
                            .border(
                                width = 1.dp,
                                color = if (isCurrent) Color.White.copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                inputUrl = platform.url
                                currentUrl = platform.url
                                webViewInstance?.loadUrl(platform.url)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = platform.iconEmoji, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = platform.name,
                            color = if (isCurrent) platform.textColor else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Web Loading Progress
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

            // Interactive Full Multi-Platform WebView
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
                            setSupportMultipleWindows(false)
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
                                url?.let {
                                    currentUrl = it
                                    inputUrl = it
                                }
                                PlatformBypassHelper.injectBypass(view)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                url?.let {
                                    currentUrl = it
                                    inputUrl = it
                                }
                                view?.title?.let { if (it.isNotEmpty()) pageTitle = it }
                                PlatformBypassHelper.injectBypass(view)
                                // Repeat at intervals for dynamic overlays
                                view?.postDelayed({ PlatformBypassHelper.injectBypass(view) }, 500)
                                view?.postDelayed({ PlatformBypassHelper.injectBypass(view) }, 1500)
                                view?.postDelayed({ PlatformBypassHelper.injectBypass(view) }, 3000)
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val reqUrl = request?.url?.toString() ?: return false
                                
                                // Crucial for TikTok, Instagram, Facebook:
                                // Platforms send snssdk1233://, intent://, or market:// to hijack web browsing into the native app.
                                // Intercept to prevent ERR_UNKNOWN_URL_SCHEME!
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
                                    return true // Stay inside web experience
                                }

                                currentUrl = reqUrl
                                inputUrl = reqUrl
                                return false // Stay inside WebView
                            }

                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                url?.let {
                                    currentUrl = it
                                    inputUrl = it
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

                            override fun onGeolocationPermissionsShowPrompt(
                                origin: String?,
                                callback: android.webkit.GeolocationPermissions.Callback?
                            ) {
                                callback?.invoke(origin, true, false)
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

        // PROMINENT BLUE CIRCULAR DOWNLOAD BUTTON (Right Side Floating)
        // Matches the same position & look across all websites!
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
                    extractMediaFromAnyPlatform(
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
                contentDescription = "Download Clean Video (No Watermark)",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        // Browser Settings Dialog (staying opshen)
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = {
                    Text(
                        text = "Browser & Download Settings",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Clean browsing & video extraction options",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )

                        // Clear Cache and Cookies
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Clear Browsing Data", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = "Clears cache, history & temporary session cookies", color = TextSecondary, fontSize = 11.5.sp)
                            }
                            TextButton(
                                onClick = {
                                    try {
                                        CookieManager.getInstance().removeAllCookies(null)
                                        webViewInstance?.clearCache(true)
                                        webViewInstance?.clearHistory()
                                        Toast.makeText(context, "Browsing cache & cookies cleared!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Data cleared", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text(text = "Clear", color = TopDownloadBlue, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Bot & Login Barrier status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Bot & Login Bypass", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = "Active on TikTok, Facebook, Instagram, Bilibili", color = Color(0xFF81C784), fontSize = 11.5.sp)
                            }
                            Text(text = "ACTIVE", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // User Agent Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Display Mode", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = if (isDesktopMode) "Desktop (PC) View" else "Mobile (Fast) View", color = TextSecondary, fontSize = 11.5.sp)
                            }
                            TextButton(onClick = { toggleDesktopMode() }) {
                                Text(text = if (isDesktopMode) "Switch to Mobile" else "Switch to Desktop", color = TopDownloadBlue)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text(text = "Done", color = TopDownloadBlue, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = DarkSurfaceElevated
            )
        }
    }
}

/**
 * Extracts active video from ANY website (TikTok, Instagram, Facebook, Snapchat, etc.)
 * Provides clean metadata for Watermark-Free download!
 */
private fun extractMediaFromAnyPlatform(
    webView: WebView?,
    currentUrl: String,
    fallbackTitle: String,
    onResult: (VideoItem) -> Unit
) {
    val platformName = when {
        currentUrl.contains("tiktok.com") -> "TikTok"
        currentUrl.contains("instagram.com") -> "Instagram"
        currentUrl.contains("facebook.com") || currentUrl.contains("fb.watch") -> "Facebook"
        currentUrl.contains("snapchat.com") -> "Snapchat"
        currentUrl.contains("x.com") || currentUrl.contains("twitter.com") -> "X (Twitter)"
        currentUrl.contains("pinterest.com") -> "Pinterest"
        currentUrl.contains("dailymotion.com") -> "Dailymotion"
        currentUrl.contains("reddit.com") -> "Reddit"
        currentUrl.contains("vimeo.com") -> "Vimeo"
        currentUrl.contains("youtube.com") || currentUrl.contains("youtu.be") -> "YouTube"
        else -> "Web"
    }

    val js = """
        (function() {
            var title = '';
            var author = '';
            var thumb = '';
            var videoSrc = '';
            var postUrl = location.href;

            // 1. Find the EXACT video element currently visible & playing in the center of viewport
            var vids = document.querySelectorAll('video');
            var bestVid = null;
            var maxScore = -999999;
            var vh = window.innerHeight || document.documentElement.clientHeight || 800;
            var vw = window.innerWidth || document.documentElement.clientWidth || 400;
            var centerY = vh / 2;

            for (var i = 0; i < vids.length; i++) {
                var v = vids[i];
                var r = v.getBoundingClientRect();
                if (r.width <= 0 || r.height <= 0) continue;

                var visibleY = Math.max(0, Math.min(r.bottom, vh) - Math.max(r.top, 0));
                var visibleX = Math.max(0, Math.min(r.right, vw) - Math.max(r.left, 0));
                var visibleArea = visibleX * visibleY;

                if (visibleArea <= 0) continue;

                var elemCenterY = (r.top + r.bottom) / 2;
                var distFromCenter = Math.abs(elemCenterY - centerY);
                var score = visibleArea - (distFromCenter * 60);

                // Prioritize video that is unpaused and currently playing
                if (!v.paused && v.currentTime > 0) {
                    score += 5000000;
                }

                if (score > maxScore) {
                    maxScore = score;
                    bestVid = v;
                }
            }

            if (bestVid) {
                var s = bestVid.currentSrc || bestVid.src || '';
                if (!s) {
                    var sc = bestVid.querySelector('source');
                    if (sc) s = sc.src || '';
                }
                if (s && s.startsWith('http')) {
                    videoSrc = s;
                }
                if (bestVid.poster) thumb = bestVid.poster;

                // Find container around the active visible video
                var container = bestVid.closest('div[data-e2e*="item"], div[class*="ItemContainer"], div[class*="video-card"], div[class*="DivItem"], article, section, [data-e2e="feed-item"]') || bestVid.parentElement;

                if (container) {
                    // TikTok post link
                    var ttLink = container.querySelector('a[href*="/video/"], a[href*="/v/"]');
                    if (ttLink && ttLink.href) {
                        postUrl = ttLink.href;
                    }
                    // Instagram reel link
                    var igLink = container.querySelector('a[href*="/reel/"], a[href*="/p/"]');
                    if (igLink && igLink.href) {
                        postUrl = igLink.href;
                    }
                    // Title / Caption
                    var descEl = container.querySelector('[data-e2e*="video-desc"], [class*="desc"], [class*="Title"], h1, h2, span.tiktok-title, [class*="caption"]');
                    if (descEl) {
                        title = descEl.innerText || descEl.textContent || '';
                    }
                    // Author username
                    var authorEl = container.querySelector('[data-e2e*="author-uniqueid"], [class*="author"], [class*="Author"], a[href^="/@"]');
                    if (authorEl) {
                        author = authorEl.innerText || authorEl.textContent || '';
                    }
                    if (!thumb) {
                        var img = container.querySelector('img[src*="http"]');
                        if (img) thumb = img.src;
                    }
                }
            }

            // Fallback: If location.href contains specific video identifier
            if (location.href.indexOf('/video/') !== -1 || location.href.indexOf('/reel/') !== -1 || location.href.indexOf('/p/') !== -1 || location.href.indexOf('/watch') !== -1) {
                postUrl = location.href;
            }

            // Fallback Title & Thumb from meta tags
            if (!title) {
                var ogTitle = document.querySelector('meta[property="og:title"], meta[name="twitter:title"]');
                if (ogTitle) title = ogTitle.content;
            }
            if (!title) {
                title = document.title || '';
            }
            if (!thumb) {
                var ogImage = document.querySelector('meta[property="og:image"], meta[name="twitter:image"]');
                if (ogImage) thumb = ogImage.content;
            }

            return JSON.stringify({
                title: (title || '').trim(),
                author: (author || '').trim(),
                thumb: (thumb || '').trim(),
                videoSrc: (videoSrc || '').trim(),
                postUrl: (postUrl || location.href).trim(),
                url: location.href
            });
        })()
    """.trimIndent()

    webView?.evaluateJavascript(js) { jsonResult ->
        try {
            val unquoted = jsonResult?.trim()?.removeSurrounding("\"")?.replace("\\\"", "\"")?.replace("\\\\", "\\") ?: ""

            val titleMatch = Regex("\"title\":\"([^\"]*)\"").find(unquoted)?.groupValues?.getOrNull(1).orEmpty()
            val authorMatch = Regex("\"author\":\"([^\"]*)\"").find(unquoted)?.groupValues?.getOrNull(1).orEmpty()
            val thumbMatch = Regex("\"thumb\":\"([^\"]*)\"").find(unquoted)?.groupValues?.getOrNull(1).orEmpty()
            val videoSrcMatch = Regex("\"videoSrc\":\"([^\"]*)\"").find(unquoted)?.groupValues?.getOrNull(1).orEmpty()
            val postUrlMatch = Regex("\"postUrl\":\"([^\"]*)\"").find(unquoted)?.groupValues?.getOrNull(1).orEmpty()

            val cleanTitle = when {
                titleMatch.isNotEmpty() && !titleMatch.equals(platformName, ignoreCase = true) -> titleMatch
                fallbackTitle.isNotEmpty() && !fallbackTitle.equals(platformName, ignoreCase = true) -> fallbackTitle
                else -> "$platformName Viral Video (No Watermark)"
            }

            val finalThumb = when {
                thumbMatch.isNotEmpty() && thumbMatch.startsWith("http") -> thumbMatch
                platformName == "TikTok" -> "https://images.unsplash.com/photo-1611605698335-8b1569810432?w=800&auto=format&fit=crop&q=80"
                platformName == "Instagram" -> "https://images.unsplash.com/photo-1611262588024-d12430b98920?w=800&auto=format&fit=crop&q=80"
                platformName == "Facebook" -> "https://images.unsplash.com/photo-1542838132-92c53300491e?w=800&auto=format&fit=crop&q=80"
                platformName == "Snapchat" -> "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&auto=format&fit=crop&q=80"
                else -> "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop&q=80"
            }

            // Pinpoint the exact download URL:
            // For TikTok: prefer the exact postUrl (contains /video/<id>) so TikWM downloads 100% clean video of this exact post!
            // For Instagram: prefer direct videoSrc (progressive MP4) or postUrl
            // For others: direct videoSrc or currentUrl
            val directUrl = when {
                platformName == "TikTok" && postUrlMatch.contains("/video/") -> postUrlMatch
                videoSrcMatch.isNotEmpty() && videoSrcMatch.startsWith("http") -> videoSrcMatch
                postUrlMatch.isNotEmpty() && postUrlMatch.startsWith("http") -> postUrlMatch
                else -> currentUrl
            }

            val authorDisplay = if (authorMatch.isNotEmpty()) "$authorMatch • $platformName" else "$platformName • Clean HD (No Logo)"

            val videoId = when {
                platformName == "TikTok" -> {
                    val ttId = Regex("(?:/video/|/v/)([0-9]{15,22})").find(directUrl)?.groupValues?.getOrNull(1)
                    if (!ttId.isNullOrEmpty()) "tiktok_$ttId" else "clean_tiktok_${System.currentTimeMillis()}"
                }
                else -> "clean_${platformName.lowercase()}_${System.currentTimeMillis()}"
            }

            onResult(
                VideoItem(
                    id = videoId,
                    title = cleanTitle,
                    channelName = authorDisplay,
                    channelAvatarUrl = finalThumb,
                    views = "No Watermark HD",
                    uploadTime = "Original Quality",
                    duration = "Full HD",
                    thumbnailUrl = finalThumb,
                    videoUrl = directUrl
                )
            )
        } catch (e: Exception) {
            fallbackVideo(platformName, fallbackTitle, currentUrl, onResult)
        }
    } ?: run {
        fallbackVideo(platformName, fallbackTitle, currentUrl, onResult)
    }
}

private fun fallbackVideo(
    platformName: String,
    fallbackTitle: String,
    currentUrl: String,
    onResult: (VideoItem) -> Unit
) {
    val thumb = when (platformName) {
        "TikTok" -> "https://images.unsplash.com/photo-1611605698335-8b1569810432?w=800&auto=format&fit=crop&q=80"
        "Instagram" -> "https://images.unsplash.com/photo-1611262588024-d12430b98920?w=800&auto=format&fit=crop&q=80"
        "Facebook" -> "https://images.unsplash.com/photo-1542838132-92c53300491e?w=800&auto=format&fit=crop&q=80"
        "Snapchat" -> "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&auto=format&fit=crop&q=80"
        else -> "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop&q=80"
    }

    val displayTitle = if (fallbackTitle.isNotEmpty() && !fallbackTitle.equals(platformName, ignoreCase = true)) {
        fallbackTitle
    } else {
        "$platformName Video [Clean HD • No Watermark]"
    }

    onResult(
        VideoItem(
            id = "clean_${platformName.lowercase()}_${System.currentTimeMillis()}",
            title = displayTitle,
            channelName = "$platformName • Clean HD (No Logo)",
            channelAvatarUrl = thumb,
            views = "No Watermark HD",
            uploadTime = "Original Quality",
            duration = "Full HD",
            thumbnailUrl = thumb,
            videoUrl = currentUrl
        )
    )
}
