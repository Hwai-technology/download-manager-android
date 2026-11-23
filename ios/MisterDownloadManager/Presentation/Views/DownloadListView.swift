import SwiftUI
import QuickLook

public struct DownloadListView: View {
    @StateObject private var viewModel: DownloadListViewModel
    @State private var showingAddSheet = false
    @State private var showingSettingsSheet = false
    @State private var previewUrl: URL?

    public init(viewModel: DownloadListViewModel) {
        _viewModel = StateObject(wrappedValue: viewModel)
    }

    public var body: some View {
        NavigationView {
            Group {
                if viewModel.downloads.isEmpty {
                    VStack(spacing: 14) {
                        Image(systemName: "arrow.down.circle.fill")
                            .font(.system(size: 56))
                            .foregroundColor(MDMTheme.electricBlue)
                        Text("No Active Transfers")
                            .font(.title3.bold())
                            .foregroundColor(.primary)
                        Text("Tap + to add a download URL")
                            .font(.subheadline)
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
                                onRemove: { viewModel.removeDownload(id: task.id) },
                                onOpen: {
                                    let url = URL(fileURLWithPath: task.destPath)
                                    if FileManager.default.fileExists(atPath: task.destPath) {
                                        previewUrl = url
                                    }
                                }
                            )
                        }
                    }
                    .listStyle(InsetGroupedListStyle())
                }
            }
            .navigationTitle("Mister Download Manager")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { showingSettingsSheet = true }) {
                        Image(systemName: "gearshape")
                            .foregroundColor(MDMTheme.electricBlue)
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showingAddSheet = true }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.title3)
                            .foregroundColor(MDMTheme.electricBlue)
                    }
                }
            }
            .sheet(isPresented: $showingAddSheet) {
                AddDownloadSheet { url, dest in
                    viewModel.addDownload(url: url, destPath: dest)
                }
            }
            .sheet(isPresented: $showingSettingsSheet) {
                IOSSettingsSheet()
            }
            .quickLookPreview($previewUrl)
        }
    }
}

