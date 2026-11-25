import SwiftUI

public struct SpeedTestScreen: View {
    @StateObject private var speedTestManager = SpeedTestManager()

    public init() {}

    private var targetSpeed: Double {
        switch speedTestManager.phase {
        case .download, .upload:
            return speedTestManager.currentSpeedMbps
        case .complete:
            return speedTestManager.downloadSpeedMbps
        default:
            return 0.0
        }
    }

    private var speedFraction: Double {
        return min(max(targetSpeed / 150.0, 0.0), 1.0)
    }

    private var isTesting: Bool {
        return speedTestManager.phase == .ping || speedTestManager.phase == .download || speedTestManager.phase == .upload
    }

    private var phaseStatus: (text: String, color: Color) {
        switch speedTestManager.phase {
        case .idle:
            return ("Ready to Test", MDMTheme.electricBlue)
        case .ping:
            return ("Measuring Ping & Jitter…", MDMTheme.orbitLime)
        case .download:
            return ("Testing Download Speed…", MDMTheme.stateDownloading)
        case .upload:
            return ("Testing Upload Speed…", MDMTheme.speedCyan)
        case .complete:
            return ("Test Completed", MDMTheme.stateDownloading)
        case .error(let msg):
            return (msg, MDMTheme.stateFailed)
        }
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Phase Status Badge
                Text(phaseStatus.text)
                    .font(.caption.bold())
                    .foregroundColor(phaseStatus.color)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 6)
                    .background(phaseStatus.color.opacity(0.12))
                    .overlay(
                        Capsule().stroke(phaseStatus.color.opacity(0.35), lineWidth: 1)
                    )
                    .clipShape(Capsule())
                    .padding(.top, 8)

