package com.misterdownloadmanager.data.repository

import com.misterdownloadmanager.core.MdmNativeBridge
import com.misterdownloadmanager.domain.model.DownloadTask
import com.misterdownloadmanager.domain.repository.DownloadRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class DownloadRepositoryImpl(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DownloadRepository {

    private val _downloadsFlow = MutableStateFlow<List<DownloadTask>>(emptyList())
    override fun getDownloadsFlow(): Flow<List<DownloadTask>> = _downloadsFlow.asStateFlow()

    private val _playlistsFlow = MutableStateFlow<List<com.misterdownloadmanager.domain.model.PlaylistTask>>(emptyList())
    override fun getPlaylistsFlow(): Flow<List<com.misterdownloadmanager.domain.model.PlaylistTask>> = _playlistsFlow.asStateFlow()

    init {
        // Initial load of restored downloads and playlists
        val initialList = MdmNativeBridge.getAllSnapshots()
        if (initialList.isNotEmpty()) {
            _downloadsFlow.value = initialList
        }
        val initialPlaylists = MdmNativeBridge.getAllPlaylists()
        if (initialPlaylists.isNotEmpty()) {
            _playlistsFlow.value = initialPlaylists
        }
    }

    override suspend fun createDownload(url: String, destPath: String): Result<Long> =
        withContext(ioDispatcher) {
            val id = MdmNativeBridge.createDownload(url, destPath)
            if (id > 0) {
                refreshSnapshots()
                Result.success(id)
            } else {
                Result.failure(IllegalStateException("Native engine failed to create download (code: $id)"))
            }
        }

    override suspend fun pauseDownload(id: Long): Result<Unit> = withContext(ioDispatcher) {
        val success = MdmNativeBridge.pauseDownload(id)
        if (success) {
            refreshSnapshots()
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed pausing download with id: $id"))
        }
    }

    override suspend fun resumeDownload(id: Long): Result<Unit> = withContext(ioDispatcher) {
        val success = MdmNativeBridge.resumeDownload(id)
        if (success) {
            refreshSnapshots()
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed resuming download with id: $id"))
        }
    }

    override suspend fun cancelDownload(id: Long): Result<Unit> = withContext(ioDispatcher) {
        val success = MdmNativeBridge.cancelDownload(id)
        if (success) {
            refreshSnapshots()
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed cancelling download with id: $id"))
        }
    }

    override suspend fun removeDownload(id: Long): Result<Unit> = withContext(ioDispatcher) {
        val success = MdmNativeBridge.removeDownload(id)
        if (success) {
            refreshSnapshots()
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed removing download with id: $id"))
        }
    }

    override suspend fun refreshSnapshots() = withContext(ioDispatcher) {
        val list = MdmNativeBridge.getAllSnapshots()
        _downloadsFlow.value = list
    }

    // --- Playlist Operations ---

    override fun isPlaylistUrl(url: String): Boolean =
        MdmNativeBridge.isPlaylistUrl(url)

    override suspend fun discoverPlaylist(url: String): Result<com.misterdownloadmanager.domain.model.DiscoveredPlaylistDto> =
        withContext(ioDispatcher) {
            val disc = MdmNativeBridge.discoverPlaylist(url)
            if (disc != null) {
                Result.success(disc)
            } else {
                Result.failure(IllegalStateException("Failed to discover playlist at URL: $url"))
            }
        }

    override suspend fun createPlaylist(url: String, destDir: String, title: String?): Result<Long> =
        withContext(ioDispatcher) {
            val id = MdmNativeBridge.createPlaylist(url, destDir, title)
            if (id > 0) {
                refreshPlaylists()
                Result.success(id)
            } else {
                Result.failure(IllegalStateException("Failed to create playlist (code: $id)"))
            }
        }

    override suspend fun startPlaylist(id: Long): Result<Unit> = withContext(ioDispatcher) {
        val success = MdmNativeBridge.startPlaylist(id)
        if (success) {
            refreshPlaylists()
            refreshSnapshots()
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed starting playlist with id: $id"))
        }
    }

    override suspend fun pausePlaylist(id: Long): Result<Unit> = withContext(ioDispatcher) {
        val success = MdmNativeBridge.pausePlaylist(id)
        if (success) {
            refreshPlaylists()
            refreshSnapshots()
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed pausing playlist with id: $id"))
        }
    }

    override suspend fun resumePlaylist(id: Long): Result<Unit> = withContext(ioDispatcher) {
        val success = MdmNativeBridge.resumePlaylist(id)
        if (success) {
            refreshPlaylists()
            refreshSnapshots()
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed resuming playlist with id: $id"))
        }
    }

    override suspend fun cancelPlaylist(id: Long): Result<Unit> = withContext(ioDispatcher) {
        val success = MdmNativeBridge.cancelPlaylist(id)
        if (success) {
            refreshPlaylists()
            refreshSnapshots()
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed cancelling playlist with id: $id"))
        }
    }

    override suspend fun retryFailedPlaylist(id: Long): Result<Unit> = withContext(ioDispatcher) {
        val success = MdmNativeBridge.retryFailedPlaylist(id)
        if (success) {
            refreshPlaylists()
            refreshSnapshots()
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed retrying failed items in playlist with id: $id"))
        }
    }

    override suspend fun removePlaylist(id: Long, deleteFiles: Boolean): Result<Unit> = withContext(ioDispatcher) {
        val success = MdmNativeBridge.removePlaylist(id, deleteFiles)
        if (success) {
            refreshPlaylists()
            refreshSnapshots()
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed removing playlist with id: $id"))
        }
    }

    override suspend fun refreshPlaylists() = withContext(ioDispatcher) {
        val list = MdmNativeBridge.getAllPlaylists()
        _playlistsFlow.value = list
    }
}
