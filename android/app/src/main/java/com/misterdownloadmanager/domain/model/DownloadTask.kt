package com.misterdownloadmanager.domain.model

enum class DownloadState(val code: Int) {
    PROBING(0),
    DOWNLOADING(1),
    VERIFYING(2),
    COMPLETING(3),
    DONE(4),
    FAILED(5),
    CANCELLED(6),
    PAUSED(7),
    WAITING(8);

    companion object {
        fun fromCode(code: Int): DownloadState =
            entries.firstOrNull { it.code == code } ?: WAITING

        fun fromString(name: String?): DownloadState {
            if (name == null) return WAITING
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: WAITING
        }
    }
}

data class DownloadTask(
    val id: Long,
    val url: String,
    val filePath: String?,
    val state: DownloadState,
    val totalBytes: Long?,
    val downloadedBytes: Long,
    val speedBytesPerSec: Long?,
    val error: String?,
    val isResumable: Boolean
) {
    val progressFraction: Float
        get() = if (totalBytes != null && totalBytes > 0) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}
