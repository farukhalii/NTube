package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.DownloadStatus
import com.example.ui.components.DownloadDialog
import com.example.ui.components.PermissionsManagerDialog
import com.example.ui.components.TopDownloadBlue
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.DownloadScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.WebSearchScreen
import com.example.ui.screens.YouTubeWebViewScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initWebViewEnvironment()
        handleIncomingIntent(intent)
        setContent {
            MyApplicationTheme {
                NTubeApp(viewModel = mainViewModel)
            }
        }
    }

    private fun initWebViewEnvironment() {
        try {
            val cacheRoot = File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            File(cacheRoot, "wasm").mkdirs()
            File(cacheRoot, "js").mkdirs()
            File(cacheDir, "WebView/Default/HTTP Cache/index-dir").mkdirs()
        } catch (_: Exception) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    val extractedUrl = extractUrlFromText(sharedText)
                    if (extractedUrl != null) {
                        routeSharedUrl(extractedUrl)
                    }
                }
            }
        } else if (Intent.ACTION_VIEW == action) {
            val dataUri = intent.dataString
            if (!dataUri.isNullOrBlank()) {
                routeSharedUrl(dataUri)
            }
        }
    }

    private fun extractUrlFromText(text: String): String? {
        val urlRegex = Regex("""https?://[^\s]+""")
        return urlRegex.find(text)?.value
    }

    private fun routeSharedUrl(url: String) {
        val quickIntent = Intent(this, QuickDownloadActivity::class.java).apply {
            putExtra(Intent.EXTRA_TEXT, url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(quickIntent)
        finish()
    }
}

@Composable
fun NTubeApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val isDownloadsScreenOpen by viewModel.isDownloadsScreenOpen.collectAsStateWithLifecycle()
    val isBrowserOpen by viewModel.isBrowserOpen.collectAsStateWithLifecycle()
    val isWebSearchPageOpen by viewModel.isWebSearchPageOpen.collectAsStateWithLifecycle()
    val browserCurrentUrl by viewModel.browserCurrentUrl.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchExpanded by viewModel.isSearchExpanded.collectAsStateWithLifecycle()
    val webCurrentUrl by viewModel.webCurrentUrl.collectAsStateWithLifecycle()

    val isLiveWebMode by viewModel.isLiveWebMode.collectAsStateWithLifecycle()
    val selectedPlatform by viewModel.selectedPlatform.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()

    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val selectedVideoForDownload by viewModel.selectedVideoForDownload.collectAsStateWithLifecycle()
    val selectedVideoForPlaying by viewModel.selectedVideoForPlaying.collectAsStateWithLifecycle()

    var showPermissionsDialog by remember { mutableStateOf(false) }

    // Multi-permission request launcher for Files/Gallery, Mic, and Notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsMap[Manifest.permission.POST_NOTIFICATIONS] == true
        } else true

        val micGranted = permissionsMap[Manifest.permission.RECORD_AUDIO] == true
        val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsMap[Manifest.permission.READ_MEDIA_VIDEO] == true || permissionsMap[Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            permissionsMap[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true || permissionsMap[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }

        if (notifGranted && storageGranted) {
            Toast.makeText(context, "Permissions Granted! Download & Notifications active", Toast.LENGTH_SHORT).show()
        }
    }

    val requestAllAppPermissions = {
        val permissionsToRequest = mutableListOf<String>()

        // Notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Microphone
        permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)

        // Storage / Gallery / Media
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    // Auto-prompt critical permissions once on initial launch
    LaunchedEffect(Unit) {
        requestAllAppPermissions()
    }

    // Listen for download completion / progress events
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            snackbarHostState.showSnackbar(message)
        }
    }

    // Intercept back button when in search page, downloads, or browser
    BackHandler(enabled = isDownloadsScreenOpen || isBrowserOpen || isWebSearchPageOpen) {
        if (isWebSearchPageOpen) {
            viewModel.closeWebSearchPage()
        } else if (isDownloadsScreenOpen) {
            viewModel.closeDownloadsScreen()
        } else if (isBrowserOpen) {
            viewModel.closeBrowser()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            // Main Primary View: Live YouTube Mobile Web Experience loaded directly on app start!
            // Clean NewTube branding, no YouTube logo, no bot check, and floating download button on center right.
            YouTubeWebViewScreen(
                initialUrl = webCurrentUrl,
                onOpenDownloads = { viewModel.openDownloadsScreen() },
                searchQuery = searchQuery,
                onSearchQueryChange = { query -> viewModel.onSearchQueryChange(query) },
                onSearchSubmitted = { query -> viewModel.triggerSearchOrNavigate(query) },
                isSearchExpanded = isSearchExpanded,
                onToggleSearch = { viewModel.toggleSearch() },
                downloadsCount = downloads.count { it.status == DownloadStatus.DOWNLOADING },
                onOpenBrowser = { viewModel.openBrowser() },
                onOpenWebSearch = { viewModel.openWebSearchPage() },
                onDownloadDetectedVideo = { video ->
                    viewModel.openDownloadDialog(video)
                }
            )

            // Dedicated Web Search & URL Navigation Page (Opens when user clicks Search button next to Globe)
            AnimatedVisibility(
                visible = isWebSearchPageOpen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                WebSearchScreen(
                    onClose = { viewModel.closeWebSearchPage() },
                    onNavigateToUrl = { url -> viewModel.navigateFromSearchPage(url) }
                )
            }

            // Multi-Platform Web Browser (for TikTok, Instagram, Facebook, Snapchat, Bilibili, etc.)
            AnimatedVisibility(
                visible = isBrowserOpen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                BrowserScreen(
                    initialUrl = browserCurrentUrl,
                    onCloseBrowser = { viewModel.closeBrowser() },
                    onOpenDownloads = { viewModel.openDownloadsScreen() },
                    downloadsCount = downloads.count { it.status == DownloadStatus.DOWNLOADING },
                    onDownloadDetectedVideo = { video ->
                        viewModel.openDownloadDialog(video)
                    }
                )
            }

            // Downloads Screen (Opens when user clicks top blue download button)
            AnimatedVisibility(
                visible = isDownloadsScreenOpen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                DownloadScreen(
                    downloads = downloads,
                    onBackToYouTube = { viewModel.closeDownloadsScreen() },
                    onPlayDownload = { video -> viewModel.openPlayer(video) },
                    onPauseDownload = { id -> viewModel.pauseDownload(id) },
                    onResumeDownload = { id -> viewModel.resumeDownload(id) },
                    onDeleteDownload = { id -> viewModel.deleteDownload(id) },
                    onOpenPermissions = { showPermissionsDialog = true }
                )
            }

            // Permissions Manager Modal Dialog (Files, Gallery, Mic, Notifications)
            if (showPermissionsDialog) {
                PermissionsManagerDialog(
                    onDismiss = { showPermissionsDialog = false },
                    onRequestAllPermissions = { requestAllAppPermissions() }
                )
            }

            // CHOTI SCREEN: Download Dialog Modal with Video & Music Tabs + Quality Options
            selectedVideoForDownload?.let { video ->
                DownloadDialog(
                    video = video,
                    onDismiss = { viewModel.closeDownloadDialog() },
                    onConfirmDownload = { quality ->
                        viewModel.startDownload(video, quality)
                    },
                    onPlayOnline = {
                        viewModel.closeDownloadDialog()
                        viewModel.openPlayer(video)
                    }
                )
            }

            // Optional In-app Video Player Dialog
            selectedVideoForPlaying?.let { video ->
                VideoPlayerDialog(
                    video = video,
                    onDismiss = { viewModel.closePlayer() },
                    onDownloadClick = {
                        viewModel.closePlayer()
                        viewModel.openDownloadDialog(video)
                    }
                )
            }
        }
    }
}
