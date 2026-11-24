package com.misterdownloadmanager.domain.repository

import com.misterdownloadmanager.domain.model.DiscoveredPlaylistDto
import com.misterdownloadmanager.domain.model.DownloadTask
import com.misterdownloadmanager.domain.model.PlaylistTask
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getDownloadsFlow(): Flow<List<DownloadTask>>
    suspend fun createDownload(url: String, destPath: String): Result<Long>
    suspend fun pauseDownload(id: Long): Result<Unit>
    suspend fun resumeDownload(id: Long): Result<Unit>
    suspend fun cancelDownload(id: Long): Result<Unit>
    suspend fun removeDownload(id: Long): Result<Unit>
    suspend fun refreshSnapshots()

    // Playlist repository methods
    fun getPlaylistsFlow(): Flow<List<PlaylistTask>>
    fun isPlaylistUrl(url: String): Boolean
    suspend fun discoverPlaylist(url: String): Result<DiscoveredPlaylistDto>
    suspend fun createPlaylist(url: String, destDir: String, title: String? = null): Result<Long>
    suspend fun startPlaylist(id: Long): Result<Unit>
    suspend fun pausePlaylist(id: Long): Result<Unit>
    suspend fun resumePlaylist(id: Long): Result<Unit>
    suspend fun cancelPlaylist(id: Long): Result<Unit>
    suspend fun retryFailedPlaylist(id: Long): Result<Unit>
    suspend fun removePlaylist(id: Long, deleteFiles: Boolean = false): Result<Unit>
    suspend fun refreshPlaylists()
}
