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
}
