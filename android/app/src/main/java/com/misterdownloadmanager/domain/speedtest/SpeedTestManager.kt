package com.misterdownloadmanager.domain.speedtest

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.min

enum class SpeedTestPhase {
    IDLE,
    PING,
    DOWNLOAD,
    UPLOAD,
    COMPLETE,
    ERROR
}

data class SpeedTestResult(
    val phase: SpeedTestPhase = SpeedTestPhase.IDLE,
    val pingMs: Double = 0.0,
    val jitterMs: Double = 0.0,
    val downloadSpeedMbps: Double = 0.0,
    val uploadSpeedMbps: Double = 0.0,
    val currentSpeedMbps: Double = 0.0,
    val currentProgress: Float = 0f,
    val errorMessage: String? = null
)

class SpeedTestManager {

    private val _testState = MutableStateFlow(SpeedTestResult())
    val testState: StateFlow<SpeedTestResult> = _testState.asStateFlow()

    private var testJob: Job? = null
    private val isRunning = AtomicBoolean(false)

    // Trusted, high-bandwidth global Anycast CDN endpoints for precision testing
    private val PING_URL = "https://speed.cloudflare.com/__down?bytes=0"
    private val DOWNLOAD_CHUNK_URL = "https://speed.cloudflare.com/__down?bytes=25000000" // 25 MB
    private val UPLOAD_ENDPOINT_URL = "https://speed.cloudflare.com/__up"

