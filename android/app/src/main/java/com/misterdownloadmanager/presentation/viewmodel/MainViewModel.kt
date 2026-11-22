package com.misterdownloadmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misterdownloadmanager.domain.model.DownloadTask
import com.misterdownloadmanager.domain.usecase.DownloadUseCases
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(
    private val useCases: DownloadUseCases
) : ViewModel() {

    val downloads: StateFlow<List<DownloadTask>> = useCases.getDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            while (isActive) {
                useCases.refresh()
                delay(1000)
            }
        }
    }

    fun addDownload(url: String, destPath: String) {
        viewModelScope.launch {
            useCases.createDownload(url, destPath)
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

    fun removeDownload(id: Long) {
        viewModelScope.launch {
            useCases.removeDownload(id)
        }
    }
}
