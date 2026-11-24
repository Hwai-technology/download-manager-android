import SwiftUI

public struct PlaylistsScreen: View {
    @ObservedObject public var viewModel: DownloadListViewModel
    @State private var selectedPlaylist: PlaylistTask?

    public init(viewModel: DownloadListViewModel) {
        self.viewModel = viewModel
    }

    public var body: some View {
        NavigationView {
            ZStack {
                Color(uiColor: .systemGroupedBackground)
                    .ignoresSafeArea()

                if viewModel.playlists.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "music.note.list")
                            .font(.system(size: 60))
                            .foregroundColor(.secondary.opacity(0.6))

                        Text("No playlists yet")
                            .font(.headline)
                            .foregroundColor(.primary)

                        Text("Add a playlist URL (YouTube, M3U/M3U8) to download all items")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)
                    }
                } else {
                    List {
                        ForEach(viewModel.playlists) { playlist in
                            PlaylistCardView(
                                playlist: playlist,
                                onStart: { viewModel.startPlaylist(id: playlist.id) },
                                onPause: { viewModel.pausePlaylist(id: playlist.id) },
                                onResume: { viewModel.resumePlaylist(id: playlist.id) },
                                onRetryFailed: { viewModel.retryFailedPlaylist(id: playlist.id) },
                                onRemove: { viewModel.removePlaylist(id: playlist.id, deleteFiles: false) },
                                onTap: { selectedPlaylist = playlist }
                            )
                            .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                            .listRowBackground(Color.clear)
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Playlists")
            .sheet(item: $selectedPlaylist) { playlist in
                PlaylistDetailSheet(playlist: playlist)
            }
        }
    }
}

public struct PlaylistCardView: View {
    public let playlist: PlaylistTask
    public let onStart: () -> Void
    public let onPause: () -> Void
    public let onResume: () -> Void
    public let onRetryFailed: () -> Void
    public let onRemove: () -> Void
    public let onTap: () -> Void

    public var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top) {
                    Image(systemName: "list.bullet.rectangle.fill")
                        .font(.system(size: 28))
                        .foregroundColor(Color("mdmBlue", bundle: nil))
                        .padding(8)
                        .background(Color("mdmBlue", bundle: nil).opacity(0.12))
                        .cornerRadius(8)

                    VStack(alignment: .leading, spacing: 4) {
                        Text(playlist.title)
                            .font(.headline)
                            .foregroundColor(.primary)
                            .lineLimit(1)

                        Text("\(playlist.completedItems) of \(playlist.totalItems) items")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    PlaylistStateBadgeView(state: playlist.state)
                }

                // Progress Bar
                ProgressView(value: playlist.progress)
                    .progressViewStyle(LinearProgressViewStyle(tint: stateColor))
                    .scaleEffect(x: 1, y: 1.5, anchor: .center)

                // Info & Action Row
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        let downloadedStr = ByteCountFormatter.string(fromByteCount: Int64(playlist.downloadedBytes), countStyle: .file)
                        let totalStr = playlist.totalBytes > 0 ? ByteCountFormatter.string(fromByteCount: Int64(playlist.totalBytes), countStyle: .file) : "--"
                        Text("\(downloadedStr) / \(totalStr)")
                            .font(.caption2)
                            .foregroundColor(.secondary)

                        if playlist.state == .downloading && playlist.aggregateSpeedBytesPerSec > 0 {
                            let speedStr = ByteCountFormatter.string(fromByteCount: Int64(playlist.aggregateSpeedBytesPerSec), countStyle: .file) + "/s"
                            Text(speedStr)
                                .font(.caption2)
                                .foregroundColor(Color("mdmBlue", bundle: nil))
                        }
                    }

                    Spacer()

                    HStack(spacing: 12) {
                        switch playlist.state {
                        case .downloading:
                            Button(action: onPause) {
                                Image(systemName: "pause.fill")
                                    .foregroundColor(.primary)
                            }
                        case .paused, .queued, .ready, .created:
                            Button(action: onResume) {
                                Image(systemName: "play.fill")
                                    .foregroundColor(.primary)
                            }
                        case .completedWithErrors, .failed:
                            Button(action: onRetryFailed) {
                                Image(systemName: "arrow.clockwise")
                                    .foregroundColor(Color("mdmBlue", bundle: nil))
                            }
                        default:
                            EmptyView()
                        }

                        Button(action: onRemove) {
                            Image(systemName: "trash")
                                .foregroundColor(.red)
                        }
                    }
                }
            }
            .padding(16)
            .background(Color(uiColor: .secondarySystemGroupedBackground))
            .cornerRadius(12)
            .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 2)
        }
        .buttonStyle(.plain)
    }

    private var stateColor: Color {
        switch playlist.state {
        case .downloading:
            return Color.green
        case .completed:
            return Color.blue
        case .failed:
            return Color.red
        default:
            return Color.blue
        }
    }
}

public struct PlaylistStateBadgeView: View {
    public let state: PlaylistState

    public var body: some View {
        Text(title)
            .font(.system(size: 11, weight: .bold))
            .foregroundColor(color)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(color.opacity(0.12))
            .cornerRadius(6)
    }

    private var title: String {
        switch state {
        case .created, .ready: return "Ready"
        case .scanning: return "Scanning…"
        case .queued: return "Queued"
        case .downloading: return "Downloading"
        case .paused: return "Paused"
        case .completed: return "Completed"
        case .completedWithErrors: return "Errors"
        case .failed: return "Failed"
        case .cancelled: return "Cancelled"
        }
    }

    private var color: Color {
        switch state {
        case .created, .ready: return .blue
        case .scanning: return .purple
        case .queued, .paused, .completedWithErrors: return .orange
        case .downloading: return .green
        case .completed: return .blue
        case .failed: return .red
        case .cancelled: return .gray
        }
    }
}

public struct PlaylistDetailSheet: View {
    public let playlist: PlaylistTask
    @Environment(\.presentationMode) var presentationMode

    public var body: some View {
        NavigationView {
            VStack(alignment: .leading, spacing: 16) {
                // Header progress summary
                VStack(alignment: .leading, spacing: 8) {
                    Text(playlist.title)
                        .font(.title2)
                        .fontWeight(.bold)
                        .lineLimit(2)

                    Text("\(playlist.completedItems) / \(playlist.totalItems) items completed")
                        .font(.subheadline)
                        .foregroundColor(.secondary)

                    ProgressView(value: playlist.progress)
                        .progressViewStyle(LinearProgressViewStyle(tint: .blue))
                }
                .padding(.horizontal)

                Divider()

                Text("Items")
                    .font(.headline)
                    .padding(.horizontal)

                List(playlist.items) { item in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("\(item.index). \(item.title)")
                                .font(.body)
                                .fontWeight(.medium)
                                .lineLimit(1)

                            Text(item.filename)
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .lineLimit(1)
                        }

                        Spacer()

                        Text(itemStatusText(item.status))
                            .font(.caption2)
                            .fontWeight(.bold)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.secondary.opacity(0.12))
                            .cornerRadius(4)
                    }
                }
                .listStyle(.plain)
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
            }
        }
    }

    private func itemStatusText(_ status: PlaylistItemStatus) -> String {
        switch status {
        case .pending: return "Pending"
        case .active: return "Downloading"
        case .paused: return "Paused"
        case .completed: return "Done"
        case .failed: return "Failed"
        case .skipped: return "Skipped"
        case .cancelled: return "Cancelled"
        }
    }
}
