package com.misterdownloadmanager.domain.repository

import com.misterdownloadmanager.domain.model.DownloadTask
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getDownloadsFlow(): Flow<List<DownloadTask>>
    suspend fun createDownload(url: String, destPath: String): Result<Long>
    suspend fun pauseDownload(id: Long): Result<Unit>
    suspend fun resumeDownload(id: Long): Result<Unit>
    suspend fun cancelDownload(id: Long): Result<Unit>
    suspend fun removeDownload(id: Long): Result<Unit>
    suspend fun refreshSnapshots()
}
