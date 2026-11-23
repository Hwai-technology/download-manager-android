import SwiftUI

public struct DeleteConfirmationDialog: View {
    let task: DownloadTask
    let onConfirm: () -> Void
    let onDismiss: () -> Void

    public var body: some View {
        // Presented via standard SwiftUI .alert
        EmptyView()
    }
}
