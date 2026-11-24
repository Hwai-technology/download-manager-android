import SwiftUI

public struct MainTabView: View {
    @ObservedObject var viewModel: DownloadListViewModel
    @State private var selectedTab: Int = 0

    public init(viewModel: DownloadListViewModel) {
        self.viewModel = viewModel
    }

    public var body: some View {
        TabView(selection: $selectedTab) {
            DashboardScreen(viewModel: viewModel, selectedTab: $selectedTab)
                .tabItem {
                    Label("Dashboard", systemImage: "gauge.with.needle")
                }
                .tag(0)

            DownloadsScreen(viewModel: viewModel)
                .tabItem {
                    Label("Downloads", systemImage: "arrow.down.circle")
                }
                .tag(1)

            PlaylistsScreen(viewModel: viewModel)
                .tabItem {
                    Label("Playlists", systemImage: "list.bullet.rectangle")
                }
                .tag(2)

            SpeedTestScreen()
                .tabItem {
                    Label("Speed Test", systemImage: "network")
                }
                .tag(3)

            SettingsScreen()
                .tabItem {
                    Label("Settings", systemImage: "gearshape")
                }
                .tag(4)
        }
        .accentColor(MDMTheme.electricBlue)
    }
}
