package com.misterdownloadmanager.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.misterdownloadmanager.R
import com.misterdownloadmanager.domain.model.DownloadState
import com.misterdownloadmanager.domain.model.DownloadTask
import com.misterdownloadmanager.presentation.ui.component.DownloadCard
import com.misterdownloadmanager.util.FileUtils
import java.io.File

enum class StatusFilter {
    ALL, ACTIVE, COMPLETED, QUEUED, FAILED
}

enum class CategoryFilter {
    ALL, VIDEO, AUDIO, DOCUMENTS, ARCHIVES, OTHER
}

@Composable
fun DownloadsScreen(
    downloads: List<DownloadTask>,
    onPlayOrOpen: (DownloadTask) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onRemoveRequest: (DownloadTask) -> Unit,
    onDetailsRequest: (DownloadTask) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf(StatusFilter.ALL) }
    var selectedCategory by remember { mutableStateOf(CategoryFilter.ALL) }

    val filteredDownloads = remember(downloads, searchQuery, selectedStatus, selectedCategory) {
        downloads.filter { task ->
            val fileName = task.filePath?.substringAfterLast(File.separator) ?: task.url
            val matchesSearch = searchQuery.isBlank() || fileName.contains(searchQuery, ignoreCase = true) || task.url.contains(searchQuery, ignoreCase = true)

            val matchesStatus = when (selectedStatus) {
                StatusFilter.ALL -> true
                StatusFilter.ACTIVE -> task.state == DownloadState.DOWNLOADING || task.state == DownloadState.PROBING
                StatusFilter.COMPLETED -> task.state == DownloadState.DONE
                StatusFilter.QUEUED -> task.state == DownloadState.WAITING || task.state == DownloadState.PAUSED
                StatusFilter.FAILED -> task.state == DownloadState.FAILED || task.state == DownloadState.CANCELLED
            }

            val matchesCategory = when (selectedCategory) {
                CategoryFilter.ALL -> true
                CategoryFilter.VIDEO -> FileUtils.isVideoFile(task.filePath)
                CategoryFilter.AUDIO -> isAudioFile(task.filePath)
                CategoryFilter.DOCUMENTS -> isDocumentFile(task.filePath)
                CategoryFilter.ARCHIVES -> isArchiveFile(task.filePath)
                CategoryFilter.OTHER -> !FileUtils.isVideoFile(task.filePath) && !isAudioFile(task.filePath) && !isDocumentFile(task.filePath) && !isArchiveFile(task.filePath)
            }

            matchesSearch && matchesStatus && matchesCategory
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterTabChip(
                label = stringResource(R.string.filter_all),
                count = downloads.size,
                selected = selectedStatus == StatusFilter.ALL,
                onClick = { selectedStatus = StatusFilter.ALL }
            )
            FilterTabChip(
                label = stringResource(R.string.filter_active),
                count = downloads.count { it.state == DownloadState.DOWNLOADING || it.state == DownloadState.PROBING },
                selected = selectedStatus == StatusFilter.ACTIVE,
                onClick = { selectedStatus = StatusFilter.ACTIVE }
            )
            FilterTabChip(
                label = stringResource(R.string.filter_completed),
                count = downloads.count { it.state == DownloadState.DONE },
                selected = selectedStatus == StatusFilter.COMPLETED,
                onClick = { selectedStatus = StatusFilter.COMPLETED }
            )
            FilterTabChip(
                label = stringResource(R.string.filter_queued),
                count = downloads.count { it.state == DownloadState.WAITING || it.state == DownloadState.PAUSED },
                selected = selectedStatus == StatusFilter.QUEUED,
                onClick = { selectedStatus = StatusFilter.QUEUED }
            )
            FilterTabChip(
                label = stringResource(R.string.filter_failed),
                count = downloads.count { it.state == DownloadState.FAILED },
                selected = selectedStatus == StatusFilter.FAILED,
                onClick = { selectedStatus = StatusFilter.FAILED }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CategoryChip(stringResource(R.string.cat_all), selectedCategory == CategoryFilter.ALL) { selectedCategory = CategoryFilter.ALL }
            CategoryChip(stringResource(R.string.cat_video), selectedCategory == CategoryFilter.VIDEO) { selectedCategory = CategoryFilter.VIDEO }
            CategoryChip(stringResource(R.string.cat_audio), selectedCategory == CategoryFilter.AUDIO) { selectedCategory = CategoryFilter.AUDIO }
            CategoryChip(stringResource(R.string.cat_documents), selectedCategory == CategoryFilter.DOCUMENTS) { selectedCategory = CategoryFilter.DOCUMENTS }
            CategoryChip(stringResource(R.string.cat_archives), selectedCategory == CategoryFilter.ARCHIVES) { selectedCategory = CategoryFilter.ARCHIVES }
            CategoryChip(stringResource(R.string.cat_other), selectedCategory == CategoryFilter.OTHER) { selectedCategory = CategoryFilter.OTHER }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Content Area
        if (filteredDownloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (searchQuery.isNotBlank()) Icons.Default.SearchOff else Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) stringResource(R.string.empty_search_title) else stringResource(R.string.empty_downloads_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) stringResource(R.string.empty_search_subtitle) else stringResource(R.string.empty_downloads_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredDownloads, key = { it.id }) { task ->
                    DownloadCard(
                        task = task,
                        onPlayOrOpen = { onPlayOrOpen(task) },
                        onPause = { onPause(task.id) },
                        onResume = { onResume(task.id) },
                        onCancel = { onCancel(task.id) },
                        onRemoveRequest = { onRemoveRequest(task) },
                        onDetailsRequest = { onDetailsRequest(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterTabChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textCol = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val borderCol = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "$label ($count)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textCol
        )
    }
}

@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val textCol = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
    val borderCol = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = textCol
        )
    }
}

private fun isAudioFile(path: String?): Boolean {
    if (path == null) return false
    val ext = path.substringAfterLast('.', "").lowercase()
    return ext in listOf("mp3", "wav", "m4a", "flac", "aac", "ogg")
}

private fun isDocumentFile(path: String?): Boolean {
    if (path == null) return false
    val ext = path.substringAfterLast('.', "").lowercase()
    return ext in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt")
}

private fun isArchiveFile(path: String?): Boolean {
    if (path == null) return false
    val ext = path.substringAfterLast('.', "").lowercase()
    return ext in listOf("zip", "rar", "7z", "tar", "gz")
}

