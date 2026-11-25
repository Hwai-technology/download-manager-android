package com.misterdownloadmanager.core

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.misterdownloadmanager.domain.model.DownloadState
import com.misterdownloadmanager.domain.model.DownloadTask

object MdmNativeBridge {
    private const val TAG = "MdmNativeBridge"
    private val gson = Gson()
    private var isLoaded = false

    init {
        try {
            System.loadLibrary("mdm_mobile")
            isLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library mdm_mobile not found in APK path. Fallback/scaffolding mode.")
        }
    }

    fun initialize(context: android.content.Context? = null, persistenceDir: String? = null) {
        if (isLoaded) {
            try {
                nativeInit()
                if (context != null) {
                    val tlsRes = nativeInitTls(context.applicationContext)
                    Log.i(TAG, "Native TLS verifier initialized with result: $tlsRes")
                }
                if (persistenceDir != null) {
                    nativeSetPersistenceDir(persistenceDir)
                    val count = nativeRestoreFromDisk()
                    Log.i(TAG, "Restored $count downloads from disk metadata")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed calling nativeInit: ${e.message}")
            }
        }
    }

    fun setPersistenceDir(dir: String): Boolean {
        if (!isLoaded) return false
        return nativeSetPersistenceDir(dir) == 0
    }

    fun restoreFromDisk(): Int {
        if (!isLoaded) return 0
        return nativeRestoreFromDisk()
    }

    fun setMaxConcurrentDownloads(max: Int) {
        if (!isLoaded) return
        nativeSetMaxConcurrentDownloads(max)
    }

    fun createDownload(url: String, dest: String): Long {
        if (!isLoaded) return -1L
        return nativeCreateDownload(url, dest)
    }

    fun pauseDownload(id: Long): Boolean {
        if (!isLoaded) return false
        return nativePauseDownload(id) == 0
    }

    fun resumeDownload(id: Long): Boolean {
        if (!isLoaded) return false
        return nativeResumeDownload(id) == 0
    }

    fun cancelDownload(id: Long): Boolean {
        if (!isLoaded) return false
        return nativeCancelDownload(id) == 0
    }

    fun removeDownload(id: Long): Boolean {
        if (!isLoaded) return false
        return nativeRemoveDownload(id) == 0
    }

    fun restoreDownload(id: Long, url: String, dest: String, startPaused: Boolean): Boolean {
        if (!isLoaded) return false
        return nativeRestoreDownload(id, url, dest, startPaused) == 0
    }

    fun getSnapshot(id: Long): DownloadTask? {
        if (!isLoaded) return null
        val json = nativeGetSnapshotJson(id) ?: return null
        return parseSnapshot(json)
    }

    fun getAllSnapshots(): List<DownloadTask> {
        if (!isLoaded) return emptyList()
        val json = nativeGetAllSnapshotsJson() ?: return emptyList()
        return try {
            val listType = object : com.google.gson.reflect.TypeToken<List<SnapshotDto>>() {}.type
            val dtos: List<SnapshotDto> = gson.fromJson(json, listType) ?: emptyList()
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing all snapshots: ${e.message}")
            emptyList()
        }
    }

    fun extractVideoMetadata(url: String): com.misterdownloadmanager.domain.model.VideoMetadata? {
        if (!isLoaded) return null
        val json = nativeExtractVideoMetadataJson(url) ?: return null
        return try {
            gson.fromJson(json, com.misterdownloadmanager.domain.model.VideoMetadata::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing video metadata: ${e.message}")
            null
        }
    }

    // --- Playlist Bridge APIs ---

    fun isPlaylistUrl(url: String): Boolean {
        val lower = url.trim().lowercase()
        return lower.contains("list=") ||
                lower.contains("/playlist") ||
                lower.contains(".m3u8") ||
                lower.contains(".m3u")
    }

    fun discoverPlaylist(url: String): com.misterdownloadmanager.domain.model.DiscoveredPlaylistDto? {
        if (!isLoaded) return null
        val json = nativeDiscoverPlaylistJson(url) ?: return null
        return try {
            gson.fromJson(json, com.misterdownloadmanager.domain.model.DiscoveredPlaylistDto::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering playlist: ${e.message}")
            null
        }
    }

    fun createPlaylist(url: String, destDir: String, title: String? = null): Long {
        if (!isLoaded) return -1L
        return nativeCreatePlaylist(url, destDir, title ?: "")
    }

    fun startPlaylist(id: Long): Boolean {
        if (!isLoaded) return false
        return nativeStartPlaylist(id) == 0
    }

    fun pausePlaylist(id: Long): Boolean {
        if (!isLoaded) return false
        return nativePausePlaylist(id) == 0
    }

    fun resumePlaylist(id: Long): Boolean {
        if (!isLoaded) return false
        return nativeResumePlaylist(id) == 0
    }

    fun cancelPlaylist(id: Long): Boolean {
        if (!isLoaded) return false
        return nativeCancelPlaylist(id) == 0
    }

    fun retryFailedPlaylist(id: Long): Boolean {
        if (!isLoaded) return false
        return nativeRetryFailedPlaylist(id) == 0
    }

    fun removePlaylist(id: Long, deleteFiles: Boolean): Boolean {
        if (!isLoaded) return false
        return nativeRemovePlaylist(id, if (deleteFiles) 1 else 0) == 0
    }

    fun getPlaylistSnapshot(id: Long): com.misterdownloadmanager.domain.model.PlaylistTask? {
        if (!isLoaded) return null
        val json = nativeGetPlaylistSnapshotJson(id) ?: return null
        return parsePlaylistSnapshot(json)
    }

    fun getAllPlaylists(): List<com.misterdownloadmanager.domain.model.PlaylistTask> {
        if (!isLoaded) return emptyList()
        val json = nativeGetAllPlaylistsJson() ?: return emptyList()
        return try {
            val listType = object : com.google.gson.reflect.TypeToken<List<PlaylistSnapshotDto>>() {}.type
            val dtos: List<PlaylistSnapshotDto> = gson.fromJson(json, listType) ?: emptyList()
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing all playlists: ${e.message}")
            emptyList()
        }
    }

    private fun parsePlaylistSnapshot(json: String): com.misterdownloadmanager.domain.model.PlaylistTask? {
        return try {
            val dto = gson.fromJson(json, PlaylistSnapshotDto::class.java)
            dto?.toDomain()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing playlist snapshot: ${e.message}")
            null
        }
    }

    private data class PlaylistSnapshotDto(
        @SerializedName("id") val id: Long,
        @SerializedName("url") val url: String,
        @SerializedName("title") val title: String,
        @SerializedName("dest_dir") val destDir: String,
        @SerializedName("state") val stateRaw: com.google.gson.JsonElement?,
        @SerializedName("total_items") val totalItems: Int,
        @SerializedName("completed_items") val completedItems: Int,
        @SerializedName("failed_items") val failedItems: Int,
        @SerializedName("active_items") val activeItems: Int,
        @SerializedName("total_bytes") val totalBytes: Long,
        @SerializedName("downloaded_bytes") val downloadedBytes: Long,
        @SerializedName("aggregate_speed_bytes_per_sec") val aggregateSpeed: Long,
        @SerializedName("eta_secs") val etaSecs: Long?,
        @SerializedName("error") val error: String?,
        @SerializedName("items") val items: List<PlaylistItemDto>?
    ) {
        fun toDomain(): com.misterdownloadmanager.domain.model.PlaylistTask {
            val resolvedState = when {
                stateRaw == null || stateRaw.isJsonNull -> com.misterdownloadmanager.domain.model.PlaylistState.CREATED
                stateRaw.isJsonPrimitive && stateRaw.asJsonPrimitive.isNumber ->
                    com.misterdownloadmanager.domain.model.PlaylistState.fromCode(stateRaw.asInt)
                stateRaw.isJsonPrimitive && stateRaw.asJsonPrimitive.isString ->
                    com.misterdownloadmanager.domain.model.PlaylistState.fromString(stateRaw.asString)
                else -> com.misterdownloadmanager.domain.model.PlaylistState.CREATED
            }

            return com.misterdownloadmanager.domain.model.PlaylistTask(
                id = id,
                url = url,
                title = title,
                destDir = destDir,
                state = resolvedState,
                totalItems = totalItems,
                completedItems = completedItems,
                failedItems = failedItems,
                activeItems = activeItems,
                totalBytes = totalBytes,
                downloadedBytes = downloadedBytes,
                aggregateSpeedBytesPerSec = aggregateSpeed,
                etaSecs = etaSecs,
                error = error,
                items = items?.map { it.toDomain() } ?: emptyList()
            )
        }
    }

    private data class PlaylistItemDto(
        @SerializedName("index") val index: Long,
        @SerializedName("download_id") val downloadId: Long?,
        @SerializedName("url") val url: String,
        @SerializedName("title") val title: String,
        @SerializedName("filename") val filename: String,
        @SerializedName("total_bytes") val totalBytes: Long?,
        @SerializedName("downloaded_bytes") val downloadedBytes: Long,
        @SerializedName("status") val statusRaw: com.google.gson.JsonElement?,
        @SerializedName("error") val error: String?,
        @SerializedName("duration_secs") val durationSecs: Double?,
        @SerializedName("unavailable") val unavailable: Boolean
    ) {
        fun toDomain(): com.misterdownloadmanager.domain.model.PlaylistItem {
            val resolvedStatus = when {
                statusRaw == null || statusRaw.isJsonNull -> com.misterdownloadmanager.domain.model.PlaylistItemStatus.PENDING
                statusRaw.isJsonPrimitive && statusRaw.asJsonPrimitive.isNumber ->
                    com.misterdownloadmanager.domain.model.PlaylistItemStatus.fromCode(statusRaw.asInt)
                statusRaw.isJsonPrimitive && statusRaw.asJsonPrimitive.isString ->
                    com.misterdownloadmanager.domain.model.PlaylistItemStatus.fromString(statusRaw.asString)
                else -> com.misterdownloadmanager.domain.model.PlaylistItemStatus.PENDING
            }

            return com.misterdownloadmanager.domain.model.PlaylistItem(
                index = index,
                downloadId = downloadId,
                url = url,
                title = title,
                filename = filename,
                totalBytes = totalBytes,
                downloadedBytes = downloadedBytes,
                status = resolvedStatus,
                error = error,
                durationSecs = durationSecs,
                isUnavailable = unavailable
            )
        }
    }

    private fun parseSnapshot(json: String): DownloadTask? {
        return try {
            val dto = gson.fromJson(json, SnapshotDto::class.java)
            dto?.toDomain()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing snapshot: ${e.message}")
            null
        }
    }

    private data class SnapshotDto(
        @SerializedName("id") val id: Long,
        @SerializedName("url") val url: String,
        @SerializedName("file_path") val filePath: String?,
        @SerializedName("state") val stateRaw: com.google.gson.JsonElement?,
        @SerializedName("total_bytes") val totalBytes: Long?,
        @SerializedName("downloaded_bytes") val downloadedBytes: Long,
        @SerializedName("speed_bytes_per_sec") val speed: Long?,
        @SerializedName("error") val error: String?,
        @SerializedName("resumable") val resumable: Boolean
    ) {
        fun toDomain(): DownloadTask {
            val resolvedState = when {
                stateRaw == null || stateRaw.isJsonNull -> DownloadState.WAITING
                stateRaw.isJsonPrimitive && stateRaw.asJsonPrimitive.isNumber ->
                    DownloadState.fromCode(stateRaw.asInt)
                stateRaw.isJsonPrimitive && stateRaw.asJsonPrimitive.isString ->
                    DownloadState.fromString(stateRaw.asString)
                else -> DownloadState.WAITING
            }

            return DownloadTask(
                id = id,
                url = url,
                filePath = filePath,
                state = resolvedState,
                totalBytes = totalBytes,
                downloadedBytes = downloadedBytes,
                speedBytesPerSec = speed,
                error = error,
                isResumable = resumable
            )
        }
    }

    private external fun nativeInit()
    private external fun nativeInitTls(context: android.content.Context): Int
    private external fun nativeSetPersistenceDir(dir: String): Int
    private external fun nativeRestoreFromDisk(): Int
    private external fun nativeSetMaxConcurrentDownloads(max: Int)
    private external fun nativeCreateDownload(url: String, dest: String): Long
    private external fun nativePauseDownload(id: Long): Int
    private external fun nativeResumeDownload(id: Long): Int
    private external fun nativeCancelDownload(id: Long): Int
    private external fun nativeRemoveDownload(id: Long): Int
    private external fun nativeRestoreDownload(id: Long, url: String, dest: String, startPaused: Boolean): Int
    private external fun nativeGetSnapshotJson(id: Long): String?
    private external fun nativeGetAllSnapshotsJson(): String?

    private external fun nativeExtractVideoMetadataJson(url: String): String?
    private external fun nativeDiscoverPlaylistJson(url: String): String?
    private external fun nativeCreatePlaylist(url: String, destDir: String, title: String): Long
    private external fun nativeStartPlaylist(id: Long): Int
    private external fun nativePausePlaylist(id: Long): Int
    private external fun nativeResumePlaylist(id: Long): Int
    private external fun nativeCancelPlaylist(id: Long): Int
    private external fun nativeRetryFailedPlaylist(id: Long): Int
    private external fun nativeRemovePlaylist(id: Long, deleteFiles: Int): Int
    private external fun nativeGetPlaylistSnapshotJson(id: Long): String?
    private external fun nativeGetAllPlaylistsJson(): String?
}