struct DownloadRowView: View {
    let task: DownloadTask
    let onPause: () -> Void
    let onResume: () -> Void
    let onCancel: () -> Void
    let onRemove: () -> Void
    let onOpen: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Circle()
                    .fill(stateColor(task.state))
                    .frame(width: 8, height: 8)
                Text(task.fileName)
                    .font(.headline)
                    .lineLimit(1)
                Spacer()
                Text(stateTitle(task.state))
                    .font(.caption.bold())
                    .foregroundColor(stateColor(task.state))
            }

            HStack {
                if task.speedBytesPerSec > 0 {
                    Text("\(task.speedBytesPerSec / 1024) KB/s")
                        .font(.caption.monospacedDigit())
                        .foregroundColor(MDMTheme.speedCyan)
                    Text("•")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Text("\(task.downloadedBytes) bytes")
                    .font(.caption.monospacedDigit())
                    .foregroundColor(.secondary)
            }

            if let error = task.error {
                Text(error)
                    .font(.caption)
                    .foregroundColor(MDMTheme.stateFailed)
            }

            ProgressView(value: task.progress)
                .accentColor(stateColor(task.state))

            HStack {
                if task.state == .done {
                    Button(action: onOpen) {
                        Label("Open File", systemImage: "doc.text.magnifyingglass")
                            .font(.caption.bold())
                    }
                    .buttonStyle(BorderlessButtonStyle())
                    .foregroundColor(MDMTheme.electricBlue)
                }

                Spacer()

                if task.state == .downloading || task.state == .probing {
                    Button(action: onPause) {
                        Image(systemName: "pause.fill")
                            .foregroundColor(.secondary)
                    }
                    .buttonStyle(BorderlessButtonStyle())
                } else if task.state == .paused || task.state == .waiting {
                    Button(action: onResume) {
                        Image(systemName: "play.fill")
                            .foregroundColor(MDMTheme.electricBlue)
                    }
                    .buttonStyle(BorderlessButtonStyle())
                }
                
                if task.state == .done || task.state == .failed || task.state == .cancelled {
                    Button(action: onRemove) {
                        Image(systemName: "trash")
                            .foregroundColor(MDMTheme.stateFailed)
                    }
                    .buttonStyle(BorderlessButtonStyle())
                } else {
                    Button(action: onCancel) {
                        Image(systemName: "xmark")
                            .foregroundColor(MDMTheme.stateFailed)
                    }
                    .buttonStyle(BorderlessButtonStyle())
                }
            }
        }
        .padding(.vertical, 6)
    }

    private func stateColor(_ state: DownloadState) -> Color {
        switch state {
        case .downloading: return MDMTheme.stateDownloading
        case .done: return MDMTheme.stateDone
        case .paused: return MDMTheme.statePaused
        case .failed: return MDMTheme.stateFailed
        case .probing, .verifying, .completing: return MDMTheme.stateProbing
        case .cancelled, .waiting: return MDMTheme.stateWaiting
        }
    }

    private func stateTitle(_ state: DownloadState) -> String {
        switch state {
        case .probing: return "Connecting"
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
    @State private var isCustomName: Bool = false
    let onAdd: (String, String) -> Void

    private var isYoutubePlaylist: Bool {
        let u = url.lowercased()
        return (u.contains("youtube.com") || u.contains("youtu.be")) && (u.contains("playlist") || u.contains("list="))
    }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Download URL")) {
                    TextField("https://example.com/file.zip", text: $url)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                        .onChange(of: url) { newUrl in
                            if !isCustomName {
                                let detected = sanitizeFileName(extractFileName(from: newUrl))
                                fileName = detected
                            }
                        }

                    if isYoutubePlaylist {
                        HStack(spacing: 6) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(MDMTheme.stateFailed)
                            Text("YouTube playlists cannot be downloaded as raw files. Direct files or Desktop extraction are required.")
                                .font(.caption)
                                .foregroundColor(MDMTheme.stateFailed)
                        }
                    }
                }

                Section(header: Text("Save As")) {
                    TextField("File Name", text: $fileName)
                        .onChange(of: fileName) { _ in
                            isCustomName = true
                        }
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
                        let cleanName = sanitizeFileName(fileName)
                        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
                        let dest = docs.appendingPathComponent(cleanName).path
                        onAdd(url, dest)
                        presentationMode.wrappedValue.dismiss()
                    }
                    .disabled(url.isEmpty)
                }
            }
        }
    }

    private func extractFileName(from urlString: String) -> String {
        let clean = urlString.components(separatedBy: "?")[0].components(separatedBy: "#")[0]
        let name = (clean as NSString).lastPathComponent
        return name.contains(".") ? name : "download.bin"
    }

    private func sanitizeFileName(_ name: String) -> String {
        let illegal = CharacterSet(charactersIn: "/\\:*?\"<>|")
        var cleaned = name.components(separatedBy: illegal).joined(separator: "_")
        cleaned = cleaned.replacingOccurrences(of: "..", with: "")
        cleaned = cleaned.trimmingCharacters(in: .whitespacesAndNewlines)
        return cleaned.isEmpty ? "download.bin" : cleaned
    }
}

struct IOSSettingsSheet: View {
    @Environment(\.presentationMode) var presentationMode

    var body: some View {
        NavigationView {
            List {
                Section(header: Text("About")) {
                    HStack {
                        Image(systemName: "info.circle")
                            .foregroundColor(MDMTheme.electricBlue)
                        Text("Mister Download Manager")
                        Spacer()
                        Text("v0.5.2")
                            .foregroundColor(.secondary)
                    }

                    Link(destination: URL(string: "https://github.com/Hwai-technology/download-manager-android")!) {
                        HStack {
                            Image(systemName: "link")
                                .foregroundColor(MDMTheme.electricBlue)
                            Text("GitHub Repository")
                            Spacer()
                            Image(systemName: "arrow.up.right.square")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }

                    Link(destination: URL(string: "https://www.gnu.org/licenses/gpl-3.0.en.html")!) {
                        HStack {
                            Image(systemName: "doc.text")
                                .foregroundColor(MDMTheme.electricBlue)
                            Text("Open-source License (GPLv3)")
                            Spacer()
                            Image(systemName: "arrow.up.right.square")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("Settings")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
            }
        }
    }
}

