package com.misterdownloadmanager.presentation.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.misterdownloadmanager.R
import com.misterdownloadmanager.domain.model.DownloadTask
import com.misterdownloadmanager.util.FileUtils
import java.io.File

@Composable
fun DownloadDetailsDialog(
    task: DownloadTask,
    onDismiss: () -> Unit
) {
    val fileName = remember(task.filePath, task.url) {
        task.filePath?.substringAfterLast(File.separator) ?: task.url
    }

    val speedText = if (task.speedBytesPerSec != null && task.speedBytesPerSec > 0) {
        FileUtils.formatSpeed(task.speedBytesPerSec)
    } else {
        "--"
    }

    val sizeText = if (task.totalBytes != null && task.totalBytes > 0) {
        val downloadedMb = String.format(java.util.Locale.US, "%.2f MB", task.downloadedBytes.toDouble() / 1_000_000.0)
        val totalMb = String.format(java.util.Locale.US, "%.2f MB", task.totalBytes.toDouble() / 1_000_000.0)
        "$downloadedMb / $totalMb (${(task.progressFraction * 100).toInt()}%)"
    } else {
        formatByteSize(task.downloadedBytes)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.details_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                DetailRow(label = stringResource(R.string.details_filename), value = fileName)
                DetailRow(label = stringResource(R.string.details_status), value = task.state.name)
                DetailRow(label = stringResource(R.string.details_size), value = sizeText)
                DetailRow(label = stringResource(R.string.details_speed), value = speedText)
                DetailRow(label = stringResource(R.string.details_destination), value = task.filePath ?: "--")
                DetailRow(label = stringResource(R.string.details_url), value = task.url)

                if (task.error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.details_error),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.details_close))
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

