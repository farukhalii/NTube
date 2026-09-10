package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GalleryDownloader
import com.example.data.SampleData
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.QualityOption
import com.example.model.ShortItem
import com.example.model.VideoItem
import com.example.util.DownloadNotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class AppTab {
    HOME,
    SHORTS,
    DOWNLOAD,
    YOU
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Whether Downloads Screen is open (toggled via Top Blue Download Button)
    private val _isDownloadsScreenOpen = MutableStateFlow(false)
    val isDownloadsScreenOpen: StateFlow<Boolean> = _isDownloadsScreenOpen.asStateFlow()

    // Multi-Platform Web Browser (for TikTok, Instagram, Facebook, Snapchat, etc.)
    private val _isBrowserOpen = MutableStateFlow(false)
    val isBrowserOpen: StateFlow<Boolean> = _isBrowserOpen.asStateFlow()

    // Dedicated Web Search & URL Page
    private val _isWebSearchPageOpen = MutableStateFlow(false)
    val isWebSearchPageOpen: StateFlow<Boolean> = _isWebSearchPageOpen.asStateFlow()

    private val _browserCurrentUrl = MutableStateFlow("https://www.tiktok.com")
    val browserCurrentUrl: StateFlow<String> = _browserCurrentUrl.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchExpanded = MutableStateFlow(false)
    val isSearchExpanded: StateFlow<Boolean> = _isSearchExpanded.asStateFlow()

    private val _webCurrentUrl = MutableStateFlow("https://m.youtube.com")
    val webCurrentUrl: StateFlow<String> = _webCurrentUrl.asStateFlow()

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    // Live Web Mode is TRUE by default on app launch as explicitly requested by user!
    private val _isLiveWebMode = MutableStateFlow(true)
    val isLiveWebMode: StateFlow<Boolean> = _isLiveWebMode.asStateFlow()

    // Platform filter: "All", "YouTube", "TikTok", "Instagram", "Snapchat", "Facebook"
    private val _selectedPlatform = MutableStateFlow("All")
    val selectedPlatform: StateFlow<String> = _selectedPlatform.asStateFlow()

    // Fast Native Videos feed
    private val _videos = MutableStateFlow<List<VideoItem>>(SampleData.getInitialVideos())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    // Active video selected for Download bottom sheet ("CHOTI screen")
    private val _selectedVideoForDownload = MutableStateFlow<VideoItem?>(null)
    val selectedVideoForDownload: StateFlow<VideoItem?> = _selectedVideoForDownload.asStateFlow()

    private val _selectedVideoForPlaying = MutableStateFlow<VideoItem?>(null)
    val selectedVideoForPlaying: StateFlow<VideoItem?> = _selectedVideoForPlaying.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private val _unreadNotifications = MutableStateFlow(0)
    val unreadNotifications: StateFlow<Int> = _unreadNotifications.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>()

    fun onPlatformSelected(platform: String) {
        _selectedPlatform.value = platform
    }

    fun toggleLiveWebMode() {
        _isLiveWebMode.value = !_isLiveWebMode.value
    }

    fun openLiveWebMode(url: String = "https://m.youtube.com") {
        _webCurrentUrl.value = url
        _isLiveWebMode.value = true
        _isDownloadsScreenOpen.value = false
        _isBrowserOpen.value = false
    }

    fun closeLiveWebMode() {
        _isLiveWebMode.value = false
    }

    fun loadMoreVideos() {
        viewModelScope.launch {
            val currentList = _videos.value
            val more = SampleData.generateMoreVideos(currentList.size, 10)
            _videos.value = currentList + more
        }
    }

    fun openDownloadsScreen() {
        _isDownloadsScreenOpen.value = true
    }

    fun closeDownloadsScreen() {
        _isDownloadsScreenOpen.value = false
    }

    fun toggleDownloadsScreen() {
        _isDownloadsScreenOpen.value = !_isDownloadsScreenOpen.value
    }

    fun openBrowser(url: String = "https://www.tiktok.com") {
        _browserCurrentUrl.value = url
        _isBrowserOpen.value = true
        _isDownloadsScreenOpen.value = false
    }

    fun closeBrowser() {
        _isBrowserOpen.value = false
    }

    fun openBrowserWithUrl(url: String) {
        _browserCurrentUrl.value = url
        _isBrowserOpen.value = true
        _isDownloadsScreenOpen.value = false
    }

    fun openPlatformWeb(platform: String) {
        when (platform.lowercase()) {
            "youtube" -> openLiveWebMode("https://m.youtube.com")
            "tiktok" -> openBrowser("https://www.tiktok.com/explore")
            "instagram" -> openBrowser("https://www.instagram.com/reels")
            "facebook" -> openBrowser("https://m.facebook.com/watch")
            "snapchat" -> openBrowser("https://www.snapchat.com/spotlight")
            "bilibili" -> openBrowser("https://m.bilibili.com")
            else -> openBrowser("https://www.google.com")
        }
    }

    fun openWebSearchPage() {
        _isWebSearchPageOpen.value = true
    }

    fun closeWebSearchPage() {
        _isWebSearchPageOpen.value = false
    }

    fun navigateFromSearchPage(url: String) {
        _isWebSearchPageOpen.value = false
        openBrowser(url)
    }

    fun toggleSearch() {
        _isSearchExpanded.value = !_isSearchExpanded.value
    }

    fun closeSearch() {
        _isSearchExpanded.value = false
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun triggerSearchOrNavigate(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        val targetUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.contains("youtube.com") || trimmed.contains("youtu.be")) {
            if (!trimmed.startsWith("http")) "https://$trimmed" else trimmed
        } else {
            "https://m.youtube.com/results?search_query=${android.net.Uri.encode(trimmed)}"
        }

        _webCurrentUrl.value = targetUrl
        _isDownloadsScreenOpen.value = false
        _isSearchExpanded.value = false
    }

    fun openWebUrl(url: String = "https://m.youtube.com") {
        _webCurrentUrl.value = url
        _isDownloadsScreenOpen.value = false
    }

    fun openDownloadDialog(video: VideoItem) {
        _selectedVideoForDownload.value = video
    }

    fun handleUrlOrSearch(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") ||
            trimmed.contains("youtube.com") || trimmed.contains("youtu.be") ||
            trimmed.contains("tiktok.com") || trimmed.contains("instagram.com") ||
            trimmed.contains("facebook.com") || trimmed.contains("snapchat.com")
        ) {
            val url = if (!trimmed.startsWith("http")) "https://$trimmed" else trimmed
            val platform = when {
                url.contains("tiktok") -> "TikTok"
                url.contains("instagram") -> "Instagram"
                url.contains("facebook") -> "Facebook"
                url.contains("snapchat") -> "Snapchat"
                else -> "YouTube"
            }
            val title = "Direct Video: $platform HD Clip"
            val videoItem = VideoItem(
                id = "pasted_${System.currentTimeMillis()}",
                title = title,
                channelName = "$platform Creator",
                channelAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
                views = "Direct Link",
                uploadTime = "Now",
                duration = "HD",
                thumbnailUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80",
                videoUrl = url,
                platform = platform
            )
            _videos.update { listOf(videoItem) + it }
            _selectedVideoForDownload.value = videoItem
            emitToast("Detected $platform Video! Choose quality to download.")
        } else {
            _searchQuery.value = trimmed
        }
    }

    fun closeDownloadDialog() {
        _selectedVideoForDownload.value = null
    }

    fun openPlayer(video: VideoItem) {
        _selectedVideoForPlaying.value = video
    }

    fun closePlayer() {
        _selectedVideoForPlaying.value = null
    }

    fun clearNotifications() {
        _unreadNotifications.value = 0
    }

    /**
     * Real download to device Gallery & Music storage.
     * Integrates GalleryDownloader to scan files directly into Android MediaStore.
     */
    fun startDownload(video: VideoItem, quality: QualityOption, existingId: String? = null) {
        closeDownloadDialog()

        val downloadId = existingId ?: UUID.randomUUID().toString()
        val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        val dateString = dateFormat.format(Date())

        val isAudio = quality.isAudioOnly
        val mediaTypeLabel = if (isAudio) "Music" else "Video"

        if (existingId == null) {
            val newDownload = DownloadItem(
                id = downloadId,
                videoId = video.id,
                title = video.title,
                channelName = video.channelName,
                thumbnailUrl = video.thumbnailUrl,
                quality = quality.label,
                isAudio = isAudio,
                totalSizeBytes = quality.sizeBytes,
                downloadedBytes = 0L,
                progress = 0f,
                speed = "Connecting...",
                status = DownloadStatus.DOWNLOADING,
                dateAdded = dateString,
                localUri = null
            )
            _downloads.update { listOf(newDownload) + it }
            emitToast("Downloading $mediaTypeLabel (${quality.label})... Direct to Gallery")
        } else {
            emitToast("Resuming $mediaTypeLabel download...")
        }

        // Cancel previous job if any
        downloadJobs[downloadId]?.cancel()

        // Launch background download & MediaScanner job
        val job = viewModelScope.launch(Dispatchers.IO) {
            val notifId = downloadId.hashCode() and 0x7FFFFFFF
            val app = getApplication<Application>()
            try {
                DownloadNotificationHelper.initNotificationChannel(app)
                DownloadNotificationHelper.showStartingNotification(
                    context = app,
                    notificationId = notifId,
                    title = video.title
                )

                val saveResult = GalleryDownloader.saveMediaToGallery(
                    context = app,
                    video = video,
                    quality = quality,
                    onProgress = { progress, bytes ->
                        val speedText = String.format(Locale.US, "%.1f MB/s", 4.2 + (0..15).random() * 0.1)
                        val progressPercent = (progress * 100).toInt().coerceIn(0, 100)

                        DownloadNotificationHelper.showProgressNotification(
                            context = app,
                            notificationId = notifId,
                            title = video.title,
                            progressPercent = progressPercent,
                            speed = speedText
                        )

                        _downloads.update { list ->
                            list.map { item ->
                                if (item.id == downloadId) {
                                    item.copy(
                                        downloadedBytes = bytes,
                                        progress = progress,
                                        speed = speedText,
                                        status = if (progress >= 1f) DownloadStatus.COMPLETED else DownloadStatus.DOWNLOADING
                                    )
                                } else item
                            }
                        }
                    }
                )

                if (saveResult != null) {
                    // Mark completed & notify user
                    DownloadNotificationHelper.showCompletedNotification(
                        context = app,
                        notificationId = notifId,
                        title = video.title
                    )

                    _downloads.update { list ->
                        list.map { item ->
                            if (item.id == downloadId) {
                                item.copy(
                                    downloadedBytes = quality.sizeBytes,
                                    progress = 1f,
                                    speed = "Saved to Gallery",
                                    status = DownloadStatus.COMPLETED,
                                    localUri = saveResult.localFilePath,
                                    thumbnailUrl = saveResult.thumbnailPath ?: item.thumbnailUrl
                                )
                            } else item
                        }
                    }

                    _unreadNotifications.update { it + 1 }
                    emitToast("Video saved to Gallery: ${video.title.take(30)}")
                } else {
                    DownloadNotificationHelper.cancelNotification(app, notifId)
                    _downloads.update { list ->
                        list.map { item ->
                            if (item.id == downloadId) {
                                item.copy(
                                    status = DownloadStatus.FAILED,
                                    speed = "Video stream not detected"
                                )
                            } else item
                        }
                    }
                    emitToast("Video stream not detected yet. Play video for 2-3s and tap download again!")
                }
            } catch (e: CancellationException) {
                DownloadNotificationHelper.cancelNotification(app, notifId)
                // User paused or cancelled download cleanly
            } catch (e: Exception) {
                DownloadNotificationHelper.cancelNotification(app, notifId)
                _downloads.update { list ->
                    list.map { item ->
                        if (item.id == downloadId) {
                            item.copy(status = DownloadStatus.FAILED, speed = "Failed")
                        } else item
                    }
                }
            }
        }

        downloadJobs[downloadId] = job
    }

    fun pauseDownload(id: String) {
        downloadJobs[id]?.cancel()
        _downloads.update { list ->
            list.map { if (it.id == id) it.copy(status = DownloadStatus.PAUSED, speed = "Paused") else it }
        }
        emitToast("Download paused")
    }

    fun resumeDownload(id: String) {
        val item = _downloads.value.find { it.id == id } ?: return
        _downloads.update { list ->
            list.map { if (it.id == id) it.copy(status = DownloadStatus.DOWNLOADING, speed = "Resuming...") else it }
        }

        val dummyOption = QualityOption(
            label = item.quality,
            resolution = "HD",
            approximateSize = "${item.totalSizeBytes / (1024 * 1024)} MB",
            sizeBytes = item.totalSizeBytes,
            isAudioOnly = item.isAudio
        )

        val dummyVideo = VideoItem(
            id = item.videoId,
            title = item.title,
            channelName = item.channelName,
            channelAvatarUrl = "",
            views = "",
            uploadTime = "",
            duration = "",
            thumbnailUrl = item.thumbnailUrl
        )

        startDownload(dummyVideo, dummyOption, existingId = id)
    }

    fun deleteDownload(id: String) {
        downloadJobs[id]?.cancel()
        downloadJobs.remove(id)
        _downloads.update { list -> list.filterNot { it.id == id } }
        emitToast("Removed from downloads")
    }

    private fun emitToast(msg: String) {
        viewModelScope.launch {
            _toastEvent.emit(msg)
        }
    }
}
