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

                    // Active Transfers Header
                    HStack {
                        Text("Active Transfers")
                            .font(.headline)
                            .foregroundColor(.primary)
                        Spacer()
                        Button(action: { selectedTab = 1 }) {
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
