import Foundation
import Combine

@MainActor
public final class DownloadListViewModel: ObservableObject {
    @Published public var downloads: [DownloadTask] = []
    @Published public var playlists: [PlaylistTask] = []
    @Published public var isAddingDownload: Bool = false

    private let useCases: DownloadUseCases
    private var cancellables = Set<AnyCancellable>()
    private var timer: AnyCancellable?

    public init(useCases: DownloadUseCases) {
        self.useCases = useCases

        useCases.getDownloads()
            .receive(on: DispatchQueue.main)
            .assign(to: &$downloads)

        useCases.getPlaylists()
            .receive(on: DispatchQueue.main)
            .assign(to: &$playlists)

        timer = Timer.publish(every: 1.0, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                Task {
                    await self?.useCases.refresh()
                    await self?.useCases.refreshPlaylists()
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

    // --- Playlist Methods ---

    public func isPlaylistUrl(_ url: String) -> Bool {
        return useCases.isPlaylistUrl(url)
    }

    public func addPlaylist(url: String, destDir: String, title: String? = nil) {
        Task {
            if let id = try? await useCases.createPlaylist(url: url, destDir: destDir, title: title) {
                try? await useCases.startPlaylist(id: id)
            }
        }
    }

    public func startPlaylist(id: UInt64) {
        Task {
            try? await useCases.startPlaylist(id: id)
        }
    }

    public func pausePlaylist(id: UInt64) {
        Task {
            try? await useCases.pausePlaylist(id: id)
        }
    }

    public func resumePlaylist(id: UInt64) {
        Task {
            try? await useCases.resumePlaylist(id: id)
        }
    }

    public func cancelPlaylist(id: UInt64) {
        Task {
            try? await useCases.cancelPlaylist(id: id)
        }
    }

    public func retryFailedPlaylist(id: UInt64) {
        Task {
            try? await useCases.retryFailedPlaylist(id: id)
        }
    }

    public func removePlaylist(id: UInt64, deleteFiles: Bool = false) {
        Task {
            try? await useCases.removePlaylist(id: id, deleteFiles: deleteFiles)
        }
    }
}
