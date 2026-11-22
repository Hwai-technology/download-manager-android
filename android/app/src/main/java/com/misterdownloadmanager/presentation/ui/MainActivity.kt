package com.misterdownloadmanager.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.misterdownloadmanager.data.repository.DownloadRepositoryImpl
import com.misterdownloadmanager.domain.model.DownloadState
import com.misterdownloadmanager.domain.model.DownloadTask
import com.misterdownloadmanager.domain.usecase.DownloadUseCases
import com.misterdownloadmanager.presentation.viewmodel.MainViewModel
import com.misterdownloadmanager.service.DownloadForegroundService
import java.io.File

class MainActivity : ComponentActivity() {

    private val repository = DownloadRepositoryImpl()
    private val useCases = DownloadUseCases(repository)
    private val viewModel = MainViewModel(useCases)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DownloadScreen(
                        viewModel = viewModel,
                        onStartService = { DownloadForegroundService.startService(this) },
                        defaultDownloadDir = getExternalFilesDir(null)?.absolutePath
                            ?: filesDir.absolutePath
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    viewModel: MainViewModel,
    onStartService: () -> Unit,
    defaultDownloadDir: String
) {
    val downloads by viewModel.downloads.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mister Download Manager") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Download")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (downloads.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active downloads",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(downloads, key = { it.id }) { task ->
                        DownloadItemRow(
                            task = task,
                            onPause = { viewModel.pauseDownload(task.id) },
                            onResume = {
                                viewModel.resumeDownload(task.id)
                                onStartService()
                            },
                            onCancel = { viewModel.cancelDownload(task.id) },
                            onRemove = { viewModel.removeDownload(task.id) }
                        )
                    }
                }
            }
        }

        if (showDialog) {
            AddDownloadDialog(
                defaultDir = defaultDownloadDir,
                onDismiss = { showDialog = false },
                onConfirm = { url, dest ->
                    viewModel.addDownload(url, dest)
                    onStartService()
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun DownloadItemRow(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = task.filePath?.substringAfterLast(File.separator) ?: task.url,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            val speedText = if (task.speedBytesPerSec != null && task.speedBytesPerSec > 0) {
                "${task.speedBytesPerSec / 1024} KB/s"
            } else {
                "--"
            }
            Text(
                text = "State: ${task.state.name} | Speed: $speedText | ${task.downloadedBytes} B",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (task.error != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Error: ${task.error}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { task.progressFraction },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (task.state == DownloadState.DOWNLOADING || task.state == DownloadState.PROBING) {
                    IconButton(onClick = onPause) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause")
                    }
                } else if (task.state == DownloadState.PAUSED || task.state == DownloadState.WAITING) {
                    IconButton(onClick = onResume) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                    }
                }
                
                if (task.state == DownloadState.DONE || task.state == DownloadState.FAILED || task.state == DownloadState.CANCELLED) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                    }
                } else {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun AddDownloadDialog(
    defaultDir: String,
    onDismiss: () -> Unit,
    onConfirm: (url: String, dest: String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("download.bin") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Download") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        val suggestedName = it.substringAfterLast("/").substringBefore("?")
                        if (suggestedName.isNotEmpty() && suggestedName.contains(".")) {
                            fileName = suggestedName
                        }
                    },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        val dest = File(defaultDir, fileName).absolutePath
                        onConfirm(url.trim(), dest)
                    }
                }
            ) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
