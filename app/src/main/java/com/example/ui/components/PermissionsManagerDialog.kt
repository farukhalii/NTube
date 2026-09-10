package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NTubeRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PermissionsManagerDialog(
    onDismiss: () -> Unit,
    onRequestAllPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Check statuses
    val hasNotifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else true

    val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED

    val hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x2A1A73E8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = TopDownloadBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "App Permissions (Ijazat)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "New Tube ko behtareen tareeqe se kaam karne ke liye darj zail permissions ki zaroorat hai:",
                    color = TextSecondary,
                    fontSize = 12.5.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Files & Storage Permission Item
                PermissionStatusItem(
                    icon = Icons.Default.Folder,
                    title = "Files & Storage",
                    subtitle = "Videos ko phone storage aur download folder me save karne ke liye",
                    isGranted = hasStoragePermission
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Gallery & Media Permission Item
                PermissionStatusItem(
                    icon = Icons.Default.PhotoLibrary,
                    title = "Gallery (Photos & Media)",
                    subtitle = "Downloaded videos aur music ko seedha phone Gallery me dikhane ke liye",
                    isGranted = hasStoragePermission
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Mic Permission Item
                PermissionStatusItem(
                    icon = Icons.Default.Mic,
                    title = "Mic (Microphone)",
                    subtitle = "Voice search aur audio recording features ke liye",
                    isGranted = hasMicPermission
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Notifications Permission Item
                PermissionStatusItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Video download shuru hone, progress aur mukammal hone ki notification ke liye",
                    isGranted = hasNotifPermission
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onRequestAllPermissions()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = TopDownloadBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Grant Permissions (Ijazat Dain)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    // Open Android App Settings if user wants to toggle directly
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // ignore
                    }
                    onDismiss()
                }
            ) {
                Text(
                    text = "Phone Settings",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        },
        modifier = modifier
    )
}

@Composable
fun PermissionStatusItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) Color(0x2A34A853) else Color(0x261A73E8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF34A853) else TopDownloadBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (isGranted) {
                        Text(
                            text = "Granted",
                            color = Color(0xFF34A853),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 13.sp
                )
            }

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = Color(0xFF34A853),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
