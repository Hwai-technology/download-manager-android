import SwiftUI
import UIKit
import UserNotifications

public struct MainTabView: View {
    @ObservedObject var viewModel: DownloadListViewModel
    @Binding var pendingUrl: String?
    @State private var selectedTab: Int = 0
    @State private var showingAddSheet = false
    @State private var addInitialUrl: String = ""
    @State private var lastClipUrl: String? = nil

    public init(viewModel: DownloadListViewModel, pendingUrl: Binding<String?> = .constant(nil)) {
        self.viewModel = viewModel
        self._pendingUrl = pendingUrl
    }

    public var body: some View {
        TabView(selection: $selectedTab) {
            SpeedTestScreen()
                .tabItem {
                    Label("Speed Test", systemImage: "network")
                }
                .tag(0)

            DashboardScreen(viewModel: viewModel, selectedTab: $selectedTab)
                .tabItem {
                    Label("Dashboard", systemImage: "gauge.with.needle")
                }
                .tag(1)

            DownloadsScreen(viewModel: viewModel)
                .tabItem {
                    Label("Downloads", systemImage: "arrow.down.circle")
                }
                .tag(2)

            PlaylistsScreen(viewModel: viewModel)
                .tabItem {
                    Label("Playlists", systemImage: "list.bullet.rectangle")
                }
                .tag(3)

            SettingsScreen()
                .tabItem {
                    Label("Settings", systemImage: "gearshape")
                }
                .tag(4)
        }
        .accentColor(MDMTheme.electricBlue)
        .sheet(isPresented: $showingAddSheet) {
            AddDownloadDialog(initialUrl: addInitialUrl) { url, dest in
                viewModel.addDownload(url: url, destPath: dest)
            }
        }
        .onChange(of: pendingUrl) { newUrl in
            if let url = newUrl, !url.isEmpty {
                addInitialUrl = url
                showingAddSheet = true
                pendingUrl = nil
            }
        }
        .onAppear {
            checkClipboard()
        }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
            checkClipboard()
        }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
            checkClipboard()
        }
    }

    private func checkClipboard() {
        // First check App Group shared defaults for incoming links shared from Share Extension
        if let sharedDefaults = UserDefaults(suiteName: "group.com.misterdownloadmanager.shared"),
           let sharedUrl = sharedDefaults.string(forKey: "pending_shared_url"), !sharedUrl.isEmpty {
            sharedDefaults.removeObject(forKey: "pending_shared_url")
            sharedDefaults.synchronize()
            addInitialUrl = sharedUrl
            showingAddSheet = true
            return
        }

        let isEnabled = UserDefaults.standard.object(forKey: "pref_clipboard_detection") as? Bool ?? true
        guard isEnabled else { return }

        guard let clipString = UIPasteboard.general.string, !clipString.isEmpty else { return }
        guard let url = AddDownloadDialog.extractFirstUrl(from: clipString) else { return }

        if url != lastClipUrl {
            lastClipUrl = url
            showClipboardNotification(for: url)
        }
    }

    private func formatShortUrlPreview(_ urlString: String, maxLength: Int = 45) -> String {
        guard let url = URL(string: urlString), let host = url.host, !host.isEmpty else {
            return urlString.count > maxLength ? String(urlString.prefix(maxLength - 3)) + "..." : urlString
        }
        let path = url.path
        let cleanBase = (path.isEmpty || path == "/") ? host : "\(host)\(path)"
        if cleanBase.count <= maxLength {
            return cleanBase
        } else {
            let headLen = max(10, maxLength - 12)
            return "\(cleanBase.prefix(headLen))...\(cleanBase.suffix(9))"
        }
    }

    private func showClipboardNotification(for url: String) {
        let content = UNMutableNotificationContent()
        content.title = NSLocalizedString("clipboard_notif_title", value: "New link detected", comment: "Title for clipboard link copied notification")
        let preview = formatShortUrlPreview(url, maxLength: 45)
        let baseMsg = NSLocalizedString("clipboard_notif_message", value: "Would you like to download this link?", comment: "Message asking if user wants to download copied link")
        content.body = "\(baseMsg)\n\u{200E}\(preview)"
        content.sound = .default
        content.categoryIdentifier = "CLIPBOARD_LINK_CATEGORY"
        content.userInfo = ["url": url]

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 0.5, repeats: false)
        let request = UNNotificationRequest(identifier: "mdm_clip_\(url.hashValue)", content: content, trigger: trigger)
        UNUserNotificationCenter.current().add(request)
    }
}
