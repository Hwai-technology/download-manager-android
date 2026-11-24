import Foundation
import Combine

public protocol DownloadRepositoryProtocol {
    func getDownloadsPublisher() -> AnyPublisher<[DownloadTask], Never>
    func createDownload(url: String, destPath: String) async throws -> UInt64
    func pauseDownload(id: UInt64) async throws
    func resumeDownload(id: UInt64) async throws
    func cancelDownload(id: UInt64) async throws
    func removeDownload(id: UInt64) async throws
    func refreshSnapshots() async

    // Playlist repository methods
    func getPlaylistsPublisher() -> AnyPublisher<[PlaylistTask], Never>
    func isPlaylistUrl(_ url: String) -> Bool
    func discoverPlaylist(url: String) async throws -> DiscoveredPlaylist
    func createPlaylist(url: String, destDir: String, title: String?) async throws -> UInt64
    func startPlaylist(id: UInt64) async throws
    func pausePlaylist(id: UInt64) async throws
    func resumePlaylist(id: UInt64) async throws
    func cancelPlaylist(id: UInt64) async throws
    func retryFailedPlaylist(id: UInt64) async throws
    func removePlaylist(id: UInt64, deleteFiles: Bool) async throws
    func refreshPlaylists() async
}
