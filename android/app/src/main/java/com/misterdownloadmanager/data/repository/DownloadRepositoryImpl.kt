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

    init {
        // Initial load of restored downloads
        val initialList = MdmNativeBridge.getAllSnapshots()
        if (initialList.isNotEmpty()) {
            _downloadsFlow.value = initialList
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
}
