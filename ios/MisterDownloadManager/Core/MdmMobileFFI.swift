import Foundation

public enum MdmMobileFFI {
    public static func initialize(persistenceDir: String? = nil) {
        mdm_mobile_init()
        if let dir = persistenceDir {
            _ = setPersistenceDir(dir)
            _ = restoreFromDisk()
        }
    }

    public static func setPersistenceDir(_ dir: String) -> Bool {
        return dir.withCString { cDir in
            return mdm_mobile_set_persistence_dir(cDir) == 0
        }
    }

    public static func restoreFromDisk() -> Int32 {
        return mdm_mobile_restore_from_disk()
    }

    public static func setMaxConcurrentDownloads(_ max: UInt32) {
        mdm_mobile_set_max_concurrent_downloads(max)
    }

    public static func createDownload(url: String, dest: String) -> Int64 {
        return url.withCString { cUrl in
            dest.withCString { cDest in
                return mdm_mobile_create_download(cUrl, cDest)
            }
        }
    }

    public static func pauseDownload(id: UInt64) -> Bool {
        return mdm_mobile_pause_download(id) == 0
    }

    public static func resumeDownload(id: UInt64) -> Bool {
        return mdm_mobile_resume_download(id) == 0
    }

    public static func cancelDownload(id: UInt64) -> Bool {
        return mdm_mobile_cancel_download(id) == 0
    }

    public static func removeDownload(id: UInt64) -> Bool {
        return mdm_mobile_remove_download(id) == 0
    }

    public static func restoreDownload(id: UInt64, url: String, dest: String, startPaused: Bool) -> Bool {
        return url.withCString { cUrl in
            dest.withCString { cDest in
                return mdm_mobile_restore_download(id, cUrl, cDest, startPaused ? 1 : 0) == 0
            }
        }
    }

    public static func getAllSnapshots() throws -> [DownloadTask] {
        var list = CMobileSnapshotList(items: nil, count: 0, capacity: 0)
        let res = mdm_mobile_get_all_snapshots(&list)
        guard res == 0 else {
            throw NSError(domain: "MdmEngineError", code: Int(res), userInfo: [NSLocalizedDescriptionKey: "Failed to retrieve engine snapshots (error code: \(res))"])
        }
        guard let items = list.items else { return [] }

        var result: [DownloadTask] = []
        result.reserveCapacity(list.count)
        for i in 0..<list.count {
            let item = items[i]
            let urlStr = item.url != nil ? String(cString: item.url) : ""
            let pathStr = item.file_path != nil ? String(cString: item.file_path) : nil
            let errStr = item.error != nil ? String(cString: item.error) : nil
            let state = DownloadState(rawValue: Int(item.state.rawValue)) ?? .waiting

            let task = DownloadTask(
                id: item.id,
                url: urlStr,
                filePath: pathStr,
                state: state,
                totalBytes: item.total_bytes,
                downloadedBytes: item.downloaded_bytes,
                speedBytesPerSec: item.speed_bytes_per_sec,
                error: errStr,
                isResumable: item.resumable
            )
            result.append(task)
        }

        mdm_mobile_free_snapshot_list(&list)
        return result
    }

    public static func extractVideoMetadata(url: String) -> VideoMetadata? {
        guard let cUrl = (url as NSString).utf8String else { return nil }
        guard let jsonPtr = mdm_mobile_extract_video_metadata_json(cUrl) else { return nil }
        let jsonStr = String(cString: jsonPtr)
        mdm_mobile_free_string(jsonPtr)
        guard let data = jsonStr.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(VideoMetadata.self, from: data)
    }

    // --- Playlist FFI Bindings ---

    public static func isPlaylistUrl(_ url: String) -> Bool {
        let lower = url.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return lower.contains("list=") ||
            lower.contains("/playlist") ||
            lower.hasSuffix(".m3u8") ||
            lower.hasSuffix(".m3u")
    }

    public static func discoverPlaylist(url: String) -> DiscoveredPlaylist? {
        guard let cUrl = (url as NSString).utf8String else { return nil }
        guard let jsonPtr = mdm_mobile_discover_playlist_json(cUrl) else { return nil }
        let jsonStr = String(cString: jsonPtr)
        mdm_mobile_free_string(jsonPtr)
        guard let data = jsonStr.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(DiscoveredPlaylist.self, from: data)
    }

    public static func createPlaylist(url: String, destDir: String, title: String? = nil) -> Int64 {
        return url.withCString { cUrl in
            destDir.withCString { cDest in
                if let t = title {
                    return t.withCString { cTitle in
                        return mdm_mobile_create_playlist(cUrl, cDest, cTitle)
                    }
                } else {
                    return mdm_mobile_create_playlist(cUrl, cDest, nil)
                }
            }
        }
    }

    public static func startPlaylist(id: UInt64) -> Bool {
        return mdm_mobile_start_playlist(id) == 0
    }

    public static func pausePlaylist(id: UInt64) -> Bool {
        return mdm_mobile_pause_playlist(id) == 0
    }

    public static func resumePlaylist(id: UInt64) -> Bool {
        return mdm_mobile_resume_playlist(id) == 0
    }

    public static func cancelPlaylist(id: UInt64) -> Bool {
        return mdm_mobile_cancel_playlist(id) == 0
    }

    public static func retryFailedPlaylist(id: UInt64) -> Bool {
        return mdm_mobile_retry_failed_playlist(id) == 0
    }

    public static func removePlaylist(id: UInt64, deleteFiles: Bool = false) -> Bool {
        return mdm_mobile_remove_playlist(id, deleteFiles ? 1 : 0) == 0
    }

    public static func getPlaylistSnapshot(id: UInt64) -> PlaylistTask? {
        guard let jsonPtr = mdm_mobile_get_playlist_snapshot_json(id) else { return nil }
        let jsonStr = String(cString: jsonPtr)
        mdm_mobile_free_string(jsonPtr)
        guard let data = jsonStr.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(PlaylistTask.self, from: data)
    }

    public static func getAllPlaylists() -> [PlaylistTask] {
        guard let jsonPtr = mdm_mobile_get_all_playlists_json() else { return [] }
        let jsonStr = String(cString: jsonPtr)
        mdm_mobile_free_string(jsonPtr)
        guard let data = jsonStr.data(using: .utf8) else { return [] }
        return (try? JSONDecoder().decode([PlaylistTask].self, from: data)) ?? []
    }
}
