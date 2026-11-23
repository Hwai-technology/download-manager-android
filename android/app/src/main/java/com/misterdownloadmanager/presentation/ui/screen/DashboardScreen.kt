package com.misterdownloadmanager.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.misterdownloadmanager.R
import com.misterdownloadmanager.domain.model.DownloadState
import com.misterdownloadmanager.domain.model.DownloadTask
import com.misterdownloadmanager.presentation.ui.component.DownloadCard
import com.misterdownloadmanager.presentation.ui.component.DownloadSpeedometerDashboard

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