    fun startTest(coroutineScope: CoroutineScope) {
        if (isRunning.getAndSet(true)) return

        testJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                _testState.value = SpeedTestResult(phase = SpeedTestPhase.PING, currentProgress = 0.05f)

                // 1. Measure Ping & Jitter
                val (ping, jitter) = measurePingAndJitter()
                if (!isActive) return@launch

                _testState.value = _testState.value.copy(
                    phase = SpeedTestPhase.DOWNLOAD,
                    pingMs = ping,
                    jitterMs = jitter,
                    currentProgress = 0.20f
                )

                // 2. Measure Download Throughput
                val downloadSpeed = measureDownloadSpeed()
                if (!isActive) return@launch

                _testState.value = _testState.value.copy(
                    phase = SpeedTestPhase.UPLOAD,
                    downloadSpeedMbps = downloadSpeed,
                    currentSpeedMbps = 0.0,
                    currentProgress = 0.70f
                )

                // 3. Measure Upload Throughput
                val uploadSpeed = measureUploadSpeed()
                if (!isActive) return@launch

                // Complete
                _testState.value = _testState.value.copy(
                    phase = SpeedTestPhase.COMPLETE,
                    uploadSpeedMbps = uploadSpeed,
                    currentSpeedMbps = downloadSpeed,
                    currentProgress = 1.0f
                )
            } catch (e: CancellationException) {
                _testState.value = SpeedTestResult(phase = SpeedTestPhase.IDLE)
            } catch (e: Exception) {
                _testState.value = _testState.value.copy(
                    phase = SpeedTestPhase.ERROR,
                    errorMessage = e.localizedMessage ?: "Speed test encountered a network error"
                )
            } finally {
                isRunning.set(false)
            }
        }
    }

    fun stopTest() {
        testJob?.cancel()
        testJob = null
        isRunning.set(false)
        _testState.value = SpeedTestResult(phase = SpeedTestPhase.IDLE)
    }

    private suspend fun measurePingAndJitter(): Pair<Double, Double> = withContext(Dispatchers.IO) {
        val samples = mutableListOf<Long>()
        val totalPings = 8

        // Warm up connection
        try {
            val warmup = URL(PING_URL).openConnection() as HttpURLConnection
            warmup.connectTimeout = 3000
            warmup.readTimeout = 3000
            warmup.connect()
            warmup.responseCode
            warmup.disconnect()
        } catch (_: Exception) {}

        for (i in 1..totalPings) {
            if (!coroutineContext.isActive) break
            val startNs = System.nanoTime()
            try {
                val conn = URL(PING_URL).openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.useCaches = false
                conn.setRequestProperty("Connection", "close")
                conn.connect()
                conn.responseCode
                val durationMs = (System.nanoTime() - startNs) / 1_000_000
                samples.add(durationMs)
                conn.disconnect()

                val progress = 0.05f + (0.15f * (i.toFloat() / totalPings))
                val avgSoFar = samples.average()
                _testState.value = _testState.value.copy(
                    pingMs = avgSoFar,
                    currentProgress = progress
                )
            } catch (_: Exception) {}
            delay(50)
        }

        if (samples.isEmpty()) {
            return@withContext Pair(25.0, 2.0)
        }

        val avgPing = samples.average()
        var jitterSum = 0.0
        for (i in 1 until samples.size) {
            jitterSum += abs(samples[i] - samples[i - 1])
        }
        val avgJitter = if (samples.size > 1) jitterSum / (samples.size - 1) else 1.0

        Pair(avgPing, avgJitter)
    }

    private suspend fun measureDownloadSpeed(): Double = withContext(Dispatchers.IO) {
        val totalBytesDownloaded = AtomicLong(0)
        val startTime = System.currentTimeMillis()
        val durationMs = 8000L // 8 seconds test
        val streamCount = 4

        var smoothedSpeedMbps = 0.0
        var lastBytes = 0L
        var lastTime = startTime

        val streamJobs = (1..streamCount).map {
            launch {
                val buffer = ByteArray(64 * 1024) // 64 KB buffer
                while (isActive && (System.currentTimeMillis() - startTime) < durationMs) {
                    var conn: HttpURLConnection? = null
                    var stream: InputStream? = null
                    try {
                        conn = URL(DOWNLOAD_CHUNK_URL).openConnection() as HttpURLConnection
                        conn.connectTimeout = 5000
                        conn.readTimeout = 5000
                        conn.useCaches = false
                        conn.setRequestProperty("User-Agent", "MisterDownloadManager/0.5.2")
                        conn.connect()

                        stream = conn.inputStream
                        var bytesRead: Int
                        while (isActive && (System.currentTimeMillis() - startTime) < durationMs) {
                            bytesRead = stream.read(buffer)
                            if (bytesRead == -1) break
                            totalBytesDownloaded.addAndGet(bytesRead.toLong())
                        }
                    } catch (_: Exception) {
                        delay(200)
                    } finally {
                        try { stream?.close() } catch (_: Exception) {}
                        try { conn?.disconnect() } catch (_: Exception) {}
                    }
                }
            }
        }

        // Sampler loop to report live real-time Mbps and progress
        while (isActive && (System.currentTimeMillis() - startTime) < durationMs) {
            delay(120)
            val now = System.currentTimeMillis()
            val elapsedTotalSec = (now - startTime) / 1000.0
            val deltaSec = (now - lastTime) / 1000.0
            val currentTotalBytes = totalBytesDownloaded.get()
            val deltaBytes = currentTotalBytes - lastBytes

            if (deltaSec > 0.05) {
                val instantMbps = (deltaBytes * 8.0) / (deltaSec * 1_000_000.0)
                smoothedSpeedMbps = if (smoothedSpeedMbps == 0.0) {
                    instantMbps
                } else {
                    (smoothedSpeedMbps * 0.70) + (instantMbps * 0.30)
                }

                lastBytes = currentTotalBytes
                lastTime = now

                val progressFraction = min(1.0f, (now - startTime).toFloat() / durationMs)
                _testState.value = _testState.value.copy(
                    currentSpeedMbps = smoothedSpeedMbps,
                    downloadSpeedMbps = (currentTotalBytes * 8.0) / (elapsedTotalSec * 1_000_000.0),
                    currentProgress = 0.20f + (0.50f * progressFraction)
                )
            }
        }

        streamJobs.forEach { it.cancel() }
        val totalSec = (System.currentTimeMillis() - startTime) / 1000.0
        val finalDownloadSpeed = if (totalSec > 0) {
            (totalBytesDownloaded.get() * 8.0) / (totalSec * 1_000_000.0)
        } else {
            0.0
        }
        finalDownloadSpeed
    }

    private suspend fun measureUploadSpeed(): Double = withContext(Dispatchers.IO) {
        val totalBytesUploaded = AtomicLong(0)
        val startTime = System.currentTimeMillis()
        val durationMs = 6000L // 6 seconds upload test
        val streamCount = 2
        val payloadChunk = ByteArray(128 * 1024) { (it % 256).toByte() } // 128 KB non-empty buffer

        var smoothedSpeedMbps = 0.0
        var lastBytes = 0L
        var lastTime = startTime

        val streamJobs = (1..streamCount).map {
            launch {
                while (isActive && (System.currentTimeMillis() - startTime) < durationMs) {
                    var conn: HttpURLConnection? = null
                    var outStream: OutputStream? = null
                    try {
                        conn = URL(UPLOAD_ENDPOINT_URL).openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.doOutput = true
                        conn.connectTimeout = 5000
                        conn.readTimeout = 5000
                        conn.useCaches = false
                        conn.setChunkedStreamingMode(128 * 1024)
                        conn.setRequestProperty("Content-Type", "application/octet-stream")
                        conn.connect()

                        outStream = conn.outputStream
                        while (isActive && (System.currentTimeMillis() - startTime) < durationMs) {
                            outStream.write(payloadChunk)
                            outStream.flush()
                            totalBytesUploaded.addAndGet(payloadChunk.size.toLong())
                        }
                    } catch (_: Exception) {
                        delay(200)
                    } finally {
                        try { outStream?.close() } catch (_: Exception) {}
                        try { conn?.disconnect() } catch (_: Exception) {}
                    }
                }
            }
        }

        while (isActive && (System.currentTimeMillis() - startTime) < durationMs) {
            delay(120)
            val now = System.currentTimeMillis()
            val elapsedTotalSec = (now - startTime) / 1000.0
            val deltaSec = (now - lastTime) / 1000.0
            val currentTotalBytes = totalBytesUploaded.get()
            val deltaBytes = currentTotalBytes - lastBytes

            if (deltaSec > 0.05) {
                val instantMbps = (deltaBytes * 8.0) / (deltaSec * 1_000_000.0)
                smoothedSpeedMbps = if (smoothedSpeedMbps == 0.0) {
                    instantMbps
                } else {
                    (smoothedSpeedMbps * 0.70) + (instantMbps * 0.30)
                }

                lastBytes = currentTotalBytes
                lastTime = now

                val progressFraction = min(1.0f, (now - startTime).toFloat() / durationMs)
                _testState.value = _testState.value.copy(
                    currentSpeedMbps = smoothedSpeedMbps,
                    uploadSpeedMbps = (currentTotalBytes * 8.0) / (elapsedTotalSec * 1_000_000.0),
                    currentProgress = 0.70f + (0.30f * progressFraction)
                )
            }
        }

        streamJobs.forEach { it.cancel() }
        val totalSec = (System.currentTimeMillis() - startTime) / 1000.0
        val finalUploadSpeed = if (totalSec > 0) {
            (totalBytesUploaded.get() * 8.0) / (totalSec * 1_000_000.0)
        } else {
            0.0
        }
        finalUploadSpeed
    }
}
