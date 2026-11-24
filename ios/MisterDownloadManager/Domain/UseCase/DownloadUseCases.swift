import Foundation
import Combine

public struct DownloadUseCases {
    private let repository: DownloadRepositoryProtocol

    public init(repository: DownloadRepositoryProtocol) {
        self.repository = repository
    }

    public func getDownloads() -> AnyPublisher<[DownloadTask], Never> {
        return repository.getDownloadsPublisher()
    }

    public func createDownload(url: String, destPath: String) async throws -> UInt64 {
        return try await repository.createDownload(url: url, destPath: destPath)
    }

    public func pauseDownload(id: UInt64) async throws {
        try await repository.pauseDownload(id: id)
    }

    public func resumeDownload(id: UInt64) async throws {
        try await repository.resumeDownload(id: id)
    }

    public func cancelDownload(id: UInt64) async throws {
        try await repository.cancelDownload(id: id)
    }

    public func removeDownload(id: UInt64) async throws {
        try await repository.removeDownload(id: id)
    }

    public func refresh() async {
        await repository.refreshSnapshots()
    }

    // --- Playlist Use Cases ---

    public func getPlaylists() -> AnyPublisher<[PlaylistTask], Never> {
        return repository.getPlaylistsPublisher()
    }

    public func isPlaylistUrl(_ url: String) -> Bool {
        return repository.isPlaylistUrl(url)
    }

    public func discoverPlaylist(url: String) async throws -> DiscoveredPlaylist {
        return try await repository.discoverPlaylist(url: url)
    }

    public func createPlaylist(url: String, destDir: String, title: String? = nil) async throws -> UInt64 {
        return try await repository.createPlaylist(url: url, destDir: destDir, title: title)
    }

    public func startPlaylist(id: UInt64) async throws {
        try await repository.startPlaylist(id: id)
    }

    public func pausePlaylist(id: UInt64) async throws {
        try await repository.pausePlaylist(id: id)
    }

    public func resumePlaylist(id: UInt64) async throws {
        try await repository.resumePlaylist(id: id)
    }

    public func cancelPlaylist(id: UInt64) async throws {
        try await repository.cancelPlaylist(id: id)
    }

    public func retryFailedPlaylist(id: UInt64) async throws {
        try await repository.retryFailedPlaylist(id: id)
    }

    public func removePlaylist(id: UInt64, deleteFiles: Bool = false) async throws {
        try await repository.removePlaylist(id: id, deleteFiles: deleteFiles)
    }

    public func refreshPlaylists() async {
        await repository.refreshPlaylists()
    }
}
