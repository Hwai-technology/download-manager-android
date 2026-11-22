import SwiftUI

public struct DownloadListView: View {
    @StateObject private var viewModel: DownloadListViewModel
    @State private var showingAddSheet = false

    public init(viewModel: DownloadListViewModel) {
        _viewModel = StateObject(wrappedValue: viewModel)
    }

    public var body: some View {
        NavigationView {
            Group {
                if viewModel.downloads.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "arrow.down.circle")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary)
                        Text("No active downloads")
                            .font(.headline)
                            .foregroundColor(.secondary)
                    }
                } else {
                    List {
                        ForEach(viewModel.downloads) { task in
                            DownloadRowView(
                                task: task,
                                onPause: { viewModel.pauseDownload(id: task.id) },
                                onResume: { viewModel.resumeDownload(id: task.id) },
                                onCancel: { viewModel.cancelDownload(id: task.id) },
                                onRemove: { viewModel.removeDownload(id: task.id) }
                            )
                        }
                    }
                    .listStyle(InsetGroupedListStyle())
                }
            }
            .navigationTitle("Downloads")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showingAddSheet = true }) {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showingAddSheet) {
                AddDownloadSheet { url, dest in
                    viewModel.addDownload(url: url, destPath: dest)
                }
            }
        }
    }
}

struct DownloadRowView: View {
    let task: DownloadTask
    let onPause: () -> Void
    let onResume: () -> Void
    let onCancel: () -> Void
    let onRemove: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(task.fileName)
                .font(.headline)
                .lineLimit(1)

            HStack {
                Text(stateTitle(task.state))
                    .font(.caption)
                    .foregroundColor(.secondary)
                Spacer()
                if task.speedBytesPerSec > 0 {
                    Text("\(task.speedBytesPerSec / 1024) KB/s | \(task.downloadedBytes) B")
                        .font(.caption)
                        .foregroundColor(.secondary)
                } else {
                    Text("\(task.downloadedBytes) B")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }

            if let error = task.error {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
            }

            ProgressView(value: task.progress)

            HStack {
                Spacer()
                if task.state == .downloading || task.state == .probing {
                    Button(action: onPause) {
                        Image(systemName: "pause.fill")
                    }
                    .buttonStyle(BorderlessButtonStyle())
                } else if task.state == .paused || task.state == .waiting {
                    Button(action: onResume) {
                        Image(systemName: "play.fill")
                    }
                    .buttonStyle(BorderlessButtonStyle())
                }
                
                if task.state == .done || task.state == .failed || task.state == .cancelled {
                    Button(action: onRemove) {
                        Image(systemName: "trash")
                            .foregroundColor(.red)
                    }
                    .buttonStyle(BorderlessButtonStyle())
                } else {
                    Button(action: onCancel) {
                        Image(systemName: "xmark")
                            .foregroundColor(.red)
                    }
                    .buttonStyle(BorderlessButtonStyle())
                }
            }
        }
        .padding(.vertical, 4)
    }

    private func stateTitle(_ state: DownloadState) -> String {
        switch state {
        case .probing: return "Connecting..."
        case .downloading: return "Downloading"
        case .verifying: return "Verifying"
        case .completing: return "Completing"
        case .done: return "Done"
        case .failed: return "Failed"
        case .cancelled: return "Cancelled"
        case .paused: return "Paused"
        case .waiting: return "Waiting"
        }
    }
}

struct AddDownloadSheet: View {
    @Environment(\.presentationMode) var presentationMode
    @State private var url: String = ""
    @State private var fileName: String = "download.bin"
    let onAdd: (String, String) -> Void

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Download URL")) {
                    TextField("https://example.com/file.zip", text: $url)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                }
                Section(header: Text("Save As")) {
                    TextField("File Name", text: $fileName)
                }
            }
            .navigationTitle("New Download")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        guard !url.isEmpty else { return }
                        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
                        let dest = docs.appendingPathComponent(fileName).path
                        onAdd(url, dest)
                        presentationMode.wrappedValue.dismiss()
                    }
                }
            }
        }
    }
}
