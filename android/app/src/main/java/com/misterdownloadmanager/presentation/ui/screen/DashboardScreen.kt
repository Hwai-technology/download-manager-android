package com.misterdownloadmanager.presentation.ui.screen

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.misterdownloadmanager.R
import com.misterdownloadmanager.domain.model.DownloadState
import com.misterdownloadmanager.domain.model.DownloadTask
import com.misterdownloadmanager.presentation.ui.component.DownloadCard
import com.misterdownloadmanager.presentation.ui.component.DownloadSpeedometerDashboard
import com.misterdownloadmanager.util.FileUtils
import java.io.File

@Composable
fun DashboardScreen(
    downloads: List<DownloadTask>,
    totalSpeedBytesPerSec: Long,
    activeDownloadsCount: Int,
    queuedDownloadsCount: Int,
    completedDownloadsCount: Int,
    totalDownloadedBytes: Long,
    onViewAllClick: () -> Unit,
    onPlayOrOpen: (DownloadTask) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onRemoveRequest: (DownloadTask) -> Unit,
    onDetailsRequest: (DownloadTask) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeDownloads = downloads.filter {
        it.state == DownloadState.DOWNLOADING || it.state == DownloadState.PROBING
    }

    // Storage info for Downloads directory
    val storageStats = remember(downloads) {
        try {
            val downloadDir = FileUtils.getDefaultDownloadDir()
            val stat = StatFs(downloadDir.absolutePath)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            val totalStorageBytes = stat.blockCountLong * stat.blockSizeLong
            val usedByAppBytes = downloads
                .filter { it.state == DownloadState.DONE }
                .sumOf { it.downloadedBytes }

            Triple(availableBytes, totalStorageBytes, usedByAppBytes)
        } catch (_: Exception) {
            Triple(0L, 0L, 0L)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
    ) {
        // Speedometer Dashboard at the top
        item {
            DownloadSpeedometerDashboard(
                totalSpeedBytesPerSec = totalSpeedBytesPerSec,
                activeDownloadsCount = activeDownloadsCount,
                queuedDownloadsCount = queuedDownloadsCount,
                completedDownloadsCount = completedDownloadsCount,
                totalDownloadedBytes = totalDownloadedBytes
            )
        }

        // Additional Useful Dashboard Section: Storage Overview
        item {
            val (freeBytes, totalBytes, appBytes) = storageStats
            if (totalBytes > 0) {
                val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
                val usedFraction = (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                val totalStr = FileUtils.formatStorageGb(totalBytes)
                val usedStr = FileUtils.formatStorageGb(usedBytes)
                val availableStr = FileUtils.formatStorageGb(freeBytes)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.dashboard_storage_quick_stats),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = totalStr,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Linear Storage Progress
                        LinearProgressIndicator(
                            progress = { usedFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Used: $usedStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Available: $availableStr",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Section header for active downloads
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dashboard_active_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onViewAllClick) {
                    Text(
                        text = stringResource(R.string.dashboard_view_all),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (activeDownloads.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_no_active),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(activeDownloads, key = { it.id }) { task ->
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

