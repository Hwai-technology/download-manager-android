import SwiftUI

public struct SpeedometerGaugeView: View {
    let speedBytesPerSec: Int64
    let activeCount: Int
    let completedCount: Int
    let queuedCount: Int
    let totalDownloadedBytes: Int64

    private var formattedSpeed: (value: String, unit: String) {
        if speedBytesPerSec >= 1024 * 1024 {
            let mb = Double(speedBytesPerSec) / (1024.0 * 1024.0)
            return (String(format: "%.1f", mb), "MB/s")
        } else if speedBytesPerSec >= 1024 {
            let kb = Double(speedBytesPerSec) / 1024.0
            return (String(format: "%.0f", kb), "KB/s")
        } else {
            return (speedBytesPerSec > 0 ? "\(speedBytesPerSec)" : "0", "B/s")
        }
    }

    private var speedFraction: Double {
        let maxRefSpeed: Double = 50.0 * 1024.0 * 1024.0 // 50 MB/s reference scale
        return min(max(Double(speedBytesPerSec) / maxRefSpeed, 0.0), 1.0)
    }

    public var body: some View {
        VStack(spacing: 16) {
            // Speedometer Arc
            ZStack {
                // Background Track Arc
                Circle()
                    .trim(from: 0.5, to: 1.0)
                    .stroke(
                        Color.secondary.opacity(0.2),
                        style: StrokeStyle(lineWidth: 16, lineCap: .round)
                    )
                    .frame(width: 220, height: 220)
                    .rotationEffect(.degrees(0))

                // Active Speed Gradient Arc
                Circle()
                    .trim(from: 0.5, to: 0.5 + (0.5 * speedFraction))
                    .stroke(
                        MDMTheme.downloadAccelerationGradient,
                        style: StrokeStyle(lineWidth: 16, lineCap: .round)
                    )
                    .frame(width: 220, height: 220)
                    .rotationEffect(.degrees(0))
                    .animation(.easeInOut(duration: 0.5), value: speedFraction)

                // Center Speed Text
                VStack(spacing: 2) {
                    Text(formattedSpeed.value)
                        .font(.system(size: 38, weight: .bold, design: .rounded))
                        .foregroundColor(.primary)
                    Text(formattedSpeed.unit)
                        .font(.caption.bold())
                        .foregroundColor(MDMTheme.speedCyan)
                    Text("ACTIVE ACCELERATION")
                        .font(.system(size: 9, weight: .semibold))
                        .foregroundColor(.secondary)
                        .padding(.top, 4)
                }
                .offset(y: -20)
            }
            .frame(height: 130)

            // Statistics Grid (Cards)
            HStack(spacing: 10) {
                StatCard(title: "Active", count: "\(activeCount)", color: MDMTheme.stateDownloading, icon: "arrow.down")
                StatCard(title: "Queued", count: "\(queuedCount)", color: MDMTheme.stateWaiting, icon: "clock")
                StatCard(title: "Done", count: "\(completedCount)", color: MDMTheme.stateDone, icon: "checkmark.circle")
            }
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(UIColor.secondarySystemGroupedBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.secondary.opacity(0.15), lineWidth: 1)
        )
    }
}

struct StatCard: View {
    let title: String
    let count: String
    let color: Color
    let icon: String

    var body: some View {
        VStack(spacing: 6) {
            HStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.caption2)
                    .foregroundColor(color)
                Text(title)
                    .font(.caption2.bold())
                    .foregroundColor(.secondary)
            }
            Text(count)
                .font(.title3.bold())
                .foregroundColor(.primary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .background(color.opacity(0.1))
        .cornerRadius(10)
    }
}
