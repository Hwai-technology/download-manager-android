import SwiftUI
import QuickLook

public enum StatusFilter: String, CaseIterable, Identifiable {
    case all = "All"
    case active = "Active"
    case completed = "Completed"
    case queued = "Queued"
    case failed = "Failed"

    public var id: String { rawValue }
}

public struct DownloadsScreen: View {
    @ObservedObject var viewModel: DownloadListViewModel
    @State private var selectedStatus: StatusFilter = .all
    @State private var searchQuery: String = ""
    @State private var showingAddSheet = false
    @State private var previewUrl: URL?
    @State private var taskForDetails: DownloadTask?
    @State private var taskToDelete: DownloadTask?

    private var filteredDownloads: [DownloadTask] {
        viewModel.downloads.filter { task in
            let matchesSearch = searchQuery.isEmpty ||
                task.fileName.localizedCaseInsensitiveContains(searchQuery) ||
                task.url.localizedCaseInsensitiveContains(searchQuery)

            let matchesStatus: Bool
            switch selectedStatus {
            case .all:
                matchesStatus = true
            case .active:
                matchesStatus = task.state == .downloading || task.state == .probing
            case .completed:
                matchesStatus = task.state == .done
            case .queued:
                matchesStatus = task.state == .waiting || task.state == .paused
            case .failed:
                matchesStatus = task.state == .failed || task.state == .cancelled
            }

            return matchesSearch && matchesStatus
        }
    }

    public var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Search Bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    TextField("Search downloads...", text: $searchQuery)
                    if !searchQuery.isEmpty {
                        Button(action: { searchQuery = "" }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding(10)
                .background(Color(UIColor.secondarySystemGroupedBackground))
                .cornerRadius(10)
                .padding(.horizontal, 16)
                .padding(.top, 8)

                // Status Filter Segmented Control
                Picker("Status", selection: $selectedStatus) {
                    ForEach(StatusFilter.allCases) { filter in
                        Text(filter.rawValue).tag(filter)
                    }
                }
                .pickerStyle(SegmentedPickerStyle())
                .padding(16)

                // List of Downloads
                if filteredDownloads.isEmpty {
                    Spacer()
                    VStack(spacing: 12) {
                        Image(systemName: "tray")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary)
                        Text("No Downloads Found")
                            .font(.headline)
                            .foregroundColor(.secondary)
                    }
                    Spacer()
                } else {
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(filteredDownloads) { task in
                                DownloadCard(
                                    task: task,
                                    onPause: { viewModel.pauseDownload(id: task.id) },
                                    onResume: { viewModel.resumeDownload(id: task.id) },
                                    onCancel: { viewModel.cancelDownload(id: task.id) },
                                    onRemove: { taskToDelete = task },
                                    onOpen: {
                                        if FileManager.default.fileExists(atPath: task.destPath) {
                                            previewUrl = URL(fileURLWithPath: task.destPath)
                                        }
                                    },
                                    onDetails: {
                                        taskForDetails = task
                                    }
                                )
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 24)
                    }
                }
            }
            .navigationTitle("Downloads")
            .background(Color(UIColor.systemGroupedBackground).ignoresSafeArea())
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showingAddSheet = true }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.title3)
                            .foregroundColor(MDMTheme.electricBlue)
                    }
                }
            }
            .sheet(isPresented: $showingAddSheet) {
                AddDownloadDialog(
                    onAdd: { url, dest in
                        viewModel.addDownload(url: url, destPath: dest)
                    },
                    onAddPlaylist: { url, destDir, title in
                        viewModel.addPlaylist(url: url, destDir: destDir, title: title)
                    }
                )
            }
            .sheet(item: $taskForDetails) { task in
                DownloadDetailsDialog(task: task)
            }
            .alert(item: $taskToDelete) { task in
                Alert(
                    title: Text("Remove Download"),
                    message: Text("Are you sure you want to remove \"\(task.fileName)\" from your download list?"),
                    primaryButton: .destructive(Text("Remove")) {
                        viewModel.removeDownload(id: task.id)
                    },
                    secondaryButton: .cancel()
                )
            }
            .quickLookPreview($previewUrl)
        }
    }
}

