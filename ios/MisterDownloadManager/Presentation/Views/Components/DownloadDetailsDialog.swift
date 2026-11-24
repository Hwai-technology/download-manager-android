import SwiftUI

public struct DownloadDetailsDialog: View {
    let task: DownloadTask
    @Environment(\.presentationMode) var presentationMode

    public var body: some View {
        NavigationView {
            List {
                Section(header: Text("File Information")) {
                    DetailRow(title: "File Name", value: task.fileName)
                    DetailRow(title: "Status", value: "\(task.state)".capitalized)
                    DetailRow(title: "Downloaded", value: "\(task.downloadedBytes) bytes")
                    if let total = task.totalBytes {
                        DetailRow(title: "Total Size", value: "\(total) bytes")
                    }
                    if task.speedBytesPerSec > 0 {
                        DetailRow(title: "Speed", value: SpeedometerGaugeView.formatSpeed(task.speedBytesPerSec))
                    }
                }

                Section(header: Text("Storage Path")) {
                    Text(task.destPath)
                        .font(.system(.footnote, design: .monospaced))
                        .foregroundColor(.secondary)
                }

                Section(header: Text("Source URL")) {
                    Text(task.url)
                        .font(.system(.footnote, design: .monospaced))
                        .foregroundColor(.secondary)
                }
            }
            .navigationTitle("Task Details")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
            }
        }
    }
}

struct DetailRow: View {
    let title: String
    let value: String

    var body: some View {
        HStack {
            Text(title)
                .font(.body)
            Spacer()
            Text(value)
                .font(.body)
                .foregroundColor(.secondary)
        }
    }
}
