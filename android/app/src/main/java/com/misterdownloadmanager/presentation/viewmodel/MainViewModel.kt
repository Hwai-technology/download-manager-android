package com.misterdownloadmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misterdownloadmanager.domain.model.DownloadTask
import com.misterdownloadmanager.domain.usecase.DownloadUseCases
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(
    private val useCases: DownloadUseCases
) : ViewModel() {

    val downloads: StateFlow<List<DownloadTask>> = useCases.getDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSpeedBytesPerSec: StateFlow<Long> = useCases.getDownloads()
        .map { list ->
            list.filter { it.state == com.misterdownloadmanager.domain.model.DownloadState.DOWNLOADING || it.state == com.misterdownloadmanager.domain.model.DownloadState.PROBING }
                .mapNotNull { it.speedBytesPerSec }
                .sum()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val activeDownloadsCount: StateFlow<Int> = useCases.getDownloads()
        .map { list ->
            list.count { it.state == com.misterdownloadmanager.domain.model.DownloadState.DOWNLOADING || it.state == com.misterdownloadmanager.domain.model.DownloadState.PROBING }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val queuedDownloadsCount: StateFlow<Int> = useCases.getDownloads()
        .map { list ->
            list.count { it.state == com.misterdownloadmanager.domain.model.DownloadState.WAITING }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedDownloadsCount: StateFlow<Int> = useCases.getDownloads()
        .map { list ->
            list.count { it.state == com.misterdownloadmanager.domain.model.DownloadState.DONE }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalDownloadedBytes: StateFlow<Long> = useCases.getDownloads()
        .map { list ->
            list.sumOf { it.downloadedBytes }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val playlists: StateFlow<List<com.misterdownloadmanager.domain.model.PlaylistTask>> = useCases.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            while (isActive) {
                useCases.refresh()
                useCases.refreshPlaylists()
                delay(1000)
            }
        }
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun clearError() {
        _errorMessage.value = null
    }

    fun addDownload(url: String, destPath: String) {
        viewModelScope.launch {
            val result = useCases.createDownload(url, destPath)
            result.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Failed to create download"
            }
        }
    }

    fun pauseDownload(id: Long) {
        viewModelScope.launch {
            useCases.pauseDownload(id)
        }
    }

    fun resumeDownload(id: Long) {
        viewModelScope.launch {
            useCases.resumeDownload(id)
        }
    }

    fun cancelDownload(id: Long) {
        viewModelScope.launch {
            useCases.cancelDownload(id)
        }
    }

    fun removeDownload(id: Long, deleteFileFromDisk: Boolean = false) {
        viewModelScope.launch {
            if (deleteFileFromDisk) {
                val task = downloads.value.find { it.id == id }
                task?.filePath?.let { path ->
                    try {
                        val file = java.io.File(path)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (_: Throwable) {}
                }
            }
            useCases.removeDownload(id)
        }
    }

    // --- Playlist Methods ---

    fun isPlaylistUrl(url: String): Boolean = useCases.isPlaylistUrl(url)

    suspend fun discoverPlaylist(url: String): com.misterdownloadmanager.domain.model.DiscoveredPlaylistDto? {
        return useCases.discoverPlaylist(url).getOrNull()
    }

    fun addPlaylist(url: String, destDir: String, title: String? = null) {
        viewModelScope.launch {
            val result = useCases.createPlaylist(url, destDir, title)
            result.onSuccess { id ->
                useCases.startPlaylist(id)
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Failed to create playlist"
            }
        }
    }

    fun startPlaylist(id: Long) {
        viewModelScope.launch {
            useCases.startPlaylist(id)
        }
    }

    fun pausePlaylist(id: Long) {
        viewModelScope.launch {
            useCases.pausePlaylist(id)
        }
    }

    fun resumePlaylist(id: Long) {
        viewModelScope.launch {
            useCases.resumePlaylist(id)
        }
    }

    fun cancelPlaylist(id: Long) {
        viewModelScope.launch {
            useCases.cancelPlaylist(id)
        }
    }

    fun retryFailedPlaylist(id: Long) {
        viewModelScope.launch {
            useCases.retryFailedPlaylist(id)
        }
    }

    fun removePlaylist(id: Long, deleteFiles: Boolean = false) {
        viewModelScope.launch {
            useCases.removePlaylist(id, deleteFiles)
        }
    }
}
