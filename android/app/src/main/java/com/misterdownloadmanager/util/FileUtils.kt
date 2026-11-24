package com.misterdownloadmanager.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object FileUtils {

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "3gp", "flv", "wmv", "m4v"
    )

    fun isVideoFile(filePath: String?): Boolean {
        if (filePath == null) return false
        val ext = filePath.substringAfterLast('.', "").lowercase()
        if (ext in VIDEO_EXTENSIONS) return true
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        return mime?.startsWith("video/") == true
    }

    fun getMimeType(filePath: String): String {
        val ext = filePath.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    fun getDefaultDownloadDir(): File {
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val mdmDir = File(publicDir, "Mister Download Manager")
        if (!mdmDir.exists()) {
            mdmDir.mkdirs()
        }
        return mdmDir
    }

    fun getUniqueDestination(baseDir: File, desiredFileName: String): File {
        var candidate = File(baseDir, desiredFileName)
        if (!candidate.exists()) {
            return candidate
        }

        val nameWithoutExt = desiredFileName.substringBeforeLast('.')
        val ext = if (desiredFileName.contains('.')) ".${desiredFileName.substringAfterLast('.')}" else ""

        var counter = 1
        while (candidate.exists()) {
            candidate = File(baseDir, "$nameWithoutExt ($counter)$ext")
            counter++
        }
        return candidate
    }

    fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("[/\\\\:*?\"<>|]"), "_")
            .replace("..", "")
            .trim()
        val withoutLeadingDots = cleaned.trimStart('.')
        return if (withoutLeadingDots.isNotBlank()) withoutLeadingDots else "download.bin"
    }

    private val URL_REGEX = Regex("https?://[^\\s\"'<>]+", RegexOption.IGNORE_CASE)

    fun extractFirstUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val match = URL_REGEX.find(text)
        return match?.value?.trimEnd('.', ',', ')', ']', ';')
    }

    /**
     * Shortens and formats a URL for user-facing previews in notifications or banners:
     * - Preserves scheme, domain/host, and first meaningful path segment.
     * - Strips tracking queries and long token strings.
     * - Uses an ellipsis if length exceeds maxPreviewLength.
     */
    fun formatShortUrlPreview(url: String, maxLength: Int = 45): String {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host ?: ""
            val path = uri.path ?: ""
            if (host.isEmpty()) {
                return if (url.length > maxLength) url.take(maxLength - 3) + "..." else url
            }
            val cleanBase = if (path.isEmpty() || path == "/") host else "$host$path"
            if (cleanBase.length <= maxLength) {
                cleanBase
            } else {
                val headLen = maxLength - 12
                cleanBase.take(headLen) + "..." + cleanBase.takeLast(9)
            }
        } catch (_: Exception) {
            if (url.length > maxLength) url.take(maxLength - 3) + "..." else url
        }
    }

    fun extractFileNameFromUrl(url: String): String {
        return try {
            val cleanUrl = url.substringBefore('?').substringBefore('#')
            val rawName = cleanUrl.substringAfterLast('/')
            val decoded = URLDecoder.decode(rawName, StandardCharsets.UTF_8.name())
            if (decoded.isNotBlank() && decoded.contains('.')) {
                return sanitizeFileName(decoded)
            }

            // Check query parameters for possible filename or title (e.g. ?filename=... or ?title=... or ?file=...)
            val uri = Uri.parse(url)
            val queryFileName = uri.getQueryParameter("filename")
                ?: uri.getQueryParameter("file")
                ?: uri.getQueryParameter("title")
                ?: uri.getQueryParameter("name")

            if (!queryFileName.isNullOrBlank()) {
                val decodedQuery = URLDecoder.decode(queryFileName, StandardCharsets.UTF_8.name())
                if (decodedQuery.isNotBlank()) {
                    val finalName = if (decodedQuery.contains('.')) decodedQuery else "$decodedQuery.mp4"
                    return sanitizeFileName(finalName)
                }
            }

            // Check if it's YouTube video
            val videoId = uri.getQueryParameter("v")
            if (!videoId.isNullOrBlank()) {
                return sanitizeFileName("YouTube_Video_$videoId.mp4")
            }

            val pathSegs = uri.pathSegments
            if (!pathSegs.isNullOrEmpty() && (uri.host?.contains("youtu.be") == true)) {
                val id = pathSegs.last()
                if (id.isNotBlank()) {
                    return sanitizeFileName("YouTube_Video_$id.mp4")
                }
            }

            val candidate = if (decoded.isNotBlank()) {
                "$decoded.mp4"
            } else {
                "download.bin"
            }
            sanitizeFileName(candidate)
        } catch (_: Exception) {
            "download.bin"
        }
    }

    fun openFileOrFolder(context: Context, filePath: String): Result<Unit> {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return Result.failure(IllegalStateException("File does not exist: $filePath"))
            }

            val parentDir = file.parentFile ?: file
            val parentUri = Uri.parse(parentDir.absolutePath)

            // Attempt to open the directory/folder in a File Manager application
            val folderIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(parentDir), "resource/folder")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (folderIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(folderIntent)
                return Result.success(Unit)
            }

            // Fallback: Open file using FileProvider with its specific MIME type
            openFile(context, filePath)
        } catch (_: Exception) {
            openFile(context, filePath)
        }
    }

    fun openFile(context: Context, filePath: String): Result<Unit> {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return Result.failure(IllegalStateException("File does not exist on storage: $filePath"))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mimeType = getMimeType(filePath)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Result.success(Unit)
            } else {
                // Fallback to chooser or generic view
                val chooser = Intent.createChooser(intent, "Open with...").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(chooser)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024L * 1024L -> String.format("%.2f GB", bytes.toDouble() / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes.toDouble() / (1024.0 * 1024.0))
            bytes >= 1024L -> String.format("%.0f KB", bytes.toDouble() / 1024.0)
            bytes > 0 -> "$bytes B"
            else -> "0 B"
        }
    }

    fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024L * 1024L * 1024L -> String.format("%.2f GB/s", bytesPerSec.toDouble() / (1024.0 * 1024.0 * 1024.0))
            bytesPerSec >= 1024L * 1024L -> String.format("%.1f MB/s", bytesPerSec.toDouble() / (1024.0 * 1024.0))
            bytesPerSec >= 1024L -> String.format("%.0f KB/s", bytesPerSec.toDouble() / 1024.0)
            bytesPerSec > 0 -> "$bytesPerSec B/s"
            else -> "0 B/s"
        }
    }

    fun formatDuration(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        val hrs = s / 3600
        val mins = (s % 3600) / 60
        val secs = s % 60
        return if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }
}

