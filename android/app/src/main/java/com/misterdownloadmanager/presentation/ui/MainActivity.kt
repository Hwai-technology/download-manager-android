package com.misterdownloadmanager.presentation.ui

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.misterdownloadmanager.R
import com.misterdownloadmanager.data.repository.DownloadRepositoryImpl
import com.misterdownloadmanager.domain.model.DownloadTask
import com.misterdownloadmanager.domain.speedtest.SpeedTestManager
import com.misterdownloadmanager.domain.usecase.DownloadUseCases
import com.misterdownloadmanager.presentation.ui.component.*
import com.misterdownloadmanager.presentation.ui.screen.DashboardScreen
import com.misterdownloadmanager.presentation.ui.screen.DownloadsScreen
import com.misterdownloadmanager.presentation.ui.screen.PlaylistsScreen
import com.misterdownloadmanager.presentation.ui.screen.SettingsScreen
import com.misterdownloadmanager.presentation.ui.screen.SpeedTestScreen
import com.misterdownloadmanager.presentation.ui.theme.MdmTheme
import com.misterdownloadmanager.presentation.ui.theme.ThemeMode
import com.misterdownloadmanager.presentation.viewmodel.MainViewModel
import com.misterdownloadmanager.service.DownloadForegroundService
import com.misterdownloadmanager.util.FileUtils

enum class NavTab {
    DASHBOARD, DOWNLOADS, PLAYLISTS, SPEED_TEST, SETTINGS
}

class MainActivity : ComponentActivity() {

    private val repository = DownloadRepositoryImpl()
    private val useCases = DownloadUseCases(repository)
    private val viewModel = MainViewModel(useCases)

