import SwiftUI

public struct DownloadCard: View {
    let task: DownloadTask
    let onPause: () -> Void
    let onResume: () -> Void
    let onCancel: () -> Void
    let onRemove: () -> Void
    let onOpen: () -> Void
    let onDetails: () -> Void

    private var ext: String {
        let name = task.fileName
        if let idx = name.lastIndex(of: ".") {
            return String(name[name.index(after: idx)...]).uppercased()
        }
        return "FILE"
    }

    private var isVideo: Bool {
        let lower = task.fileName.lowercased()
        return ["mp4", "mkv", "webm", "mov", "avi", "3gp"].contains { lower.hasSuffix($0) }
    }

    private var formattedSpeed: String {
        if task.speedBytesPerSec >= 1024 * 1024 {
            return String(format: "%.1f MB/s", Double(task.speedBytesPerSec) / (1024.0 * 1024.0))
        } else if task.speedBytesPerSec > 0 {
            return "\(task.speedBytesPerSec / 1024) KB/s"
        }
        return ""
    }

    private var stateColor: Color {
        switch task.state {
        case .downloading: return MDMTheme.stateDownloading
        case .done: return MDMTheme.stateDone
        case .paused: return MDMTheme.statePaused
        case .failed: return MDMTheme.stateFailed
        case .probing, .verifying, .completing: return MDMTheme.stateProbing
        case .cancelled, .waiting: return MDMTheme.stateWaiting
        }
    }

    private var stateLabel: String {
        switch task.state {
        case .probing: return "Connecting"
        case .downloading: return "Downloading"
        case .verifying: return "Verifying"
        case .completing: return "Completing"
        case .done: return "Done"
        case .failed: return "Failed"
        case .cancelled: return "Cancelled"
        case .paused: return "Paused"
        case .waiting: return "Waiting"
        }
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            // Header Row: File Extension badge & title
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(stateColor.opacity(0.12))
                        .frame(width: 44, height: 44)
                    Text(ext.prefix(4))
                        .font(.system(size: 11, weight: .bold, design: .rounded))
                        .foregroundColor(stateColor)
                }

                VStack(alignment: .leading, spacing: 3) {
                    Text(task.fileName)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.primary)
                        .lineLimit(1)

                    HStack(spacing: 6) {
                        Circle()
                            .fill(stateColor)
                            .frame(width: 6, height: 6)
                        Text(stateLabel)
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(stateColor)

                        Text("• \(task.downloadedBytes) B")
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)

                        if !formattedSpeed.isEmpty {
                            Text("• \(formattedSpeed)")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(MDMTheme.speedCyan)
                        }
                    }
                }

                Spacer()

                Button(action: onDetails) {
                    Image(systemName: "ellipsis")
                        .foregroundColor(.secondary)
                        .padding(8)
                }
                .buttonStyle(BorderlessButtonStyle())
            }

            // Progress Bar
            ProgressView(value: task.progress)
                .accentColor(stateColor)

            // Action Buttons Row
            HStack {
                if task.state == .done {
                    Button(action: onOpen) {
                        Label(isVideo ? "Play" : "Open", systemImage: isVideo ? "play.circle.fill" : "folder.fill")
                            .font(.system(size: 13, weight: .semibold))
                    }
                    .buttonStyle(BorderlessButtonStyle())
                    .foregroundColor(MDMTheme.electricBlue)
                }

                Spacer()

                if task.state == .downloading || task.state == .probing {
                    Button(action: onPause) {
                        Image(systemName: "pause.fill")
                            .foregroundColor(.secondary)
                    }
                    .buttonStyle(BorderlessButtonStyle())
                } else if task.state == .paused || task.state == .waiting {
                    Button(action: onResume) {
                        Image(systemName: "play.fill")
                            .foregroundColor(MDMTheme.electricBlue)
                    }
                    .buttonStyle(BorderlessButtonStyle())
                }

                if task.state == .done || task.state == .failed || task.state == .cancelled {
                    Button(action: onRemove) {
                        Image(systemName: "trash")
                            .foregroundColor(MDMTheme.stateFailed)
                    }
                    .buttonStyle(BorderlessButtonStyle())
                } else {
                    Button(action: onCancel) {
                        Image(systemName: "xmark")
                            .foregroundColor(MDMTheme.stateFailed)
                    }
                    .buttonStyle(BorderlessButtonStyle())
                }
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
