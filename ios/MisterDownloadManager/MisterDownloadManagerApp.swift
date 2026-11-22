import SwiftUI

@main
struct MisterDownloadManagerApp: App {
    private let repository: DownloadRepositoryProtocol
    private let useCases: DownloadUseCases
    @StateObject private var viewModel: DownloadListViewModel

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
    }

    var body: some Scene {
        WindowGroup {
            DownloadListView(viewModel: viewModel)
        }
    }
}
