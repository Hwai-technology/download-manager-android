import SwiftUI

public struct MainTabView: View {
    @ObservedObject var viewModel: DownloadListViewModel
    @State private var selectedTab: Int = 0

    public init(viewModel: DownloadListViewModel) {
        self.viewModel = viewModel
    }

    public var body: some View {
        TabView(selection: $selectedTab) {
            DashboardView(viewModel: viewModel, selectedTab: $selectedTab)
                .tabItem {
                    Label("Dashboard", systemImage: "gauge.with.needle")
                }
                .tag(0)

            DownloadsView(viewModel: viewModel)
                .tabItem {
                    Label("Downloads", systemImage: "arrow.down.circle")
                }
                .tag(1)

            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape")
                }
                .tag(2)
        }
        .accentColor(MDMTheme.electricBlue)
    }
}
