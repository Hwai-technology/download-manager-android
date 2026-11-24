package com.misterdownloadmanager.presentation.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onDismiss: () -> Unit,
    onConfirm: (url: String, dest: String) -> Unit,
    onConfirmPlaylist: ((url: String, destDir: String, title: String?) -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    var url by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var isCustomFileName by remember { mutableStateOf(false) }

    fun updateUrl(newUrl: String) {
        url = newUrl
        if (!isCustomFileName) {
            val detected = FileUtils.extractFileNameFromUrl(newUrl)
            fileName = detected
        }
    }

    val isPlaylist = remember(url) {
        val u = url.trim().lowercase()
        u.contains("list=") || u.contains("/playlist") || u.endsWith(".m3u8") || u.endsWith(".m3u")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isPlaylist) stringResource(R.string.playlist_dialog_title) else stringResource(R.string.dialog_new_download))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { updateUrl(it) },
                    label = { Text(stringResource(R.string.dialog_url_label)) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            clipboardManager.getText()?.text?.let { clipText ->
                                updateUrl(clipText.trim())
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.dialog_paste))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isPlaylist) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.PlaylistPlay,
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
                Spacer(modifier = Modifier.height(10.dp))
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
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.dialog_save_location),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedUrl = url.trim()
                    if (trimmedUrl.startsWith("http://", ignoreCase = true) ||
                        trimmedUrl.startsWith("https://", ignoreCase = true)
                    ) {
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
                enabled = url.isNotBlank() && (url.startsWith("http://", true) || url.startsWith("https://", true))
            ) {
                Text(if (isPlaylist) stringResource(R.string.dialog_start) else stringResource(R.string.dialog_start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

