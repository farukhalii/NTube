package com.example.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import coil.compose.AsyncImage
import com.example.data.GalleryDownloader
import com.example.model.VideoItem
import com.example.util.ActiveStreamTracker
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NTubeRed
import com.example.ui.theme.TopDownloadBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
fun VideoPlayerDialog(
    video: VideoItem,
    onDismiss: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val context = LocalContext.current
    val isAudio = video.description == "audio" ||
            video.channelName.contains("Music", ignoreCase = true) ||
            video.title.contains("mp3", ignoreCase = true) ||
            video.videoUrl.endsWith(".mp3", ignoreCase = true)

    var resolvedUri by remember { mutableStateOf<Uri?>(null) }
    var isResolvingStream by remember { mutableStateOf(true) }
    var streamError by remember { mutableStateOf<String?>(null) }

    // Resolve real playable stream URL asynchronously
    LaunchedEffect(video.videoUrl, video.id) {
        isResolvingStream = true
        streamError = null
        val url = video.videoUrl
        try {
            when {
                url.startsWith("content://") || url.startsWith("file://") -> {
                    resolvedUri = Uri.parse(url)
                    isResolvingStream = false
                }
                url.isNotEmpty() && File(url).exists() -> {
                    resolvedUri = Uri.fromFile(File(url))
                    isResolvingStream = false
                }
                url.startsWith("http") && (
                    url.contains(".mp4") || url.contains(".m4a") ||
                    url.contains("tiktokcdn") || url.contains("byteoversea") ||
                    url.contains("googlevideo.com") || url.contains("cdninstagram") ||
                    url.contains("fbcdn") || url.contains("bilivideo")
                ) -> {
                    resolvedUri = Uri.parse(url)
                    isResolvingStream = false
                }
                else -> {
                    val stream = withContext(Dispatchers.IO) {
                        ActiveStreamTracker.resolveRealStream(url, video.id, isAudio)
                    }
                    if (!stream.isNullOrEmpty()) {
                        resolvedUri = Uri.parse(stream)
                        isResolvingStream = false
                    } else if (url.startsWith("http")) {
                        resolvedUri = Uri.parse(url)
                        isResolvingStream = false
                    } else {
                        isResolvingStream = false
                        streamError = "Video stream not directly playable. Tap Download to save HD Video to Gallery!"
                    }
                }
            }
        } catch (e: Exception) {
            isResolvingStream = false
            streamError = "Failed to load stream: ${e.message}"
        }
    }

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(1000) }
    var showControls by remember { mutableStateOf(true) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var sliderProgress by remember { mutableFloatStateOf(0f) }

    // Vinyl rotation animation for audio
    val infiniteTransition = rememberInfiniteTransition(label = "music_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Periodic progress update
    LaunchedEffect(isPlaying, isUserSeeking) {
        while (isPlaying && !isUserSeeking) {
            delay(500)
            videoViewRef?.let { vv ->
                try {
                    if (vv.isPlaying) {
                        currentPositionMs = vv.currentPosition
                        val dur = vv.duration
                        if (dur > 0) {
                            totalDurationMs = dur
                            sliderProgress = (currentPositionMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // Auto-hide controls after 3.5 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3500)
            showControls = false
        }
    }

    // Cleanup video view on dialog close
    DisposableEffect(Unit) {
        onDispose {
            try {
                videoViewRef?.stopPlayback()
            } catch (_: Exception) {}
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top control bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xE6000000))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Player",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = video.title,
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isAudio) "Playing in App (Music / MP3)" else "Playing in App (Video)",
                            color = if (isAudio) TopDownloadBlue else NTubeRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Open in phone Gallery button
                    IconButton(
                        onClick = {
                            GalleryDownloader.openInGallery(
                                context = context,
                                filePathOrUri = video.videoUrl.ifEmpty { resolvedUri?.toString().orEmpty() },
                                isAudio = isAudio
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in Phone Gallery",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Download button (if needed)
                    IconButton(onClick = onDownloadClick) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Video",
                            tint = TopDownloadBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Interactive Video & Audio Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0A0A0A))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showControls = !showControls
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isAudio) {
                        // Music Playing Art View
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            // Spinning Album Art
                            Box(
                                modifier = Modifier
                                    .size(190.dp)
                                    .rotate(if (isPlaying) rotationAngle else 0f)
                                    .clip(CircleShape)
                                    .background(DarkSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = video.thumbnailUrl,
                                    contentDescription = video.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Center hole
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xDD000000))
                                )
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = TopDownloadBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = video.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = video.channelName,
                                color = Color(0xFFB0B0B0),
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Native Android VideoView for actual, real video & audio hardware playback
                    if (resolvedUri != null) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        if (isAudio) 1 else ViewGroup.LayoutParams.WRAP_CONTENT
                                    )
                                    resolvedUri?.let { setVideoURI(it) }
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        totalDurationMs = mp.duration.coerceAtLeast(1000)
                                        mp.start()
                                        isPlaying = true
                                    }
                                    setOnErrorListener { _, _, _ ->
                                        streamError = "Online playback restricted. Tap Download to save HD Video to Gallery!"
                                        true
                                    }
                                    setOnCompletionListener {
                                        isPlaying = false
                                    }
                                    videoViewRef = this
                                }
                            },
                            update = { view ->
                                try {
                                    resolvedUri?.let { uri ->
                                        view.setVideoURI(uri)
                                        view.start()
                                        isPlaying = true
                                    }
                                } catch (_: Exception) {}
                            },
                            modifier = if (isAudio) Modifier.size(1.dp) else Modifier.fillMaxSize()
                        )
                    }

                    // Loading overlay while resolving exact stream
                    if (isResolvingStream) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xCC000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = NTubeRed, modifier = Modifier.size(40.dp))
                                Text(
                                    text = "Connecting to exact HD stream...",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Error overlay if stream failed
                    if (streamError != null && !isResolvingStream) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xEE111111))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                AsyncImage(
                                    model = video.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(140.dp, 80.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    text = streamError ?: "Unable to stream online",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Button(
                                    onClick = onDownloadClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = NTubeRed),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Download to Gallery", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Overlay Controls
                    if (showControls) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0x55000000),
                                            Color(0x11000000),
                                            Color(0x88000000)
                                        )
                                    )
                                )
                        ) {
                            // Center Playback Buttons (Rewind 10, Play/Pause, Forward 10)
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        videoViewRef?.let { vv ->
                                            val newPos = (vv.currentPosition - 10000).coerceAtLeast(0)
                                            vv.seekTo(newPos)
                                            currentPositionMs = newPos
                                        }
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(Color(0x88000000), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Replay10,
                                        contentDescription = "Rewind 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        videoViewRef?.let { vv ->
                                            if (vv.isPlaying) {
                                                vv.pause()
                                                isPlaying = false
                                            } else {
                                                vv.start()
                                                isPlaying = true
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(68.dp)
                                        .background(TopDownloadBlue, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        videoViewRef?.let { vv ->
                                            val newPos = (vv.currentPosition + 10000).coerceAtMost(vv.duration)
                                            vv.seekTo(newPos)
                                            currentPositionMs = newPos
                                        }
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(Color(0x88000000), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Forward10,
                                        contentDescription = "Forward 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            // Bottom seek bar and timestamps
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color(0xCC000000))
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Slider(
                                    value = sliderProgress,
                                    onValueChange = { frac ->
                                        isUserSeeking = true
                                        sliderProgress = frac
                                    },
                                    onValueChangeFinished = {
                                        isUserSeeking = false
                                        videoViewRef?.let { vv ->
                                            val targetSeekMs = (sliderProgress * totalDurationMs).toInt()
                                            vv.seekTo(targetSeekMs)
                                            currentPositionMs = targetSeekMs
                                        }
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = TopDownloadBlue,
                                        activeTrackColor = TopDownloadBlue,
                                        inactiveTrackColor = Color(0x66FFFFFF)
                                    ),
                                    modifier = Modifier.height(24.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatTimeMs(currentPositionMs),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Text(
                                        text = formatTimeMs(totalDurationMs),
                                        color = Color(0xFFCCCCCC),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimeMs(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
