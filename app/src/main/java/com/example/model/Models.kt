package com.example.model

enum class DownloadStatus {
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

data class QualityOption(
    val label: String,
    val resolution: String,
    val approximateSize: String,
    val sizeBytes: Long,
    val isAudioOnly: Boolean = false
)

data class VideoItem(
    val id: String,
    val title: String,
    val channelName: String,
    val channelAvatarUrl: String,
    val channelAvatarColor: Long = 0xFFE50914,
    val views: String,
    val uploadTime: String,
    val duration: String,
    val thumbnailUrl: String,
    val centerPlayTitle: String? = null,
    val videoUrl: String = "https://www.youtube.com/watch?v=sample",
    val description: String = "",
    val platform: String = "YouTube"
)

data class ShortItem(
    val id: String,
    val title: String,
    val channelName: String,
    val channelAvatarUrl: String,
    val likesCount: String,
    val commentsCount: String,
    val videoUrl: String,
    val audioTitle: String,
    val tags: List<String> = listOf("#shorts", "#trending", "#viral"),
    val gradientColors: List<Long> = listOf(0xFF8A2387, 0xFFE94057, 0xFFF27121)
)

data class DownloadItem(
    val id: String,
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val quality: String,
    val isAudio: Boolean,
    val totalSizeBytes: Long,
    var downloadedBytes: Long,
    var progress: Float, // 0.0 to 1.0
    var speed: String,
    var status: DownloadStatus,
    val dateAdded: String,
    val localUri: String? = null
)
