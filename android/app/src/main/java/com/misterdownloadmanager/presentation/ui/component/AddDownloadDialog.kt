package com.misterdownloadmanager.presentation.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.misterdownloadmanager.R
import com.misterdownloadmanager.util.FileUtils
import java.io.File

@Composable
fun AddDownloadDialog(
    defaultDir: String,
    onDismiss: () -> Unit,
    onConfirm: (url: String, dest: String) -> Unit
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_new_download)) },
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
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = fileName,
                    onValueChange = {
                        fileName = it
                        isCustomFileName = true
                    },
                    label = { Text(stringResource(R.string.dialog_filename_label)) },
                    placeholder = { Text("e.g. video.mp4") },
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
                        val rawFileName = if (fileName.isNotBlank()) fileName.trim() else "download.bin"
                        val finalFileName = FileUtils.sanitizeFileName(rawFileName)
                        val baseDir = File(defaultDir)
                        val destFile = FileUtils.getUniqueDestination(baseDir, finalFileName)
                        onConfirm(trimmedUrl, destFile.absolutePath)
                    }
                },
                enabled = url.isNotBlank() && (url.startsWith("http://", true) || url.startsWith("https://", true))
            ) {
                Text(stringResource(R.string.dialog_start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

