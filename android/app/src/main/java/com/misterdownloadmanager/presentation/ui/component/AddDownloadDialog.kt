package com.misterdownloadmanager.presentation.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.misterdownloadmanager.R
import com.misterdownloadmanager.util.FileUtils
import java.io.File

@Composable
fun AddDownloadDialog(
    defaultDir: String,
    initialUrl: String = "",
    onDismiss: () -> Unit,
    onConfirm: (url: String, dest: String) -> Unit,
    onConfirmPlaylist: ((url: String, destDir: String, title: String?) -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    var url by remember { mutableStateOf(initialUrl) }
    var fileName by remember { mutableStateOf("") }
    var isCustomFileName by remember { mutableStateOf(false) }

    fun updateUrl(newUrl: String) {
        url = newUrl
        if (!isCustomFileName) {
            val detected = FileUtils.extractFileNameFromUrl(newUrl)
            fileName = detected
        }
    }

    LaunchedEffect(initialUrl) {
        if (initialUrl.isNotBlank()) {
            updateUrl(initialUrl)
        }
    }

    val isPlaylist = remember(url) {
        val u = url.trim().lowercase()
        u.contains("list=") || u.contains("/playlist") || u.endsWith(".m3u8") || u.endsWith(".m3u")
    }

    val isValidScheme = remember(url) {
        val trimmed = url.trim()
        trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaylist) Icons.AutoMirrored.Filled.PlaylistPlay else Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = if (isPlaylist) stringResource(R.string.playlist_dialog_title) else stringResource(R.string.dialog_new_download),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { updateUrl(it) },
                    label = { Text(stringResource(R.string.dialog_url_label)) },
                    placeholder = { Text("https://example.com/file.mp4") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = if (isValidScheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (url.isNotBlank()) {
                                IconButton(onClick = { updateUrl("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                            IconButton(onClick = {
                                clipboardManager.getText()?.text?.let { clipText ->
                                    val candidate = clipText.toString().trim()
                                    // Extract url from text if text contains URL
                                    val extracted = FileUtils.extractFirstUrl(candidate) ?: candidate
                                    updateUrl(extracted)
                                }
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.dialog_paste))
                            }
                        }
                    },
                    isError = url.isNotBlank() && !isValidScheme,
                    supportingText = {
                        if (url.isNotBlank() && !isValidScheme) {
                            Text(
                                text = "Please enter a valid HTTP or HTTPS link",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = isPlaylist) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.PlaylistPlay,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.playlist_detected_banner, 1),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = fileName,
                    onValueChange = {
                        fileName = it
                        isCustomFileName = true
                    },
                    label = {
                        Text(if (isPlaylist) "Playlist Title / Folder" else stringResource(R.string.dialog_filename_label))
                    },
                    placeholder = { Text(if (isPlaylist) "e.g. My Playlist" else "e.g. video.mp4") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Storage Destination indicator card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.dialog_save_location),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedUrl = url.trim()
                    if (isValidScheme) {
                        if (isPlaylist && onConfirmPlaylist != null) {
                            val playlistTitle = if (fileName.isNotBlank()) fileName.trim() else null
                            onConfirmPlaylist(trimmedUrl, defaultDir, playlistTitle)
                        } else {
                            val rawFileName = if (fileName.isNotBlank()) fileName.trim() else "download.bin"
                            val finalFileName = FileUtils.sanitizeFileName(rawFileName)
                            val baseDir = File(defaultDir)
                            val destFile = FileUtils.getUniqueDestination(baseDir, finalFileName)
                            onConfirm(trimmedUrl, destFile.absolutePath)
                        }
                    }
                },
                enabled = isValidScheme,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.dialog_start), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

