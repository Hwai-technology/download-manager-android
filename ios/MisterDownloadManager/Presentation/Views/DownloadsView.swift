import SwiftUI

public enum StatusFilter: String, CaseIterable, Identifiable {
    case all = "All"
    case active = "Active"
    case completed = "Completed"
    case queued = "Queued"
    case failed = "Failed"

    public var id: String { rawValue }
}

public struct DownloadsView: View {
    @ObservedObject var viewModel: DownloadListViewModel
    @State private var selectedStatus: StatusFilter = .all
    @State private var searchQuery: String = ""
    @State private var showingAddSheet = false
    @State private var previewUrl: URL?

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
                    List {
                        ForEach(filteredDownloads) { task in
                            DownloadRowView(
                                task: task,
                                onPause: { viewModel.pauseDownload(id: task.id) },
                                onResume: { viewModel.resumeDownload(id: task.id) },
                                onCancel: { viewModel.cancelDownload(id: task.id) },
                                onRemove: { viewModel.removeDownload(id: task.id) },
                                onOpen: {
                                    if FileManager.default.fileExists(atPath: task.destPath) {
                                        previewUrl = URL(fileURLWithPath: task.destPath)
                                    }
                                }
                            )
                        }
                    }
                    .listStyle(InsetGroupedListStyle())
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
                AddDownloadSheet { url, dest in
                    viewModel.addDownload(url: url, destPath: dest)
                }
            }
            .quickLookPreview($previewUrl)
        }
    }
}
