import Foundation

public enum DownloadState: Int, Codable {
    case probing = 0
    case downloading = 1
    case verifying = 2
    case completing = 3
    case done = 4
    case failed = 5
    case cancelled = 6
    case paused = 7
    case waiting = 8
}

public struct DownloadTask: Identifiable, Codable {
    public let id: UInt64
    public let url: String
    public let filePath: String?
    public let state: DownloadState
    public let totalBytes: Int64
    public let downloadedBytes: UInt64
    public let speedBytesPerSec: Int64 // -1 if unavailable
    public let error: String?
    public let isResumable: Bool

    public var progress: Double {
        if totalBytes > 0 {
            return Double(downloadedBytes) / Double(totalBytes)
        }
        return 0.0
    }

    public var fileName: String {
        if let path = filePath {
            return (path as NSString).lastPathComponent
        }
        return (url as NSString).lastPathComponent
    }
}
