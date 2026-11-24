package com.misterdownloadmanager.domain.usecase

import com.misterdownloadmanager.domain.model.DownloadTask
import com.misterdownloadmanager.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow

class DownloadUseCases(
    private val repository: DownloadRepository
) {
    fun getDownloads(): Flow<List<DownloadTask>> = repository.getDownloadsFlow()

    suspend fun createDownload(url: String, destPath: String): Result<Long> =
        repository.createDownload(url, destPath)

    suspend fun pauseDownload(id: Long): Result<Unit> = repository.pauseDownload(id)

    suspend fun resumeDownload(id: Long): Result<Unit> = repository.resumeDownload(id)

    suspend fun cancelDownload(id: Long): Result<Unit> = repository.cancelDownload(id)

    suspend fun removeDownload(id: Long): Result<Unit> = repository.removeDownload(id)

    suspend fun refresh(): Unit = repository.refreshSnapshots()

    // Playlist Use Cases
    fun getPlaylists(): Flow<List<com.misterdownloadmanager.domain.model.PlaylistTask>> =
        repository.getPlaylistsFlow()

    fun isPlaylistUrl(url: String): Boolean = repository.isPlaylistUrl(url)

    suspend fun discoverPlaylist(url: String): Result<com.misterdownloadmanager.domain.model.DiscoveredPlaylistDto> =
        repository.discoverPlaylist(url)

    suspend fun createPlaylist(url: String, destDir: String, title: String? = null): Result<Long> =
        repository.createPlaylist(url, destDir, title)

    suspend fun startPlaylist(id: Long): Result<Unit> = repository.startPlaylist(id)

    suspend fun pausePlaylist(id: Long): Result<Unit> = repository.pausePlaylist(id)

    suspend fun resumePlaylist(id: Long): Result<Unit> = repository.resumePlaylist(id)

    suspend fun cancelPlaylist(id: Long): Result<Unit> = repository.cancelPlaylist(id)

    suspend fun retryFailedPlaylist(id: Long): Result<Unit> = repository.retryFailedPlaylist(id)

    suspend fun removePlaylist(id: Long, deleteFiles: Boolean = false): Result<Unit> =
        repository.removePlaylist(id, deleteFiles)

    suspend fun refreshPlaylists(): Unit = repository.refreshPlaylists()
}
