package com.misterdownloadmanager.presentation.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FeaturedPlayList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
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
import kotlinx.coroutines.flow.MutableStateFlow

enum class NavTab {
    SPEED_TEST, DASHBOARD, DOWNLOADS, PLAYLISTS, SETTINGS
}

class MainActivity : ComponentActivity() {

    private val repository = DownloadRepositoryImpl()
    private val useCases = DownloadUseCases(repository)
    private val viewModel = MainViewModel(useCases)

    private val PREFS_NAME = "mdm_ui_prefs"
    private val PREF_THEME = "pref_theme"
    private val PREF_NOTIF = "pref_notif"
    private val PREF_CLIPBOARD_DETECTION = "pref_clipboard_detection"

    private val pendingSharedUrl = MutableStateFlow<String?>(null)
    private var lastProcessedClipUrl: String? = null
    private var clipListener: ClipboardManager.OnPrimaryClipChangedListener? = null

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
        val initialClipboardDetection = prefs.getBoolean(PREF_CLIPBOARD_DETECTION, true)

        // Request POST_NOTIFICATIONS on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        handleIncomingIntent(intent)

        setContent {
            var themeMode by remember { mutableStateOf(initialThemeMode) }
            var notificationsEnabled by remember { mutableStateOf(initialNotifEnabled) }
            var clipboardDetectionEnabled by remember { mutableStateOf(initialClipboardDetection) }

            val incomingUrl by pendingSharedUrl.collectAsState()

            fun saveTheme(mode: ThemeMode) {
                themeMode = mode
                prefs.edit().putString(PREF_THEME, mode.name).apply()
            }

            fun saveNotif(enabled: Boolean) {
                notificationsEnabled = enabled
                prefs.edit().putBoolean(PREF_NOTIF, enabled).apply()
            }

            fun saveClipboardDetection(enabled: Boolean) {
                clipboardDetectionEnabled = enabled
                prefs.edit().putBoolean(PREF_CLIPBOARD_DETECTION, enabled).apply()
                if (!enabled) {
                    unregisterClipListener()
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(CLIPBOARD_NOTIF_ID)
                } else {
                    registerClipListener()
                    checkClipboardForLink()
                }
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
                        clipboardDetectionEnabled = clipboardDetectionEnabled,
                        onClipboardDetectionToggle = { saveClipboardDetection(it) },
                        onStartService = { DownloadForegroundService.startService(this) },
                        defaultDownloadDir = FileUtils.getDefaultDownloadDir().absolutePath,
                        incomingUrl = incomingUrl,
                        onConsumeIncomingUrl = { pendingSharedUrl.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        autoInitializeClipboardDetection()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            autoInitializeClipboardDetection()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterClipListener()
    }

    /**
     * Automatically initializes clipboard detection based strictly on the persisted setting.
     * Ensures detection is immediately active without requiring the user to visit or toggle Settings.
     */
    private fun autoInitializeClipboardDetection() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(PREF_CLIPBOARD_DETECTION, true)
        if (isEnabled) {
            registerClipListener()
            checkClipboardForLink()
        } else {
            unregisterClipListener()
        }
    }

    private fun registerClipListener() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(PREF_CLIPBOARD_DETECTION, true)
        if (!isEnabled) {
            unregisterClipListener()
            return
        }

        if (clipListener == null) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipListener = ClipboardManager.OnPrimaryClipChangedListener {
                checkClipboardForLink()
            }
            try {
                clipboard?.addPrimaryClipChangedListener(clipListener)
            } catch (_: Exception) {}
        }
    }

    private fun unregisterClipListener() {
        if (clipListener != null) {
            try {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.removePrimaryClipChangedListener(clipListener)
            } catch (_: Exception) {}
            clipListener = null
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    val url = FileUtils.extractFirstUrl(sharedText)
                    if (url != null) {
                        pendingSharedUrl.value = url
                    } else if (!sharedText.isNullOrBlank()) {
                        Toast.makeText(this, "No valid HTTP/HTTPS link found in shared text", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                val dataUri: Uri? = intent.data
                val urlString = dataUri?.toString()
                if (urlString != null && (urlString.startsWith("http://", ignoreCase = true) || urlString.startsWith("https://", ignoreCase = true))) {
                    pendingSharedUrl.value = urlString
                }
            }
            EXTRA_ACTION_DOWNLOAD_CLIPBOARD -> {
                val url = intent.getStringExtra(EXTRA_URL)
                if (!url.isNullOrBlank()) {
                    pendingSharedUrl.value = url
                }
            }
            EXTRA_ACTION_OPEN_APP -> {
                val url = intent.getStringExtra(EXTRA_URL)
                if (!url.isNullOrBlank()) {
                    pendingSharedUrl.value = url
                }
            }
            EXTRA_ACTION_DISMISS -> {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(CLIPBOARD_NOTIF_ID)
            }
        }
    }

    private fun checkClipboardForLink() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(PREF_CLIPBOARD_DETECTION, true)
        if (!isEnabled) return

        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            if (!clipboard.hasPrimaryClip()) return

            val item = clipboard.primaryClip?.getItemAt(0) ?: return
            val text = item.text?.toString() ?: item.uri?.toString()
            val url = FileUtils.extractFirstUrl(text)

            if (url != null && url != lastProcessedClipUrl) {
                lastProcessedClipUrl = url
                showClipboardNotification(url)
            }
        } catch (_: Exception) {
            // Respect Android clipboard security/restrictions
        }
    }

    private fun showClipboardNotification(url: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(PREF_NOTIF, true)) return

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "mdm_clipboard_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Clipboard Link Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a downloadable link is copied"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action 1: Download Now (primary content intent)
        val downloadIntent = Intent(this, MainActivity::class.java).apply {
            action = EXTRA_ACTION_DOWNLOAD_CLIPBOARD
            putExtra(EXTRA_URL, url)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val downloadPendingIntent = PendingIntent.getActivity(
            this,
            (url + "_download").hashCode(),
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Open App
        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = EXTRA_ACTION_OPEN_APP
            putExtra(EXTRA_URL, url)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            (url + "_open").hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: Later / Dismiss
        val laterIntent = Intent(this, MainActivity::class.java).apply {
            action = EXTRA_ACTION_DISMISS
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val laterPendingIntent = PendingIntent.getActivity(
            this,
            (url + "_later").hashCode(),
            laterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val preview = FileUtils.formatShortUrlPreview(url, 45)
        val title = getString(R.string.clipboard_notif_title)
        val question = getString(R.string.clipboard_notif_message)
        val fullContent = "$question\n$preview"

        // Load launcher icon as high-res large icon for branded appearance
        val largeIcon = try {
            BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        } catch (_: Throwable) {
            null
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_mdm_download)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(preview)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText(getString(R.string.app_name))
                    .bigText("$question\n\n\u200E$preview")
            )
            .setContentIntent(downloadPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                R.drawable.ic_stat_mdm_download,
                getString(R.string.clipboard_dialog_download_action),
                downloadPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_view,
                getString(R.string.clipboard_action_open),
                openPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.clipboard_action_later),
                laterPendingIntent
            )
            .build()

        notificationManager.notify(CLIPBOARD_NOTIF_ID, notification)
    }

    companion object {
        const val EXTRA_ACTION_DOWNLOAD_CLIPBOARD = "com.misterdownloadmanager.ACTION_DOWNLOAD_CLIPBOARD"
        const val EXTRA_ACTION_OPEN_APP = "com.misterdownloadmanager.ACTION_OPEN_APP"
        const val EXTRA_ACTION_DISMISS = "com.misterdownloadmanager.ACTION_DISMISS"
        const val EXTRA_URL = "com.misterdownloadmanager.EXTRA_URL"
        const val CLIPBOARD_NOTIF_ID = 2002
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
    clipboardDetectionEnabled: Boolean,
    onClipboardDetectionToggle: (Boolean) -> Unit,
    onStartService: () -> Unit,
    defaultDownloadDir: String,
    incomingUrl: String? = null,
    onConsumeIncomingUrl: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(NavTab.SPEED_TEST) }
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
    var addDialogInitialUrl by remember { mutableStateOf("") }

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

    // Automatically trigger Add Download dialog when a link is received via Share or Clipboard action
    LaunchedEffect(incomingUrl) {
        if (!incomingUrl.isNullOrBlank()) {
            addDialogInitialUrl = incomingUrl
            showAddDialog = true
            onConsumeIncomingUrl()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentTab) {
                            NavTab.SPEED_TEST -> stringResource(R.string.speed_test_title)
                            NavTab.DASHBOARD -> stringResource(R.string.app_name)
                            NavTab.DOWNLOADS -> stringResource(R.string.nav_downloads)
                            NavTab.PLAYLISTS -> stringResource(R.string.playlists_title)
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
                    icon = { Icon(Icons.AutoMirrored.Filled.FeaturedPlayList, contentDescription = null) },
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
                    onClick = {
                        addDialogInitialUrl = ""
                        showAddDialog = true
                    },
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
                NavTab.SPEED_TEST -> {
                    SpeedTestScreen(speedTestManager = speedTestManager)
                }
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
                NavTab.SETTINGS -> {
                    SettingsScreen(
                        currentThemeMode = themeMode,
                        onThemeModeChange = onThemeModeChange,
                        downloadDir = defaultDownloadDir,
                        notificationsEnabled = notificationsEnabled,
                        onNotificationsToggle = onNotificationsToggle,
                        clipboardDetectionEnabled = clipboardDetectionEnabled,
                        onClipboardDetectionToggle = onClipboardDetectionToggle
                    )
                }
            }
        }

        // Add Download Dialog
        if (showAddDialog) {
            AddDownloadDialog(
                defaultDir = defaultDownloadDir,
                initialUrl = addDialogInitialUrl,
                onDismiss = {
                    showAddDialog = false
                    addDialogInitialUrl = ""
                },
                onConfirm = { url, dest ->
                    viewModel.addDownload(url, dest)
                    onStartService()
                    showAddDialog = false
                    addDialogInitialUrl = ""
                },
                onConfirmPlaylist = { url, destDir, title ->
                    viewModel.addPlaylist(url, destDir, title)
                    onStartService()
                    showAddDialog = false
                    addDialogInitialUrl = ""
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
