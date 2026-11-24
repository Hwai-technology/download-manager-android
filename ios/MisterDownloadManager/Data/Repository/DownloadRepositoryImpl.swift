import Foundation
import Combine

public final class DownloadRepositoryImpl: DownloadRepositoryProtocol {
    private let downloadsSubject = CurrentValueSubject<[DownloadTask], Never>([])
    private let playlistsSubject = CurrentValueSubject<[PlaylistTask], Never>([])

    public init() {
        Task {
            await refreshSnapshots()
            await refreshPlaylists()
        }
    }

    public func getDownloadsPublisher() -> AnyPublisher<[DownloadTask], Never> {
        return downloadsSubject.eraseToAnyPublisher()
    }

    public func getPlaylistsPublisher() -> AnyPublisher<[PlaylistTask], Never> {
        return playlistsSubject.eraseToAnyPublisher()
    }

    public func createDownload(url: String, destPath: String) async throws -> UInt64 {
        let res = MdmMobileFFI.createDownload(url: url, dest: destPath)
        guard res > 0 else {
            throw NSError(domain: "MdmEngineError", code: Int(res), userInfo: [NSLocalizedDescriptionKey: "Failed to create download in native core"])
        }
        await refreshSnapshots()
        return UInt64(res)
    }

    public func pauseDownload(id: UInt64) async throws {
        let success = MdmMobileFFI.pauseDownload(id: id)
        guard success else {
            throw NSError(domain: "MdmEngineError", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to pause download"])
        }
        await refreshSnapshots()
    }

    public func resumeDownload(id: UInt64) async throws {
        let success = MdmMobileFFI.resumeDownload(id: id)
        guard success else {
            throw NSError(domain: "MdmEngineError", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to resume download"])
        }
        await refreshSnapshots()
    }

    public func cancelDownload(id: UInt64) async throws {
        let success = MdmMobileFFI.cancelDownload(id: id)
        guard success else {
            throw NSError(domain: "MdmEngineError", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to cancel download"])
        }
        await refreshSnapshots()
    }

    public func removeDownload(id: UInt64) async throws {
        let success = MdmMobileFFI.removeDownload(id: id)
        guard success else {
            throw NSError(domain: "MdmEngineError", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to remove download"])
        }
        await refreshSnapshots()
    }

    public func refreshSnapshots() async {
        do {
            let list = try MdmMobileFFI.getAllSnapshots()
            downloadsSubject.send(list)
        } catch {
            print("Error refreshing snapshots: \(error)")
        }
    }

    // --- Playlist Repository Implementation ---

    public func isPlaylistUrl(_ url: String) -> Bool {
        return MdmMobileFFI.isPlaylistUrl(url)
    }

    public func discoverPlaylist(url: String) async throws -> DiscoveredPlaylist {
        guard let pl = MdmMobileFFI.discoverPlaylist(url: url) else {
            throw NSError(domain: "MdmPlaylistError", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to discover playlist from URL"])
        }
        return pl
    }

    public func createPlaylist(url: String, destDir: String, title: String?) async throws -> UInt64 {
        let res = MdmMobileFFI.createPlaylist(url: url, destDir: destDir, title: title)
        guard res > 0 else {
            throw NSError(domain: "MdmPlaylistError", code: Int(res), userInfo: [NSLocalizedDescriptionKey: "Failed to create playlist"])
        }
        await refreshPlaylists()
        return UInt64(res)
    }

    public func startPlaylist(id: UInt64) async throws {
        let success = MdmMobileFFI.startPlaylist(id: id)
        guard success else {
            throw NSError(domain: "MdmPlaylistError", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to start playlist"])
        }
        await refreshPlaylists()
        await refreshSnapshots()
    }

    public func pausePlaylist(id: UInt64) async throws {
        let success = MdmMobileFFI.pausePlaylist(id: id)
        guard success else {
            throw NSError(domain: "MdmPlaylistError", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to pause playlist"])
        }
        await refreshPlaylists()
        await refreshSnapshots()
    }

    public func resumePlaylist(id: UInt64) async throws {
        let success = MdmMobileFFI.resumePlaylist(id: id)
        guard success else {
            throw NSError(domain: "MdmPlaylistError", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to resume playlist"])
        }
        await refreshPlaylists()
        await refreshSnapshots()
    }

    public func cancelPlaylist(id: UInt64) async throws {
        let success = MdmMobileFFI.cancelPlaylist(id: id)
        guard success else {
            throw NSError(domain: "MdmPlaylistError", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to cancel playlist"])
        }
        await refreshPlaylists()
        await refreshSnapshots()
    }

    public func retryFailedPlaylist(id: UInt64) async throws {
        let success = MdmMobileFFI.retryFailedPlaylist(id: id)
        guard success else {
            throw NSError(domain: "MdmPlaylistError", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to retry failed playlist items"])
        }
        await refreshPlaylists()
        await refreshSnapshots()
    }

    public func removePlaylist(id: UInt64, deleteFiles: Bool) async throws {
        let success = MdmMobileFFI.removePlaylist(id: id, deleteFiles: deleteFiles)
        guard success else {
            throw NSError(domain: "MdmPlaylistError", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to remove playlist"])
        }
        await refreshPlaylists()
        await refreshSnapshots()
    }

    public func refreshPlaylists() async {
        let list = MdmMobileFFI.getAllPlaylists()
        playlistsSubject.send(list)
    }
}
