import Foundation
import Combine

public final class DownloadRepositoryImpl: DownloadRepositoryProtocol {
    private let downloadsSubject = CurrentValueSubject<[DownloadTask], Never>([])

    public init() {
        Task {
            await refreshSnapshots()
        }
    }

    public func getDownloadsPublisher() -> AnyPublisher<[DownloadTask], Never> {
        return downloadsSubject.eraseToAnyPublisher()
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
}
