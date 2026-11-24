package com.misterdownloadmanager.domain.model

enum class PlaylistState(val code: Int) {
    CREATED(0),
    SCANNING(1),
    READY(2),
    QUEUED(3),
    DOWNLOADING(4),
    PAUSED(5),
    COMPLETED(6),
    COMPLETED_WITH_ERRORS(7),
    FAILED(8),
    CANCELLED(9);

    companion object {
        fun fromCode(code: Int): PlaylistState =
            entries.find { it.code == code } ?: CREATED

        fun fromString(str: String): PlaylistState {
            val normalized = str.trim().lowercase().replace("_", "")
            return when (normalized) {
                "created" -> CREATED
                "scanning" -> SCANNING
                "ready" -> READY
                "queued" -> QUEUED
                "downloading" -> DOWNLOADING
                "paused" -> PAUSED
                "completed", "done" -> COMPLETED
                "completedwitherrors" -> COMPLETED_WITH_ERRORS
                "failed" -> FAILED
                "cancelled", "canceled" -> CANCELLED
                else -> CREATED
            }
        }
    }
}

enum class PlaylistItemStatus(val code: Int) {
    PENDING(0),
    ACTIVE(1),
    PAUSED(2),
    COMPLETED(3),
    FAILED(4),
    SKIPPED(5),
    CANCELLED(6);

    companion object {
        fun fromCode(code: Int): PlaylistItemStatus =
            entries.find { it.code == code } ?: PENDING

        fun fromString(str: String): PlaylistItemStatus {
            val normalized = str.trim().lowercase().replace("_", "")
            return when (normalized) {
                "pending", "queued", "waiting" -> PENDING
                "active", "downloading" -> ACTIVE
                "paused" -> PAUSED
                "completed", "done" -> COMPLETED
                "failed" -> FAILED
                "skipped" -> SKIPPED
                "cancelled", "canceled" -> CANCELLED
                else -> PENDING
            }
        }
    }
}

data class PlaylistItem(
    val index: Long,
    val downloadId: Long? = null,
    val url: String,
    val title: String,
    val filename: String,
    val totalBytes: Long? = null,
    val downloadedBytes: Long = 0L,
    val status: PlaylistItemStatus = PlaylistItemStatus.PENDING,
    val error: String? = null,
    val durationSecs: Double? = null,
    val isUnavailable: Boolean = false
) {
    val progress: Float
        get() = if (totalBytes != null && totalBytes > 0) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else if (status == PlaylistItemStatus.COMPLETED) {
            1f
        } else {
            0f
        }
}

data class PlaylistTask(
    val id: Long,
    val url: String,
    val title: String,
    val destDir: String,
    val state: PlaylistState,
    val totalItems: Int,
    val completedItems: Int,
    val failedItems: Int,
    val activeItems: Int,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val aggregateSpeedBytesPerSec: Long,
    val etaSecs: Long?,
    val error: String? = null,
    val items: List<PlaylistItem> = emptyList()
) {
    val progress: Float
        get() = if (totalBytes > 0) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else if (totalItems > 0) {
            (completedItems.toFloat() / totalItems.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val isFinished: Boolean
        get() = state == PlaylistState.COMPLETED ||
                state == PlaylistState.COMPLETED_WITH_ERRORS ||
                state == PlaylistState.FAILED ||
                state == PlaylistState.CANCELLED
}

data class DiscoveredPlaylistDto(
    val url: String,
    val title: String,
    val items: List<DiscoveredItemDto>
)

data class DiscoveredItemDto(
    val index: Long,
    val url: String,
    val title: String,
    val duration_secs: Double?,
    val unavailable: Boolean
)
