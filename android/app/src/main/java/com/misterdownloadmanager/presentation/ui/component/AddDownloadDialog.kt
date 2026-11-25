package com.misterdownloadmanager.presentation.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.misterdownloadmanager.R
import com.misterdownloadmanager.core.MdmNativeBridge
import com.misterdownloadmanager.domain.model.FormatChoice
import com.misterdownloadmanager.domain.model.VideoMetadata
import com.misterdownloadmanager.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
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

    // Persistent Storage Destination Selection
    var currentFolder by remember { mutableStateOf(defaultDir) }
    val folderPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val path = FileUtils.resolvePathFromUri(uri)
            if (!path.isNullOrBlank()) {
                currentFolder = path
            }
        }
    }

    // Live video metadata extraction state
    var isExtracting by remember { mutableStateOf(false) }
    var extractedMetadata by remember { mutableStateOf<VideoMetadata?>(null) }
    var selectedDownloadType by remember { mutableStateOf("VIDEO") } // "VIDEO" or "AUDIO"
    var selectedFormat by remember { mutableStateOf<FormatChoice?>(null) }

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

    // Auto extract real metadata when valid URL is entered
    LaunchedEffect(url) {
        val trimmed = url.trim()
        if (isValidScheme && !isPlaylist) {
            isExtracting = true
            val meta = withContext(Dispatchers.IO) {
                MdmNativeBridge.extractVideoMetadata(trimmed)
            }
            isExtracting = false
            if (meta != null) {
                extractedMetadata = meta
                if (!isCustomFileName && meta.title.isNotBlank()) {
                    val ext = if (selectedDownloadType == "AUDIO") "mp3" else "mp4"
                    fileName = "${meta.title}.$ext"
                }
                // Default format
                selectedFormat = meta.formats.firstOrNull { if (selectedDownloadType == "AUDIO") it.isAudio && !it.isVideo else it.isVideo }
                    ?: meta.formats.firstOrNull()
            }
        } else {
            extractedMetadata = null
            selectedFormat = null
        }
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
                // Section A: URL Input
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

                // Extraction Loading Indicator
                AnimatedVisibility(visible = isExtracting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Inspecting video metadata and formats...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Section B: Real Metadata Preview Card
                extractedMetadata?.let { meta ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Metadata Preview",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = meta.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            meta.uploader?.let { uploader ->
                                Text(
                                    text = "By $uploader",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Section C: Download Type Selector (Video vs Audio Only)
                    Text(
                        text = "Download Type",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedDownloadType == "VIDEO",
                            onClick = {
                                selectedDownloadType = "VIDEO"
                                val ext = "mp4"
                                fileName = "${meta.title}.$ext"
                                selectedFormat = meta.formats.firstOrNull { it.isVideo }
                            },
                            label = { Text("Video (MP4)") },
                            leadingIcon = {
                                if (selectedDownloadType == "VIDEO") {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedDownloadType == "AUDIO",
                            onClick = {
                                selectedDownloadType = "AUDIO"
                                val ext = "mp3"
                                fileName = "${meta.title}.$ext"
                                selectedFormat = meta.formats.firstOrNull { it.isAudio && !it.isVideo } ?: meta.formats.firstOrNull()
                            },
                            label = { Text("Audio Only") },
                            leadingIcon = {
                                if (selectedDownloadType == "AUDIO") {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Section D: Quality / Format Selector
                    val availableFormats = remember(selectedDownloadType, meta) {
                        if (selectedDownloadType == "AUDIO") {
                            meta.formats.filter { it.isAudio }
                        } else {
                            meta.formats.filter { it.isVideo }
                        }
                    }

                    if (availableFormats.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Available Quality",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            availableFormats.forEach { fmt ->
                                val isSelected = selectedFormat?.id == fmt.id
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedFormat = fmt
                                            val ext = fmt.ext
                                            fileName = "${meta.title}.$ext"
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                selectedFormat = fmt
                                                val ext = fmt.ext
                                                fileName = "${meta.title}.$ext"
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = fmt.label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            val details = listOfNotNull(
                                                fmt.videoCodec?.let { "Video: $it" },
                                                fmt.audioCodec?.let { "Audio: $it" },
                                                fmt.bitrateKbps?.let { "$it kbps" }
                                            ).joinToString(" • ")
                                            if (details.isNotBlank()) {
                                                Text(
                                                    text = details,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Playlist Banner
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

                // Section E: Output File / Folder Name
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

                // Section F: Simple Storage Destination Selector
                Text(
                    text = "Download Location",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentFolder.substringAfterLast(File.separator).ifBlank { currentFolder },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Change", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedUrl = selectedFormat?.downloadUrl ?: url.trim()
                    if (isValidScheme) {
                        if (isPlaylist && onConfirmPlaylist != null) {
                            val playlistTitle = if (fileName.isNotBlank()) fileName.trim() else null
                            onConfirmPlaylist(trimmedUrl, currentFolder, playlistTitle)
                        } else {
                            val rawFileName = if (fileName.isNotBlank()) fileName.trim() else "download.bin"
                            val finalFileName = FileUtils.sanitizeFileName(rawFileName)
                            val baseDir = File(currentFolder)
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
