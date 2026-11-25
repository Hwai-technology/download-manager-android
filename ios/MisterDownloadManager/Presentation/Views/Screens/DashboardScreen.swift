import SwiftUI

public struct DashboardView: View {
    @ObservedObject var viewModel: DownloadListViewModel
    @Binding var selectedTab: Int
    @State private var showingAddSheet = false

    private var activeDownloads: [DownloadTask] {
        viewModel.downloads.filter { $0.state == .downloading || $0.state == .probing }
    }

    private var queuedCount: Int {
        viewModel.downloads.filter { $0.state == .waiting || $0.state == .paused }.count
    }

    private var completedCount: Int {
        viewModel.downloads.filter { $0.state == .done }.count
    }

    private var totalSpeed: Int64 {
        viewModel.downloads.reduce(0) { $0 + $1.speedBytesPerSec }
    }

    private var totalDownloaded: Int64 {
        viewModel.downloads.reduce(0) { $0 + $1.downloadedBytes }
    }

    public var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 18) {
                    // Precision Speedometer Gauge
                    SpeedometerGaugeView(
                        speedBytesPerSec: totalSpeed,
                        activeCount: activeDownloads.count,
                        completedCount: completedCount,
                        queuedCount: queuedCount,
                        totalDownloadedBytes: totalDownloaded
                    )

                    // Storage Overview Section
                    StorageOverviewCard(totalDownloaded: totalDownloaded)

                    // Active Transfers Header
                    HStack {
                        Text("Active Transfers")
                            .font(.headline)
                            .foregroundColor(.primary)
                        Spacer()
                        Button(action: { selectedTab = 2 }) {
                            Text("View All")
                                .font(.subheadline.bold())
                                .foregroundColor(MDMTheme.electricBlue)
                        }
                    }
                    .padding(.horizontal, 4)

                    // Active Tasks or Empty State
                    if activeDownloads.isEmpty {
                        VStack(spacing: 10) {
                            Image(systemName: "bolt.horizontal.circle")
                                .font(.system(size: 42))
                                .foregroundColor(.secondary)
                            Text("No Active Accelerations")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 32)
                        .background(
                            RoundedRectangle(cornerRadius: 14)
                                .fill(Color(UIColor.secondarySystemGroupedBackground))
                        )
                    } else {
                        VStack(spacing: 12) {
                            ForEach(activeDownloads) { task in
                                DownloadCard(
                                    task: task,
                                    onPause: { viewModel.pauseDownload(id: task.id) },
                                    onResume: { viewModel.resumeDownload(id: task.id) },
                                    onCancel: { viewModel.cancelDownload(id: task.id) },
                                    onRemove: { viewModel.removeDownload(id: task.id) },
                                    onOpen: {},
                                    onDetails: {}
                                )
                            }
                        }
                    }
                }
                .padding(16)
            }
            .navigationTitle("Dashboard")
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
                AddDownloadDialog { url, dest in
                    viewModel.addDownload(url: url, destPath: dest)
                }
            }
        }
    }
}

struct StorageOverviewCard: View {
    let totalDownloaded: Int64

    private var storageInfo: (free: Int64, total: Int64) {
        if let attrs = try? FileManager.default.attributesOfFileSystem(forPath: NSHomeDirectory()),
           let freeSize = attrs[.systemFreeSize] as? Int64,
           let totalSize = attrs[.systemSize] as? Int64 {
            return (freeSize, totalSize)
        }
        return (0, 0)
    }

    private func formatStorageGb(_ bytes: Int64) -> String {
        let gb = Double(bytes) / 1_000_000_000.0
        return String(format: "%.2f GB", gb)
    }

    var body: some View {
        let (freeBytes, totalBytes) = storageInfo
        if totalBytes > 0 {
            let usedBytes = max(totalBytes - freeBytes, 0)
            let usedFraction = Double(usedBytes) / Double(totalBytes)

            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 8) {
                    Image(systemName: "internaldrive")
                        .foregroundColor(MDMTheme.electricBlue)
                        .font(.subheadline)
                    Text("Phone Storage Capacity")
                        .font(.headline)
                        .foregroundColor(.primary)
                    Spacer()
                    Text(formatStorageGb(totalBytes))
                        .font(.caption.bold())
                        .foregroundColor(MDMTheme.electricBlue)
                }

                ProgressView(value: usedFraction)
                    .accentColor(MDMTheme.electricBlue)

                HStack {
                    Text("Used: \(formatStorageGb(usedBytes))")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("Available: \(formatStorageGb(freeBytes))")
                        .font(.caption.bold())
                        .foregroundColor(MDMTheme.electricBlue)
                }
            }
            .padding(14)
            .background(
                RoundedRectangle(cornerRadius: 14)
                    .fill(Color(UIColor.secondarySystemGroupedBackground))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(Color.secondary.opacity(0.12), lineWidth: 1)
            )
        }
    }
}
