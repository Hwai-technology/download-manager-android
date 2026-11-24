package com.misterdownloadmanager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.misterdownloadmanager.R
import com.misterdownloadmanager.core.MdmNativeBridge
import com.misterdownloadmanager.domain.model.DownloadState
import com.misterdownloadmanager.domain.model.DownloadTask
import com.misterdownloadmanager.presentation.ui.MainActivity
import com.misterdownloadmanager.util.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DownloadForegroundService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private lateinit var notificationManager: NotificationManager
    private val finishedNotifiedIds = mutableSetOf<Long>()

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
        startForeground(FOREGROUND_SERVICE_NOTIF_ID, buildInitialForegroundNotification())
        startPollingEngine()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                PROGRESS_CHANNEL_ID,
                "Download Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live download progress and speed"
                setShowBadge(false)
            }

            val completeChannel = NotificationChannel(
                COMPLETE_CHANNEL_ID,
                "Completed Downloads",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a download finishes or fails"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(completeChannel)
        }
    }

    private fun buildInitialForegroundNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, PROGRESS_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Download engine active")
            .setSmallIcon(R.drawable.ic_stat_mdm_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun startPollingEngine() {
        serviceScope.launch {
            while (isActive) {
                val snapshots = MdmNativeBridge.getAllSnapshots()
                val activeDownloads = snapshots.filter {
                    it.state == DownloadState.DOWNLOADING || it.state == DownloadState.PROBING
                }

                // Check completed downloads for completion notifications
                for (task in snapshots) {
                    if (task.state == DownloadState.DONE && !finishedNotifiedIds.contains(task.id)) {
                        finishedNotifiedIds.add(task.id)
                        showCompletionNotification(task)
                    }
                }

                if (activeDownloads.isEmpty()) {
                    // When no downloads are actively downloading or probing, clean up and stop service
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }

                val primaryTask = activeDownloads.first()
                val activeCount = activeDownloads.size

                val fileName = primaryTask.filePath?.substringAfterLast('/')
                    ?: FileUtils.extractFileNameFromUrl(primaryTask.url)

                val speed = primaryTask.speedBytesPerSec ?: 0L
                val speedStr = FileUtils.formatSpeed(speed)
                val downloadedStr = FileUtils.formatFileSize(primaryTask.downloadedBytes)
                val totalBytes = primaryTask.totalBytes

                val (progressInt, progressMax, sizeStr) = if (totalBytes != null && totalBytes > 0) {
                    val pct = ((primaryTask.downloadedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt().coerceIn(0, 100)
                    val totalStr = FileUtils.formatFileSize(totalBytes)
                    Triple(pct, 100, "$pct% — $downloadedStr / $totalStr")
                } else {
                    Triple(0, 0, "$downloadedStr downloaded")
                }

                val title = if (activeCount > 1) {
                    "Downloading: $fileName (+${activeCount - 1} more)"
                } else {
                    "Downloading: $fileName"
                }

                val line2 = "$sizeStr • $speedStr"

                val openIntent = Intent(this@DownloadForegroundService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    this@DownloadForegroundService,
                    0,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val largeIcon = try {
                    BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
                } catch (_: Throwable) {
                    null
                }

                val notificationBuilder = NotificationCompat.Builder(this@DownloadForegroundService, PROGRESS_CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(line2)
                    .setSmallIcon(R.drawable.ic_stat_mdm_download)
                    .setLargeIcon(largeIcon)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(pendingIntent)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .setBigContentTitle(title)
                            .bigText("$line2\nStatus: ${primaryTask.state.name}")
                    )

                if (progressMax > 0) {
                    notificationBuilder.setProgress(progressMax, progressInt, false)
                } else {
                    notificationBuilder.setProgress(0, 0, true)
                }

                notificationManager.notify(FOREGROUND_SERVICE_NOTIF_ID, notificationBuilder.build())

                delay(1000)
            }
        }
    }

    private fun showCompletionNotification(task: DownloadTask) {
        val fileName = task.filePath?.substringAfterLast('/')
            ?: FileUtils.extractFileNameFromUrl(task.url)

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            task.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val totalSize = task.totalBytes?.let { FileUtils.formatFileSize(it) } ?: FileUtils.formatFileSize(task.downloadedBytes)

        val notification = NotificationCompat.Builder(this, COMPLETE_CHANNEL_ID)
            .setContentTitle("Download completed")
            .setContentText("$fileName ($totalSize)")
            .setSmallIcon(R.drawable.ic_stat_mdm_download)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify((COMPLETION_NOTIF_BASE_ID + task.id).toInt(), notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    companion object {
        const val PROGRESS_CHANNEL_ID = "mdm_download_channel"
        const val COMPLETE_CHANNEL_ID = "mdm_complete_channel"
        const val FOREGROUND_SERVICE_NOTIF_ID = 1001
        const val COMPLETION_NOTIF_BASE_ID = 5000

        fun startService(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
