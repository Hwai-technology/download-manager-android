import SwiftUI
import UserNotifications
import UIKit

@main
struct MisterDownloadManagerApp: App {
    private let repository: DownloadRepositoryProtocol
    private let useCases: DownloadUseCases
    @StateObject private var viewModel: DownloadListViewModel
    @State private var pendingUrl: String? = nil

    init() {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let stateDir = docs.appendingPathComponent(".mdm_state").path
        try? FileManager.default.createDirectory(atPath: stateDir, withIntermediateDirectories: true)
        
        MdmMobileFFI.initialize(persistenceDir: stateDir)
        let repo = DownloadRepositoryImpl()
        let uc = DownloadUseCases(repository: repo)
        self.repository = repo
        self.useCases = uc
        _viewModel = StateObject(wrappedValue: DownloadListViewModel(useCases: uc))
        
        setupNotificationCategories()
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in }
    }

    private func setupNotificationCategories() {
        let downloadAction = UNNotificationAction(
            identifier: "ACTION_DOWNLOAD_NOW",
            title: NSLocalizedString("clipboard_dialog_download_action", value: "Download Now", comment: ""),
            options: [.foreground]
        )
        let openAction = UNNotificationAction(
            identifier: "ACTION_OPEN_APP",
            title: NSLocalizedString("clipboard_action_open", value: "Open App", comment: ""),
            options: [.foreground]
        )
        let laterAction = UNNotificationAction(
            identifier: "ACTION_LATER",
            title: NSLocalizedString("clipboard_action_later", value: "Later", comment: ""),
            options: [.destructive]
        )

        let category = UNNotificationCategory(
            identifier: "CLIPBOARD_LINK_CATEGORY",
            actions: [downloadAction, openAction, laterAction],
            intentIdentifiers: [],
            options: []
        )

        UNUserNotificationCenter.current().setNotificationCategories([category])
    }

    var body: some Scene {
        WindowGroup {
            MainTabView(viewModel: viewModel, pendingUrl: $pendingUrl)
                .onOpenURL { incomingUrl in
                    handleIncomingURL(incomingUrl)
                }
        }
    }

    private func handleIncomingURL(_ url: URL) {
        if url.scheme == "http" || url.scheme == "https" {
            pendingUrl = url.absoluteString
        } else if let components = URLComponents(url: url, resolvingAgainstBaseURL: false) {
            if let link = components.queryItems?.first(where: { $0.name == "url" || $0.name == "link" })?.value {
                pendingUrl = link
            }
        }
    }
}
