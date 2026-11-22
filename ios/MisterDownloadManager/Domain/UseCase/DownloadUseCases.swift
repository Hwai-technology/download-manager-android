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
}
