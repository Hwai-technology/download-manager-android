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
}
