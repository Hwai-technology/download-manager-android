package com.misterdownloadmanager.domain.model

import com.google.gson.annotations.SerializedName

data class FormatChoice(
    @SerializedName("id") val id: String,
    @SerializedName("label") val label: String,
    @SerializedName("ext") val ext: String,
    @SerializedName("resolution") val resolution: String?,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?,
    @SerializedName("video_codec") val videoCodec: String?,
    @SerializedName("audio_codec") val audioCodec: String?,
    @SerializedName("is_video") val isVideo: Boolean,
    @SerializedName("is_audio") val isAudio: Boolean,
    @SerializedName("size_bytes") val sizeBytes: Long?,
    @SerializedName("bitrate_kbps") val bitrateKbps: Int?,
    @SerializedName("fps") val fps: Double?,
    @SerializedName("download_url") val downloadUrl: String?
)

data class VideoMetadata(
    @SerializedName("url") val url: String,
    @SerializedName("title") val title: String,
    @SerializedName("video_id") val videoId: String?,
    @SerializedName("uploader") val uploader: String?,
    @SerializedName("duration_secs") val durationSecs: Double?,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    @SerializedName("formats") val formats: List<FormatChoice>,
    @SerializedName("direct_file") val isDirectFile: Boolean
)
