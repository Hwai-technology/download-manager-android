import Foundation
import Combine

@MainActor
public final class DownloadListViewModel: ObservableObject {
    @Published public var downloads: [DownloadTask] = []
    @Published public var isAddingDownload: Bool = false

    private let useCases: DownloadUseCases
    private var cancellables = Set<AnyCancellable>()
    private var timer: AnyCancellable?

    public init(useCases: DownloadUseCases) {
        self.useCases = useCases

        useCases.getDownloads()
            .receive(on: DispatchQueue.main)
            .assign(to: &$downloads)

        timer = Timer.publish(every: 1.0, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                Task {
                    await self?.useCases.refresh()
                }
            }
    }

    public func addDownload(url: String, destPath: String) {
        Task {
            _ = try? await useCases.createDownload(url: url, destPath: destPath)
        }
    }

    public func pauseDownload(id: UInt64) {
        Task {
            try? await useCases.pauseDownload(id: id)
        }
    }

    public func resumeDownload(id: UInt64) {
        Task {
            try? await useCases.resumeDownload(id: id)
        }
    }

    public func cancelDownload(id: UInt64) {
        Task {
            try? await useCases.cancelDownload(id: id)
        }
    }

    public func removeDownload(id: UInt64) {
        Task {
            try? await useCases.removeDownload(id: id)
        }
    }
}
