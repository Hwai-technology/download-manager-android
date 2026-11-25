import Foundation

public struct FormatChoice: Identifiable, Codable, Equatable {
    public let id: String
    public let label: String
    public let ext: String
    public let resolution: String?
    public let width: Int?
    public let height: Int?
    public let videoCodec: String?
    public let audioCodec: String?
    public let isVideo: Bool
    public let isAudio: Bool
    public let sizeBytes: UInt64?
    public let bitrateKbps: UInt32?
    public let fps: Double?
    public let downloadUrl: String?

    enum CodingKeys: String, CodingKey {
        case id, label, ext, resolution, width, height, fps
        case videoCodec = "video_codec"
        case audioCodec = "audio_codec"
        case isVideo = "is_video"
        case isAudio = "is_audio"
        case sizeBytes = "size_bytes"
        case bitrateKbps = "bitrate_kbps"
        case downloadUrl = "download_url"
    }
}

public struct VideoMetadata: Codable, Equatable {
    public let url: String
    public let title: String
    public let videoId: String?
    public let uploader: String?
    public let durationSecs: Double?
    public let thumbnailUrl: String?
    public let formats: [FormatChoice]
    public let isDirectFile: Bool

    enum CodingKeys: String, CodingKey {
        case url, title, formats
        case videoId = "video_id"
        case uploader
        case durationSecs = "duration_secs"
        case thumbnailUrl = "thumbnail_url"
        case isDirectFile = "direct_file"
    }
}