                // Precision Speedometer Gauge Card
                VStack(spacing: 14) {
                    ZStack {
                        // Background Track Arc
                        Circle()
                            .trim(from: 0.5, to: 1.0)
                            .stroke(
                                Color.secondary.opacity(0.2),
                                style: StrokeStyle(lineWidth: 16, lineCap: .round)
                            )
                            .frame(width: 230, height: 230)

                        // Active Gradient Arc
                        Circle()
                            .trim(from: 0.5, to: 0.5 + (0.5 * speedFraction))
                            .stroke(
                                MDMTheme.downloadAccelerationGradient,
                                style: StrokeStyle(lineWidth: 16, lineCap: .round)
                            )
                            .frame(width: 230, height: 230)
                            .animation(.easeInOut(duration: 0.35), value: speedFraction)

                        // Digital Readout in Center
                        VStack(spacing: 2) {
                            Text(formatReadout())
                                .font(.system(size: 40, weight: .bold, design: .rounded))
                                .foregroundColor(.primary)
                            Text(speedTestManager.phase == .ping ? "ms" : "Mbps")
                                .font(.subheadline.bold())
                                .foregroundColor(MDMTheme.speedCyan)
                        }
                        .offset(y: -20)
                    }
                    .frame(height: 140)

                    // Linear Progress Bar
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            RoundedRectangle(cornerRadius: 3)
                                .fill(Color.secondary.opacity(0.2))
                                .frame(height: 6)

                            RoundedRectangle(cornerRadius: 3)
                                .fill(MDMTheme.downloadAccelerationGradient)
                                .frame(width: geo.size.width * CGFloat(speedTestManager.currentProgress), height: 6)
                                .animation(.linear(duration: 0.1), value: speedTestManager.currentProgress)
                        }
                    }
                    .frame(height: 6)
                    .padding(.horizontal, 12)
                }
                .padding(20)
                .background(
                    RoundedRectangle(cornerRadius: 18)
                        .fill(Color(UIColor.secondarySystemGroupedBackground))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 18)
                        .stroke(Color.secondary.opacity(0.15), lineWidth: 1)
                )

                // New Speed Result Rectangle: MBps (Megabytes per second)
                // Positioned directly below the gauge and directly above the four information boxes
                HStack(alignment: .center) {
                    // Left side: Large, prominent numerical download speed in MBps
                    VStack(alignment: .leading, spacing: 2) {
                        let currentMb: Double = {
                            if speedTestManager.phase == .download {
                                return speedTestManager.currentSpeedMBps
                            } else if speedTestManager.phase == .upload || speedTestManager.phase == .complete {
                                return speedTestManager.downloadSpeedMBps
                            }
                            return 0.0
                        }()

                        Text(currentMb > 0 ? String(format: "%.2f MBps", currentMb) : "-- MBps")
                            .font(.system(size: 24, weight: .heavy, design: .rounded))
                            .foregroundColor(MDMTheme.stateDownloading)
                    }

                    Spacer()

                    // Right side: Short explanation of the measured download speed
                    VStack(alignment: .trailing, spacing: 2) {
                        Text("Actual Download Speed")
                            .font(.subheadline.bold())
                            .foregroundColor(.primary)
                        Text("Real data transfer rate")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
                .background(
                    RoundedRectangle(cornerRadius: 14)
                        .fill(Color(UIColor.secondarySystemGroupedBackground))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(MDMTheme.stateDownloading.opacity(0.35), lineWidth: 1)
                )

                // 4 Metric Tiles (Ping, Jitter, Download, Upload)
                VStack(spacing: 8) {
                    HStack(spacing: 8) {
                        IOSSpeedMetricTile(
                            title: "Ping",
                            value: speedTestManager.pingMs > 0 ? String(format: "%.0f ms", speedTestManager.pingMs) : "--",
                            color: MDMTheme.stateWaiting,
                            systemImage: "network"
                        )
                        IOSSpeedMetricTile(
                            title: "Jitter",
                            value: speedTestManager.jitterMs > 0 ? String(format: "%.1f ms", speedTestManager.jitterMs) : "--",
                            color: MDMTheme.orbitLime,
                            systemImage: "waveform.path.ecg"
                        )
                    }

                    HStack(spacing: 8) {
                        IOSSpeedMetricTile(
                            title: "Download",
                            value: speedTestManager.downloadSpeedMbps > 0 ? String(format: "%.1f Mbps", speedTestManager.downloadSpeedMbps) : "--",
                            color: MDMTheme.stateDownloading,
                            systemImage: "arrow.down"
                        )
                        IOSSpeedMetricTile(
                            title: "Upload",
                            value: speedTestManager.uploadSpeedMbps > 0 ? String(format: "%.1f Mbps", speedTestManager.uploadSpeedMbps) : "--",
                            color: MDMTheme.speedCyan,
                            systemImage: "arrow.up"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10))

                // Action Button (Start / Cancel)
                Button(action: {
                    if isTesting {
                        speedTestManager.stopTest()
                    } else {
                        speedTestManager.startTest()
                    }
                }) {
                    HStack(spacing: 8) {
                        Image(systemName: isTesting ? "stop.fill" : "play.fill")
                        Text(actionButtonTitle())
                            .fontWeight(.bold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(isTesting ? MDMTheme.stateFailed : MDMTheme.electricBlue)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
            }
            .padding(16)
        }
        .navigationTitle("Speed Test")
    }

    private func formatReadout() -> String {
        switch speedTestManager.phase {
        case .ping:
            return String(format: "%.0f", speedTestManager.pingMs)
        case .download, .upload:
            return String(format: "%.1f", speedTestManager.currentSpeedMbps)
        case .complete:
            return String(format: "%.1f", speedTestManager.downloadSpeedMbps)
        default:
            return "0.0"
        }
    }

    private func actionButtonTitle() -> String {
        if isTesting {
            return "Cancel Test"
        } else if speedTestManager.phase == .complete {
            return "Test Again"
        } else {
            return "Start Speed Test"
        }
    }
}

struct IOSSpeedMetricTile: View {
    let title: String
    let value: String
    let color: Color
    let systemImage: String

    var body: some View {
        VStack(spacing: 4) {
            HStack(spacing: 4) {
                Image(systemName: systemImage)
                    .font(.caption2)
                    .foregroundColor(color)
                Text(title)
                    .font(.caption2.bold())
                    .foregroundColor(.secondary)
            }
            Text(value)
                .font(.headline.bold())
                .foregroundColor(color)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .background(color.opacity(0.08))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(color.opacity(0.25), lineWidth: 1)
        )
        .cornerRadius(12)
    }
}
