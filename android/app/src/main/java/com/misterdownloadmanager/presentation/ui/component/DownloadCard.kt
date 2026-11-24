package com.misterdownloadmanager.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.misterdownloadmanager.R
import com.misterdownloadmanager.domain.model.DownloadState
import com.misterdownloadmanager.domain.model.DownloadTask
import com.misterdownloadmanager.presentation.ui.theme.*
import com.misterdownloadmanager.util.FileUtils
import java.io.File

@Composable
fun DownloadCard(
    task: DownloadTask,
    onPlayOrOpen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRemoveRequest: () -> Unit,
    onDetailsRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVideo = remember(task.filePath) { FileUtils.isVideoFile(task.filePath) }
    val fileName = remember(task.filePath, task.url) {
        task.filePath?.substringAfterLast(File.separator) ?: FileUtils.extractFileNameFromUrl(task.url)
    }
    val ext = remember(fileName) {
        fileName.substringAfterLast('.', "").uppercase().take(4)
    }

    val stateColor = when (task.state) {
        DownloadState.DOWNLOADING -> StateDownloading
        DownloadState.DONE, DownloadState.COMPLETING -> StateDone
        DownloadState.PAUSED -> StatePaused
        DownloadState.FAILED -> StateFailed
        DownloadState.PROBING, DownloadState.VERIFYING -> StateProbing
        DownloadState.WAITING, DownloadState.CANCELLED -> StateWaiting
    }

    val stateLabel = when (task.state) {
        DownloadState.WAITING -> stringResource(R.string.state_waiting)
        DownloadState.PROBING -> stringResource(R.string.state_probing)
        DownloadState.DOWNLOADING -> stringResource(R.string.state_downloading)
        DownloadState.VERIFYING -> "Verifying"
        DownloadState.COMPLETING -> "Completing"
        DownloadState.PAUSED -> stringResource(R.string.state_paused)
        DownloadState.DONE -> stringResource(R.string.state_done)
        DownloadState.FAILED -> stringResource(R.string.state_failed)
        DownloadState.CANCELLED -> stringResource(R.string.state_cancelled)
    }

    val speedText = if (task.speedBytesPerSec != null && task.speedBytesPerSec > 0) {
        FileUtils.formatSpeed(task.speedBytesPerSec)
    } else {
        "--"
    }

    val sizeText = if (task.totalBytes != null && task.totalBytes > 0) {
        val downloadedMb = String.format("%.1f", task.downloadedBytes.toDouble() / (1024 * 1024))
        val totalMb = String.format("%.1f MB", task.totalBytes.toDouble() / (1024 * 1024))
        "$downloadedMb / $totalMb"
    } else {
        formatByteSize(task.downloadedBytes)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .clickable { onDetailsRequest() }
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File Type / Extension Badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = stateColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (ext.isNotBlank()) {
                        Text(
                            text = ext,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = stateColor
                        )
                    } else {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.Movie else Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = stateColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(stateColor, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stateLabel,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = stateColor
                        )
                        Text(
                            text = " • $sizeText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (task.state == DownloadState.DOWNLOADING) {
                            Text(
                                text = " • $speedText",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Progress bar
            if (task.state == DownloadState.DOWNLOADING || task.state == DownloadState.PAUSED || task.state == DownloadState.PROBING) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { task.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = stateColor,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            }

            // Error summary if failed
            if (task.error != null && task.state == DownloadState.FAILED) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = task.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.state == DownloadState.DONE) {
                    FilledTonalButton(
                        onClick = onPlayOrOpen,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.PlayCircle else Icons.Default.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isVideo) stringResource(R.string.action_play) else stringResource(R.string.action_open),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (task.state == DownloadState.DOWNLOADING || task.state == DownloadState.PROBING) {
                    IconButton(
                        onClick = onPause,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Pause,
                            contentDescription = stringResource(R.string.action_pause),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (task.state == DownloadState.PAUSED || task.state == DownloadState.WAITING) {
                    IconButton(
                        onClick = onResume,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.action_resume),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (task.state == DownloadState.DONE || task.state == DownloadState.FAILED || task.state == DownloadState.CANCELLED) {
                    IconButton(
                        onClick = onRemoveRequest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
