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
                    let downMb = Double(task.downloadedBytes) / 1_000_000.0
                    DetailRow(title: "Downloaded", value: String(format: "%.2f MB", downMb))
                    if task.totalBytes > 0 {
                        let totMb = Double(task.totalBytes) / 1_000_000.0
                        DetailRow(title: "Total Size", value: String(format: "%.2f MB", totMb))
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
