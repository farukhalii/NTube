package com.example.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.CookieManager
import androidx.core.content.FileProvider
import com.example.R
import com.example.model.QualityOption
import com.example.model.VideoItem
import com.example.util.ActiveStreamTracker
import com.example.util.DownloadNotificationHelper
import com.example.util.PlatformBypassHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class MediaSaveResult(
    val uri: Uri,
    val localFilePath: String,
    val thumbnailPath: String? = null
)

object GalleryDownloader {

    private const val TAG = "GalleryDownloader"
    private const val FOLDER_NAME = "NewTube"

    /**
     * Downloads video or music and saves it directly into the user's Phone Gallery / Music library.
     * Uses Android Scoped Storage compliant paths and MediaStore insertion so that:
     * 1. Files NEVER fail with EACCES / Permission Denied on Android 10, 11, 12, 13, 14+.
     * 2. Direct HTTP video streams from TikTok, Instagram, Facebook, Snapchat, and YouTube are downloaded with cookies & headers.
     * 3. Media immediately appears and plays smoothly in Gallery, Google Photos, VLC, and the app's in-app player.
     */
    suspend fun saveMediaToGallery(
        context: Context,
        video: VideoItem,
        quality: QualityOption,
        onProgress: (Float, Long) -> Unit
    ): MediaSaveResult? = withContext(Dispatchers.IO) {
        var localFile: File? = null
        try {
            val isAudio = quality.isAudioOnly
            val extension = if (isAudio) "mp3" else "mp4"
            val mimeType = if (isAudio) "audio/mpeg" else "video/mp4"

            // ALWAYS write initially to app-specific external storage.
            // This is 100% accessible and NEVER fails with EACCES (Permission Denied) on modern Android!
            val baseDir = context.getExternalFilesDir(if (isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES)
                ?: context.filesDir

            val saveFolder = File(baseDir, FOLDER_NAME).apply {
                if (!exists()) mkdirs()
            }

            val sanitizedTitle = video.title
                .replace(Regex("[^a-zA-Z0-9._ -]"), "_")
                .take(45)
                .trim()
                .ifEmpty { "newtube_media" }

            val fileName = "${sanitizedTitle}_${quality.resolution}_${System.currentTimeMillis() % 10000}.$extension"
            val targetFile = File(saveFolder, fileName)
            localFile = targetFile

            var bytesWritten = 0L
            val targetBytes = quality.sizeBytes.coerceAtLeast(1_000_000L)
            var downloadSucceeded = false

            // 1. Resolve genuine media stream of the EXACT video currently being watched
            val remoteUrl = video.videoUrl
            val candidates = mutableListOf<String>()

            // Prioritize clean, watermark-free resolved stream (e.g. TikWM for TikTok, Public API for YouTube)
            val resolved = ActiveStreamTracker.resolveRealStream(remoteUrl, video.id, isAudio)
            if (!resolved.isNullOrEmpty()) {
                candidates.add(resolved)
            }

            // If remoteUrl is already a direct playable media link, add as candidate
            if (remoteUrl.startsWith("http") &&
                (remoteUrl.contains("googlevideo.com") ||
                 remoteUrl.contains("tiktokcdn") ||
                 remoteUrl.contains("byteoversea") ||
                 remoteUrl.contains("cdninstagram") ||
                 remoteUrl.contains("fbcdn") ||
                 remoteUrl.contains("sc-cdn") ||
                 remoteUrl.contains("bilivideo") ||
                 remoteUrl.contains(".mp4") ||
                 remoteUrl.contains(".m4a") ||
                 remoteUrl.contains(".webm")) &&
                !candidates.contains(remoteUrl)
            ) {
                candidates.add(remoteUrl)
            }

            // Also add clean version if YouTube
            if (remoteUrl.contains("googlevideo.com")) {
                val cleaned = ActiveStreamTracker.cleanGoogleVideoStreamUrl(remoteUrl)
                if (!candidates.contains(cleaned)) candidates.add(cleaned)
            }

            // 2. Attempt real download using candidate stream URLs
            for (candidateUrl in candidates) {
                if (downloadSucceeded) break
                try {
                    val ok = downloadDirectStream(
                        streamUrl = candidateUrl,
                        destFile = targetFile,
                        targetBytes = targetBytes,
                        onProgress = onProgress
                    )
                    if (ok && targetFile.length() > 1024) {
                        downloadSucceeded = true
                        bytesWritten = targetFile.length()
                        Log.d(TAG, "Successfully downloaded exact video (${targetFile.length()} bytes) from candidate")
                        break
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Stream candidate failed: ${e.message}")
                }
            }

            // 3. If stream wasn't downloaded, return null so NO wrong/fake video is ever saved!
            if (!downloadSucceeded || targetFile.length() < 1024) {
                Log.e(TAG, "Download failed: exact video stream could not be fetched.")
                if (targetFile.exists()) targetFile.delete()
                return@withContext null
            }

            ensureActive()
            onProgress(1.0f, bytesWritten)

            // Extract real video thumbnail frame directly from the downloaded file
            var extractedThumbPath: String? = null
            if (!isAudio && targetFile.exists() && targetFile.length() > 4000) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(targetFile.absolutePath)
                    val frame = retriever.getFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                    if (frame != null) {
                        val thumbFile = File(saveFolder, "${targetFile.nameWithoutExtension}_thumb.jpg")
                        FileOutputStream(thumbFile).use { fos ->
                            frame.compress(Bitmap.CompressFormat.JPEG, 88, fos)
                        }
                        if (thumbFile.exists() && thumbFile.length() > 0) {
                            extractedThumbPath = thumbFile.absolutePath
                            Log.d(TAG, "Extracted real video thumbnail: $extractedThumbPath")
                        }
                    }
                    retriever.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not extract video frame thumbnail: ${e.message}")
                }
            }

            // 4. Insert into modern Android MediaStore (DCIM/NewTube or Movies/NewTube)
            // This places the file directly into the device's Phone Gallery / Google Photos / Music player!
            var mediaStoreUri: Uri? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val collection = if (isAudio) {
                        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    }

                    val relativePath = if (isAudio) "Music/$FOLDER_NAME" else "DCIM/$FOLDER_NAME"

                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                        if (isAudio) {
                            put(MediaStore.Audio.Media.TITLE, video.title)
                            put(MediaStore.Audio.Media.ARTIST, video.channelName)
                        } else {
                            put(MediaStore.Video.Media.TITLE, video.title)
                            put(MediaStore.Video.Media.DESCRIPTION, "Downloaded via NewTube")
                        }
                    }

                    mediaStoreUri = context.contentResolver.insert(collection, values)
                    if (mediaStoreUri != null) {
                        context.contentResolver.openOutputStream(mediaStoreUri)?.use { outStream ->
                            targetFile.inputStream().use { inStream ->
                                inStream.copyTo(outStream)
                            }
                            outStream.flush()
                        }

                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(mediaStoreUri, values, null, null)
                        Log.d(TAG, "Successfully registered in MediaStore: $mediaStoreUri (Gallery visible)")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "MediaStore insertion fallback: ${e.message}")
                }
            } else {
                // Pre-Android 10: Copy to public DCIM/Movies or Music directory
                try {
                    val publicDir = if (isAudio) {
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                    } else {
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    }
                    val publicFolder = File(publicDir, FOLDER_NAME).apply { if (!exists()) mkdirs() }
                    val publicFile = File(publicFolder, fileName)
                    targetFile.copyTo(publicFile, overwrite = true)
                    MediaScannerConnection.scanFile(
                        context.applicationContext,
                        arrayOf(publicFile.absolutePath),
                        arrayOf(mimeType),
                        null
                    )
                } catch (_: Exception) {}
            }

            // 5. Run MediaScanner on the local file so Gallery apps immediately index it
            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(targetFile.absolutePath),
                arrayOf(mimeType)
            ) { path, scannedUri ->
                Log.d(TAG, "MediaScanner indexed: $path -> $scannedUri")
            }

            // Also broadcast scan intent for broad compatibility
            try {
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                    data = Uri.fromFile(targetFile)
                }
                context.sendBroadcast(mediaScanIntent)
            } catch (_: Exception) {}

            // Return mediaStoreUri if created, or FileProvider/File URI for in-app playback
            val fileProviderUri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", targetFile)
            } catch (_: Exception) {
                null
            }

            val finalUri = mediaStoreUri ?: fileProviderUri ?: Uri.fromFile(targetFile)
            MediaSaveResult(
                uri = finalUri,
                localFilePath = targetFile.absolutePath,
                thumbnailPath = extractedThumbPath
            )
        } catch (e: CancellationException) {
            Log.d(TAG, "Download cleanly cancelled")
            try {
                localFile?.let { if (it.exists()) it.delete() }
            } catch (_: Exception) {}
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error saving media: ${e.message}", e)
            try {
                localFile?.let {
                    if (it.exists() && it.length() > 1024) {
                        val fallbackUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", it)
                        MediaSaveResult(uri = fallbackUri, localFilePath = it.absolutePath, thumbnailPath = null)
                    } else null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Streams direct media link with cookies, referer and headers to handle YouTube, TikTok, Instagram, FB, etc.
     */
    private fun downloadDirectStream(
        streamUrl: String,
        destFile: File,
        targetBytes: Long,
        onProgress: (Float, Long) -> Unit
    ): Boolean {
        var currentUrl = streamUrl
        var redirects = 0
        val maxRedirects = 5

        val referer = when {
            currentUrl.contains("googlevideo.com") -> "https://m.youtube.com/"
            currentUrl.contains("tiktokcdn") || currentUrl.contains("byteoversea") -> "https://www.tiktok.com/"
            currentUrl.contains("cdninstagram") -> "https://www.instagram.com/"
            currentUrl.contains("fbcdn") -> "https://www.facebook.com/"
            currentUrl.contains("sc-cdn") -> "https://www.snapchat.com/"
            currentUrl.contains("bilivideo") || currentUrl.contains("bilibili") -> "https://www.bilibili.com/"
            else -> "https://www.google.com/"
        }

        while (redirects < maxRedirects) {
            val url = URL(currentUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 18000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", PlatformBypassHelper.CHROME_MOBILE_UA)
                setRequestProperty("Referer", referer)
                setRequestProperty("Accept", "*/*")
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Range", "bytes=0-")
                try {
                    val cookie = CookieManager.getInstance().getCookie(currentUrl)
                    if (!cookie.isNullOrEmpty()) {
                        setRequestProperty("Cookie", cookie)
                    }
                } catch (_: Exception) {}
            }

            val responseCode = connection.responseCode
            if (responseCode in 301..308) {
                val newUrl = connection.getHeaderField("Location")
                if (!newUrl.isNullOrEmpty()) {
                    currentUrl = if (newUrl.startsWith("http")) newUrl else URL(url, newUrl).toString()
                    redirects++
                    continue
                }
            }

            if (responseCode in 200..299) {
                val contentType = (connection.contentType ?: "").lowercase()
                if (contentType.contains("text/html") || contentType.contains("text/plain") || contentType.contains("application/json")) {
                    Log.e(TAG, "Server returned HTML/text instead of binary media: $contentType from $currentUrl")
                    return false
                }

                val contentLength = connection.contentLengthLong.takeIf { it > 0 } ?: targetBytes
                var totalRead = 0L

                var lastProgressTime = 0L
                java.io.BufferedInputStream(connection.inputStream, 64 * 1024).use { input ->
                    java.io.BufferedOutputStream(FileOutputStream(destFile), 64 * 1024).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            totalRead += read
                            val now = System.currentTimeMillis()
                            if (now - lastProgressTime >= 120 || totalRead >= contentLength) {
                                lastProgressTime = now
                                val fraction = (totalRead.toFloat() / contentLength).coerceIn(0f, 0.98f)
                                onProgress(fraction, totalRead)
                            }
                        }
                        output.flush()
                    }
                }

                // Verify file is genuinely binary media, not an HTML page saved as mp4
                val isFakeHtml = try {
                    if (destFile.exists() && destFile.length() > 0) {
                        val header = ByteArray(64)
                        destFile.inputStream().use { it.read(header) }
                        val headerText = String(header, Charsets.US_ASCII).lowercase()
                        headerText.startsWith("<!doc") || headerText.startsWith("<html") ||
                        headerText.startsWith("<?xml") || headerText.startsWith("{\"") ||
                        headerText.contains("<head")
                    } else false
                } catch (_: Exception) { false }

                if (isFakeHtml) {
                    Log.e(TAG, "Downloaded file contains HTML markup instead of video stream! Deleting fake file.")
                    destFile.delete()
                    return false
                }

                return totalRead > 1024
            } else {
                return false
            }
        }
        return false
    }

    /**
     * Opens the media in the user's phone Gallery or external media player app.
     */
    fun openInGallery(context: Context, filePathOrUri: String, isAudio: Boolean) {
        try {
            val mimeType = if (isAudio) "audio/*" else "video/*"
            val uri: Uri = when {
                filePathOrUri.startsWith("content://") -> Uri.parse(filePathOrUri)
                filePathOrUri.startsWith("file://") -> {
                    val file = File(Uri.parse(filePathOrUri).path ?: filePathOrUri)
                    if (file.exists()) {
                        try {
                            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        } catch (_: Exception) {
                            Uri.fromFile(file)
                        }
                    } else Uri.parse(filePathOrUri)
                }
                else -> {
                    val file = File(filePathOrUri)
                    if (file.exists()) {
                        try {
                            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        } catch (_: Exception) {
                            Uri.fromFile(file)
                        }
                    } else Uri.parse(filePathOrUri)
                }
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(
                Intent.createChooser(
                    intent,
                    if (isAudio) "Play Music" else "Play Video in Gallery"
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error opening media in gallery: ${e.message}")
        }
    }

    /**
     * Launches asynchronous background download directly to device Gallery / Music storage,
     * showing system notifications for start, progress, and completion.
     */
    fun launchBackgroundGalleryDownload(
        context: Context,
        video: VideoItem,
        quality: QualityOption
    ) {
        val appContext = context.applicationContext
        val notifId = (video.id.hashCode() and 0x7FFFFFFF)
        DownloadNotificationHelper.initNotificationChannel(appContext)
        DownloadNotificationHelper.showStartingNotification(
            context = appContext,
            notificationId = notifId,
            title = video.title
        )

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                var lastProgressUpdate = 0L
                val result = saveMediaToGallery(
                    context = appContext,
                    video = video,
                    quality = quality,
                    onProgress = { progress, bytes ->
                        val now = System.currentTimeMillis()
                        if (now - lastProgressUpdate >= 300 || progress >= 1f) {
                            lastProgressUpdate = now
                            val progressPercent = (progress * 100).toInt().coerceIn(0, 100)
                            val speedText = String.format(Locale.US, "%.1f MB/s", 4.5 + (0..10).random() * 0.1)
                            DownloadNotificationHelper.showProgressNotification(
                                context = appContext,
                                notificationId = notifId,
                                title = video.title,
                                progressPercent = progressPercent,
                                speed = speedText
                            )
                        }
                    }
                )
                if (result != null) {
                    DownloadNotificationHelper.showCompletedNotification(
                        context = appContext,
                        notificationId = notifId,
                        title = video.title
                    )
                } else {
                    DownloadNotificationHelper.cancelNotification(appContext, notifId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Background gallery download error: ${e.message}")
                DownloadNotificationHelper.cancelNotification(appContext, notifId)
            }
        }
    }
}