    private val PREFS_NAME = "mdm_ui_prefs"
    private val PREF_THEME = "pref_theme"
    private val PREF_NOTIF = "pref_notif"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedTheme = prefs.getString(PREF_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val initialThemeMode = try {
            ThemeMode.valueOf(savedTheme)
        } catch (_: Throwable) {
            ThemeMode.SYSTEM
        }
        val initialNotifEnabled = prefs.getBoolean(PREF_NOTIF, true)

        setContent {
            var themeMode by remember { mutableStateOf(initialThemeMode) }
            var notificationsEnabled by remember { mutableStateOf(initialNotifEnabled) }

            fun saveTheme(mode: ThemeMode) {
                themeMode = mode
                prefs.edit().putString(PREF_THEME, mode.name).apply()
            }

            fun saveNotif(enabled: Boolean) {
                notificationsEnabled = enabled
                prefs.edit().putBoolean(PREF_NOTIF, enabled).apply()
            }

            MdmTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScaffold(
                        viewModel = viewModel,
                        themeMode = themeMode,
                        onThemeModeChange = { saveTheme(it) },
                        notificationsEnabled = notificationsEnabled,
                        onNotificationsToggle = { saveNotif(it) },
                        onStartService = { DownloadForegroundService.startService(this) },
                        defaultDownloadDir = FileUtils.getDefaultDownloadDir().absolutePath
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: MainViewModel,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    onStartService: () -> Unit,
    defaultDownloadDir: String
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(NavTab.DASHBOARD) }
    val speedTestManager = remember { SpeedTestManager() }

    val downloads by viewModel.downloads.collectAsState()
    val totalSpeed by viewModel.totalSpeedBytesPerSec.collectAsState()
    val activeCount by viewModel.activeDownloadsCount.collectAsState()
    val queuedCount by viewModel.queuedDownloadsCount.collectAsState()
    val completedCount by viewModel.completedDownloadsCount.collectAsState()
    val totalDownloaded by viewModel.totalDownloadedBytes.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }

    // Dialog state for Task Deletion confirmation
    var taskToDelete by remember { mutableStateOf<DownloadTask?>(null) }
    // Dialog state for Task Details
    var taskForDetails by remember { mutableStateOf<DownloadTask?>(null) }
    // Dialog state for Playlist Details
    var playlistForDetails by remember { mutableStateOf<com.misterdownloadmanager.domain.model.PlaylistTask?>(null) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentTab) {
                            NavTab.DASHBOARD -> stringResource(R.string.app_name)
                            NavTab.DOWNLOADS -> stringResource(R.string.nav_downloads)
                            NavTab.PLAYLISTS -> stringResource(R.string.playlists_title)
                            NavTab.SPEED_TEST -> stringResource(R.string.speed_test_title)
                            NavTab.SETTINGS -> stringResource(R.string.nav_settings)
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                NavigationBarItem(
                    selected = currentTab == NavTab.DASHBOARD,
                    onClick = { currentTab = NavTab.DASHBOARD },
                    icon = { Icon(Icons.Default.Speed, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_dashboard)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = currentTab == NavTab.DOWNLOADS,
                    onClick = { currentTab = NavTab.DOWNLOADS },
                    icon = { Icon(Icons.Default.Download, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_downloads)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = currentTab == NavTab.PLAYLISTS,
                    onClick = { currentTab = NavTab.PLAYLISTS },
                    icon = { Icon(Icons.Default.FeaturedPlayList, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_playlists)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = currentTab == NavTab.SPEED_TEST,
                    onClick = { currentTab = NavTab.SPEED_TEST },
                    icon = { Icon(Icons.Default.NetworkCheck, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_speed_test)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = currentTab == NavTab.SETTINGS,
                    onClick = { currentTab = NavTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_settings)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        floatingActionButton = {
            if (currentTab != NavTab.SETTINGS && currentTab != NavTab.SPEED_TEST) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentTab) {
                NavTab.DASHBOARD -> {
                    DashboardScreen(
                        downloads = downloads,
                        totalSpeedBytesPerSec = totalSpeed,
                        activeDownloadsCount = activeCount,
                        queuedDownloadsCount = queuedCount,
                        completedDownloadsCount = completedCount,
                        totalDownloadedBytes = totalDownloaded,
                        onViewAllClick = { currentTab = NavTab.DOWNLOADS },
                        onPlayOrOpen = { task ->
                            task.filePath?.let { path ->
                                val res = FileUtils.openFileOrFolder(context, path)
                                res.onFailure {
                                    Toast.makeText(context, "Cannot open: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onPause = { viewModel.pauseDownload(it) },
                        onResume = {
                            viewModel.resumeDownload(it)
                            onStartService()
                        },
                        onCancel = { viewModel.cancelDownload(it) },
                        onRemoveRequest = { taskToDelete = it },
                        onDetailsRequest = { taskForDetails = it }
                    )
                }
                NavTab.DOWNLOADS -> {
                    DownloadsScreen(
                        downloads = downloads,
                        onPlayOrOpen = { task ->
                            task.filePath?.let { path ->
                                val res = FileUtils.openFileOrFolder(context, path)
                                res.onFailure {
                                    Toast.makeText(context, "Cannot open: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onPause = { viewModel.pauseDownload(it) },
                        onResume = {
                            viewModel.resumeDownload(it)
                            onStartService()
                        },
                        onCancel = { viewModel.cancelDownload(it) },
                        onRemoveRequest = { taskToDelete = it },
                        onDetailsRequest = { taskForDetails = it }
                    )
                }
                NavTab.PLAYLISTS -> {
                    PlaylistsScreen(
                        playlists = playlists,
                        onStart = {
                            viewModel.startPlaylist(it)
                            onStartService()
                        },
                        onPause = { viewModel.pausePlaylist(it) },
                        onResume = {
                            viewModel.resumePlaylist(it)
                            onStartService()
                        },
                        onCancel = { viewModel.cancelPlaylist(it) },
                        onRetryFailed = {
                            viewModel.retryFailedPlaylist(it)
                            onStartService()
                        },
                        onRemove = { id, deleteFiles ->
                            viewModel.removePlaylist(id, deleteFiles)
                        },
                        onSelectPlaylist = { playlistForDetails = it }
                    )
                }
                NavTab.SPEED_TEST -> {
                    SpeedTestScreen(speedTestManager = speedTestManager)
                }
                NavTab.SETTINGS -> {
                    SettingsScreen(
                        currentThemeMode = themeMode,
                        onThemeModeChange = onThemeModeChange,
                        downloadDir = defaultDownloadDir,
                        notificationsEnabled = notificationsEnabled,
                        onNotificationsToggle = onNotificationsToggle
                    )
                }
            }
        }

        // Add Download Dialog
        if (showAddDialog) {
            AddDownloadDialog(
                defaultDir = defaultDownloadDir,
                onDismiss = { showAddDialog = false },
                onConfirm = { url, dest ->
                    viewModel.addDownload(url, dest)
                    onStartService()
                    showAddDialog = false
                },
                onConfirmPlaylist = { url, destDir, title ->
                    viewModel.addPlaylist(url, destDir, title)
                    onStartService()
                    showAddDialog = false
                }
            )
        }

        // Delete Confirmation Dialog
        taskToDelete?.let { task ->
            val name = task.filePath?.substringAfterLast(java.io.File.separator) ?: task.url
            DeleteConfirmationDialog(
                fileName = name,
                onDismiss = { taskToDelete = null },
                onConfirm = { deleteFile ->
                    viewModel.removeDownload(task.id, deleteFileFromDisk = deleteFile)
                    taskToDelete = null
                }
            )
        }

        // Details Dialog
        taskForDetails?.let { task ->
            DownloadDetailsDialog(
                task = task,
                onDismiss = { taskForDetails = null }
            )
        }

        // Playlist Details Dialog
        playlistForDetails?.let { pl ->
            // Keep updated state from playlists
            val currentPl = playlists.find { it.id == pl.id } ?: pl
            PlaylistDetailsDialog(
                playlist = currentPl,
                onDismiss = { playlistForDetails = null }
            )
        }
    }
}
