package com.misterdownloadmanager.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.misterdownloadmanager.R
import com.misterdownloadmanager.domain.model.PlaylistState
import com.misterdownloadmanager.domain.model.PlaylistTask
import com.misterdownloadmanager.presentation.ui.theme.*
import com.misterdownloadmanager.util.FileUtils

@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistTask>,
    onStart: (Long) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onRetryFailed: (Long) -> Unit,
    onRemove: (Long, Boolean) -> Unit,
    onSelectPlaylist: (PlaylistTask) -> Unit,
    modifier: Modifier = Modifier
) {
    if (playlists.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistPlay,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.empty_playlists_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.empty_playlists_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(playlists, key = { it.id }) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    onStart = { onStart(playlist.id) },
                    onPause = { onPause(playlist.id) },
                    onResume = { onResume(playlist.id) },
                    onCancel = { onCancel(playlist.id) },
                    onRetryFailed = { onRetryFailed(playlist.id) },
                    onRemove = { onRemove(playlist.id, false) },
                    onClick = { onSelectPlaylist(playlist) }
                )
            }
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: PlaylistTask,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetryFailed: () -> Unit,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FeaturedPlayList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = playlist.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(
                                R.string.playlist_items_count,
                                playlist.completedItems,
                                playlist.totalItems
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                PlaylistStateBadge(state = playlist.state)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { playlist.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when (playlist.state) {
                    PlaylistState.DOWNLOADING -> MaterialTheme.colorScheme.secondary
                    PlaylistState.COMPLETED -> MaterialTheme.colorScheme.primary
                    PlaylistState.FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Details and actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val downloadedStr = FileUtils.formatFileSize(playlist.downloadedBytes)
                    val totalStr = if (playlist.totalBytes > 0) FileUtils.formatFileSize(playlist.totalBytes) else "--"
                    Text(
                        text = "$downloadedStr / $totalStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (playlist.state == PlaylistState.DOWNLOADING && playlist.aggregateSpeedBytesPerSec > 0) {
                        val speedStr = FileUtils.formatSpeed(playlist.aggregateSpeedBytesPerSec)
                        val etaStr = playlist.etaSecs?.let { FileUtils.formatDuration(it) } ?: "--"
                        Text(
                            text = stringResource(R.string.playlist_speed_eta, speedStr, etaStr),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (playlist.state) {
                        PlaylistState.DOWNLOADING -> {
                            IconButton(onClick = onPause) {
                                Icon(Icons.Default.Pause, contentDescription = stringResource(R.string.action_pause))
                            }
                        }
                        PlaylistState.PAUSED, PlaylistState.QUEUED, PlaylistState.READY, PlaylistState.CREATED -> {
                            IconButton(onClick = onResume) {
                                Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.action_resume))
                            }
                        }
                        PlaylistState.COMPLETED_WITH_ERRORS, PlaylistState.FAILED -> {
                            IconButton(onClick = onRetryFailed) {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.playlist_action_retry_failed))
                            }
                        }
                        else -> {}
                    }

                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistStateBadge(state: PlaylistState) {
    val (label, bg, fg) = when (state) {
        PlaylistState.CREATED, PlaylistState.READY -> Triple(
            stringResource(R.string.playlist_status_ready),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.primary
        )
        PlaylistState.SCANNING -> Triple(
            stringResource(R.string.playlist_status_scanning),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.tertiary
        )
        PlaylistState.DOWNLOADING -> Triple(
            stringResource(R.string.state_downloading),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.secondary
        )
        PlaylistState.PAUSED, PlaylistState.QUEUED -> Triple(
            stringResource(R.string.playlist_status_paused),
            StatePaused.copy(alpha = 0.1f),
            StatePaused
        )
        PlaylistState.COMPLETED -> Triple(
            stringResource(R.string.playlist_status_completed),
            StateDone.copy(alpha = 0.1f),
            StateDone
        )
        PlaylistState.COMPLETED_WITH_ERRORS -> Triple(
            stringResource(R.string.playlist_status_completed_errors),
            StatePaused.copy(alpha = 0.1f),
            StatePaused
        )
        PlaylistState.FAILED -> Triple(
            stringResource(R.string.playlist_status_failed),
            MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.error
        )
        PlaylistState.CANCELLED -> Triple(
            stringResource(R.string.playlist_status_cancelled),
            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
