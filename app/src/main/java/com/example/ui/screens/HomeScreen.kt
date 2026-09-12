package com.example.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.components.TopHeaderBar

@Composable
fun HomeScreen(
    onNavigateToDownloads: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("YouTube") }
    var currentUrl by remember { mutableStateOf("https://m.youtube.com") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Navigation Bar
            TopHeaderBar(
                selectedCategory = selectedCategory,
                onCategorySelected = { name, url ->
                    selectedCategory = name
                    currentUrl = url
                },
                onMenuClick = onNavigateToSettings,
                onDownloadClick = onNavigateToDownloads
            )

            // Main Web View Engine
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                        loadUrl(currentUrl)
                    }
                },
                update = { webView ->
                    if (webView.url != currentUrl) {
                        webView.loadUrl(currentUrl)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Center-Right Floating Download Button
        FloatingActionButton(
            onClick = {
                // Downloader Action Trigger
            },
            containerColor = Color(0xFF2ECC71),
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download Video",
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
