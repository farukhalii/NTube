package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.GalleryDownloader
import com.example.model.QualityOption
import com.example.model.VideoItem
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NTubeRed
import com.example.ui.theme.TopDownloadBlue
import com.example.util.ActiveStreamTracker
import com.example.util.ResolvedMediaInfo
import kotlinx.coroutines.delay
import java.util.UUID

class QuickDownloadActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val rawText = intent?.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent?.dataString
            ?: ""

        val urlRegex = Regex("""https?://\S+""")
        val detectedUrl = urlRegex.find(rawText)?.value
            ?: if (rawText.startsWith("http")) rawText.trim() else ""

        if (detectedUrl.isEmpty()) {
            Toast.makeText(this, "No valid video link found to download", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            MyApplicationTheme {
                QuickDownloadOverlayScreen(
                    targetUrl = detectedUrl,
                    onDismiss = { finish() },
                    onStartDownload = { video, quality ->
                        GalleryDownloader.launchBackgroundGalleryDownload(
                            context = applicationContext,
                            video = video,
                            quality = quality
                        )
                        Toast.makeText(
                            applicationContext,
                            "Downloading to Phone Gallery: ${video.title.take(30)}...",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                )
            }
        }
    }
}

enum class QuickDownloadStep {
    AD,
    OPTIONS
}

@Composable
fun QuickDownloadOverlayScreen(
    targetUrl: String,
    onDismiss: () -> Unit,
    onStartDownload: (VideoItem, QualityOption) -> Unit
) {
    var step by remember { mutableStateOf(QuickDownloadStep.AD) }

    // Ad states (YouTube style: 15s total, skip available after 5s)
    var adRemainingSeconds by remember { mutableIntStateOf(15) }
    var adProgress by remember { mutableFloatStateOf(0f) }
    var canSkipAd by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }

    // Stream resolution states
    var resolvedInfo by remember { mutableStateOf<ResolvedMediaInfo?>(null) }
    var isResolving by remember { mutableStateOf(true) }

    // Format & Quality selection states
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Video, 1: Audio
    var selectedQualityIndex by remember { mutableIntStateOf(0) }

    // 1. Concurrently resolve media stream in background while Ad is playing
    LaunchedEffect(targetUrl) {
        isResolving = true
        try {
            val result = ActiveStreamTracker.resolveMediaMetadata(targetUrl)
            resolvedInfo = result
        } catch (_: Exception) {
            resolvedInfo = ResolvedMediaInfo(
                title = "Downloaded Video",
                author = "Social Video",
                thumbnailUrl = "",
                videoStreamUrl = targetUrl,
                audioStreamUrl = null,
                platformName = "Web"
            )
        } finally {
            isResolving = false
        }
    }

    // 2. Ad countdown timer (YouTube style)
    LaunchedEffect(step) {
        if (step == QuickDownloadStep.AD) {
            val totalSeconds = 15
            while (adRemainingSeconds > 0) {
                delay(1000)
                adRemainingSeconds--
                adProgress = (totalSeconds - adRemainingSeconds).toFloat() / totalSeconds.toFloat()
                if (adRemainingSeconds <= 10) {
                    canSkipAd = true
                }
            }
            // Once ad finishes completely, auto-advance
            step = QuickDownloadStep.OPTIONS
        }
    }

    // Semi-transparent scrim background that dims the current app (TikTok/YouTube)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Floating Box / Dialog Card ("dabe sa Wala page")
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* prevent dismiss when clicking inside dialog */ }
                ),
            shape = RoundedCornerShape(22.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                if (step == QuickDownloadStep.AD) {
                    // ==========================================
                    // PHASE 1: YOUTUBE STYLE VIDEO AD
                    // ==========================================
                    AdStageView(
                        adRemainingSeconds = adRemainingSeconds,
                        adProgress = adProgress,
                        canSkipAd = canSkipAd,
                        isMuted = isMuted,
                        onToggleMute = { isMuted = !isMuted },
                        onSkipAd = { step = QuickDownloadStep.OPTIONS },
                        onClose = onDismiss,
                        isResolving = isResolving,
                        platformName = resolvedInfo?.platformName ?: "Video"
                    )
                } else {
                    // ==========================================
                    // PHASE 2: QUALITY & FORMAT OPTIONS
                    // ==========================================
                    val mediaInfo = resolvedInfo ?: ResolvedMediaInfo(
                        title = "Shared Video",
                        author = "Video Creator",
                        thumbnailUrl = "",
                        videoStreamUrl = targetUrl,
                        audioStreamUrl = null,
                        platformName = "Video"
                    )

                    DownloadOptionsStageView(
                        mediaInfo = mediaInfo,
                        selectedTab = selectedTab,
                        onTabSelect = {
                            selectedTab = it
                            selectedQualityIndex = 0
                        },
                        selectedQualityIndex = selectedQualityIndex,
                        onSelectQuality = { selectedQualityIndex = it },
                        onCancel = onDismiss,
                        onConfirmDownload = { quality ->
                            val videoItem = VideoItem(
                                id = UUID.randomUUID().toString(),
                                title = mediaInfo.title,
                                channelName = mediaInfo.author,
                                channelAvatarUrl = mediaInfo.thumbnailUrl,
                                views = "1.2M",
                                uploadTime = "Just now",
                                duration = if (mediaInfo.durationSeconds > 0) "${mediaInfo.durationSeconds}s" else "0:45",
                                thumbnailUrl = mediaInfo.thumbnailUrl,
                                videoUrl = if (quality.isAudioOnly) {
                                    mediaInfo.audioStreamUrl ?: mediaInfo.videoStreamUrl ?: targetUrl
                                } else {
                                    mediaInfo.videoStreamUrl ?: targetUrl
                                },
                                platform = mediaInfo.platformName
                            )
                            onStartDownload(videoItem, quality)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdStageView(
    adRemainingSeconds: Int,
    adProgress: Float,
    canSkipAd: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onSkipAd: () -> Unit,
    onClose: () -> Unit,
    isResolving: Boolean,
    platformName: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // YouTube-style Ad Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // YouTube Yellow Ad Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFFFC107))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Ad",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "1 of 1 • Sponsored",
                    color = Color(0xFFB0B0B0),
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Ad Commercial Container (High-tech animated display)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                    )
                )
        ) {
            // Ad content banner
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "SpeedBoost Cloud 5G",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ultra-Fast Downloads & Unlimited Storage",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x66000000))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Mute",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Decorative Visual Graphic
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "100 MB/s Download Speed",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Zero Watermark • Original HD",
                            color = Color(0xFFA5B4FC),
                            fontSize = 11.sp
                        )
                    }
                }

                // Bottom bar inside Ad
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canSkipAd) {
                        Button(
                            onClick = onSkipAd,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xDD000000),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFCCCCCC)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "Skip Ad",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xAA000000))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            val secondsUntilSkip = (adRemainingSeconds - 10).coerceAtLeast(1)
                            Text(
                                text = "Skip in ${secondsUntilSkip}s",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // YouTube Yellow Ad Progress Line
            LinearProgressIndicator(
                progress = { adProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
                color = Color(0xFFFFC107),
                trackColor = Color(0x44FFFFFF)
            )
        }

        // Live Background Search Indicator ("jab add chal Rahi hogi to Mera app us wabsit ki video tlash ker ke")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E222B)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isResolving) {
                    CircularProgressIndicator(
                        color = NTubeRed,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Searching & resolving exact $platformName video in background...",
                        color = Color(0xFFD1D5DB),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "✓ Exact clean video stream ready! Skip ad to download.",
                        color = Color(0xFF22C55E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadOptionsStageView(
    mediaInfo: ResolvedMediaInfo,
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    selectedQualityIndex: Int,
    onSelectQuality: (Int) -> Unit,
    onCancel: () -> Unit,
    onConfirmDownload: (QualityOption) -> Unit
) {
    val isAudio = selectedTab == 1

    val videoQualities = remember(mediaInfo) {
        listOf(
            QualityOption(
                label = "1080p Full HD",
                resolution = "1920x1080 • Original 60fps",
                approximateSize = "${(mediaInfo.estimatedSizeBytes * 1.5 / 1024 / 1024).toInt().coerceAtLeast(18)} MB",
                sizeBytes = (mediaInfo.estimatedSizeBytes * 1.5).toLong(),
                isAudioOnly = false
            ),
            QualityOption(
                label = "720p HD",
                resolution = "1280x720 • Recommended",
                approximateSize = "${(mediaInfo.estimatedSizeBytes / 1024 / 1024).toInt().coerceAtLeast(10)} MB",
                sizeBytes = mediaInfo.estimatedSizeBytes,
                isAudioOnly = false
            ),
            QualityOption(
                label = "480p SD",
                resolution = "854x480 • Fast Data Saver",
                approximateSize = "${(mediaInfo.estimatedSizeBytes * 0.5 / 1024 / 1024).toInt().coerceAtLeast(5)} MB",
                sizeBytes = (mediaInfo.estimatedSizeBytes * 0.5).toLong(),
                isAudioOnly = false
            )
        )
    }

    val audioQualities = remember {
        listOf(
            QualityOption(
                label = "320 kbps High Quality",
                resolution = "HQ MP3 • Crystal Clear Audio",
                approximateSize = "6.2 MB",
                sizeBytes = 6_500_000L,
                isAudioOnly = true
            ),
            QualityOption(
                label = "128 kbps Standard",
                resolution = "Standard MP3 • Fast Download",
                approximateSize = "3.1 MB",
                sizeBytes = 3_250_000L,
                isAudioOnly = true
            )
        )
    }

    val currentQualities = if (isAudio) audioQualities else videoQualities
    val safeIndex = selectedQualityIndex.coerceIn(0, currentQualities.lastIndex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Download Options",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Platform: ${mediaInfo.platformName} • Clean Stream",
                    color = Color(0xFF22C55E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Video Thumbnail & Details Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1B202B),
            border = BorderStroke(1.dp, Color(0x22FFFFFF))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp, 60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = mediaInfo.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0x88000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaInfo.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mediaInfo.author,
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Format Tabs: Video (MP4) vs Music (MP3)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF171A21),
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = NTubeRed
                )
            },
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabSelect(0) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedTab == 0) NTubeRed else Color(0xFF9CA3AF)
                        )
                        Text(
                            "Video (MP4)",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            )

            Tab(
                selected = selectedTab == 1,
                onClick = { onTabSelect(1) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedTab == 1) NTubeRed else Color(0xFF9CA3AF)
                        )
                        Text(
                            "Music (MP3)",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            )
        }

        // Quality Options List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            currentQualities.forEachIndexed { index, option ->
                val isSelected = index == safeIndex
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectQuality(index) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF2C1E23) else Color(0xFF1A1D24),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) NTubeRed else Color(0x22FFFFFF)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectQuality(index) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = NTubeRed,
                                    unselectedColor = Color(0xFF71717A)
                                ),
                                modifier = Modifier.size(20.dp)
                            )

                            Column {
                                Text(
                                    text = option.label,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = option.resolution,
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = option.approximateSize,
                            color = if (isSelected) NTubeRed else Color(0xFFD1D5DB),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Action Buttons ("ok bton per Jase hi click kare to exat wohi video download ho ker sidhe galry main chali jaye")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF262A35),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(
                    text = "Cancel",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }

            Button(
                onClick = {
                    onConfirmDownload(currentQualities[safeIndex])
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NTubeRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.4f)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "OK • Download",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
