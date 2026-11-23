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
}
