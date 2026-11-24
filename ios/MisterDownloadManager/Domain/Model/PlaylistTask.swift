import Foundation

public enum PlaylistState: Int, Codable {
    case created = 0
    case scanning = 1
    case ready = 2
    case queued = 3
    case downloading = 4
    case paused = 5
    case completed = 6
    case completedWithErrors = 7
    case failed = 8
    case cancelled = 9

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let intVal = try? container.decode(Int.self) {
            self = PlaylistState(rawValue: intVal) ?? .created
        } else if let strVal = try? container.decode(String.self) {
            let normalized = strVal.lowercased().replacingOccurrences(of: "_", with: "")
            switch normalized {
            case "created": self = .created
            case "scanning": self = .scanning
            case "ready": self = .ready
            case "queued": self = .queued
            case "downloading": self = .downloading
            case "paused": self = .paused
            case "completed", "done": self = .completed
            case "completedwitherrors": self = .completedWithErrors
            case "failed": self = .failed
            case "cancelled", "canceled": self = .cancelled
            default: self = .created
            }
        } else {
            self = .created
        }
    }
}

public enum PlaylistItemStatus: Int, Codable {
    case pending = 0
    case active = 1
    case paused = 2
    case completed = 3
    case failed = 4
    case skipped = 5
    case cancelled = 6

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let intVal = try? container.decode(Int.self) {
            self = PlaylistItemStatus(rawValue: intVal) ?? .pending
        } else if let strVal = try? container.decode(String.self) {
            let normalized = strVal.lowercased().replacingOccurrences(of: "_", with: "")
            switch normalized {
            case "pending", "queued", "waiting": self = .pending
            case "active", "downloading": self = .active
            case "paused": self = .paused
            case "completed", "done": self = .completed
            case "failed": self = .failed
            case "skipped": self = .skipped
            case "cancelled", "canceled": self = .cancelled
            default: self = .pending
            }
        } else {
            self = .pending
        }
    }
}

public struct PlaylistItem: Identifiable, Codable {
    public var id: UInt64 { index }
    public let index: UInt64
    public let downloadId: UInt64?
    public let url: String
    public let title: String
    public let filename: String
    public let totalBytes: Int64?
    public let downloadedBytes: UInt64
    public let status: PlaylistItemStatus
    public let error: String?
    public let durationSecs: Double?
    public let isUnavailable: Bool

    enum CodingKeys: String, CodingKey {
        case index
        case downloadId = "download_id"
        case url
        case title
        case filename
        case totalBytes = "total_bytes"
        case downloadedBytes = "downloaded_bytes"
        case status
        case error
        case durationSecs = "duration_secs"
        case isUnavailable = "unavailable"
    }

    public var progress: Double {
        if let total = totalBytes, total > 0 {
            return min(1.0, max(0.0, Double(downloadedBytes) / Double(total)))
        } else if status == .completed {
            return 1.0
        }
        return 0.0
    }
}

public struct PlaylistTask: Identifiable, Codable {
    public let id: UInt64
    public let url: String
    public let title: String
    public let destDir: String
    public let state: PlaylistState
    public let totalItems: Int
    public let completedItems: Int
    public let failedItems: Int
    public let activeItems: Int
    public let totalBytes: UInt64
    public let downloadedBytes: UInt64
    public let aggregateSpeedBytesPerSec: UInt64
    public let etaSecs: UInt64?
    public let error: String?
    public let items: [PlaylistItem]

    enum CodingKeys: String, CodingKey {
        case id
        case url
        case title
        case destDir = "dest_dir"
        case state
        case totalItems = "total_items"
        case completedItems = "completed_items"
        case failedItems = "failed_items"
        case activeItems = "active_items"
        case totalBytes = "total_bytes"
        case downloadedBytes = "downloaded_bytes"
        case aggregateSpeedBytesPerSec = "aggregate_speed_bytes_per_sec"
        case etaSecs = "eta_secs"
        case error
        case items
    }

    public var progress: Double {
        if totalBytes > 0 {
            return min(1.0, max(0.0, Double(downloadedBytes) / Double(totalBytes)))
        } else if totalItems > 0 {
            return min(1.0, max(0.0, Double(completedItems) / Double(totalItems)))
        }
        return 0.0
    }

    public var isFinished: Bool {
        return state == .completed || state == .completedWithErrors || state == .failed || state == .cancelled
    }
}

public struct DiscoveredPlaylist: Codable {
    public let url: String
    public let title: String
    public let items: [DiscoveredItem]
}

public struct DiscoveredItem: Codable {
    public let index: UInt64
    public let url: String
    public let title: String
    public let durationSecs: Double?
    public let unavailable: Bool

    enum CodingKeys: String, CodingKey {
        case index
        case url
        case title
        case durationSecs = "duration_secs"
        case unavailable
    }
}
