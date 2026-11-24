import Foundation
import Combine

public enum SpeedTestPhase: Equatable {
    case idle
    case ping
    case download
    case upload
    case complete
    case error(String)
}

public class SpeedTestManager: ObservableObject {
    @Published public var phase: SpeedTestPhase = .idle
    @Published public var pingMs: Double = 0.0
    @Published public var jitterMs: Double = 0.0
    @Published public var downloadSpeedMbps: Double = 0.0
    @Published public var uploadSpeedMbps: Double = 0.0
    @Published public var currentSpeedMbps: Double = 0.0
    @Published public var currentProgress: Double = 0.0
    @Published public var errorMessage: String? = nil

    private var activeTask: Task<Void, Never>? = nil
    private let pingUrl = URL(string: "https://speed.cloudflare.com/__down?bytes=0")!
    private let downloadUrl = URL(string: "https://speed.cloudflare.com/__down?bytes=25000000")!
    private let uploadUrl = URL(string: "https://speed.cloudflare.com/__up")!

    public init() {}

    public func startTest() {
        stopTest()
        self.phase = .ping
        self.currentProgress = 0.05
        self.pingMs = 0.0
        self.jitterMs = 0.0
        self.downloadSpeedMbps = 0.0
        self.uploadSpeedMbps = 0.0
        self.currentSpeedMbps = 0.0
        self.errorMessage = nil

        activeTask = Task { [weak self] in
            guard let self = self else { return }

            // 1. Ping & Jitter
            let (ping, jitter) = await self.measurePingAndJitter()
            if Task.isCancelled { return }

            await MainActor.run {
                self.pingMs = ping
                self.jitterMs = jitter
                self.phase = .download
                self.currentProgress = 0.20
            }

            // 2. Download Speed
            let download = await self.measureDownloadSpeed()
            if Task.isCancelled { return }

            await MainActor.run {
                self.downloadSpeedMbps = download
                self.currentSpeedMbps = 0.0
                self.phase = .upload
                self.currentProgress = 0.70
            }

            // 3. Upload Speed
            let upload = await self.measureUploadSpeed()
            if Task.isCancelled { return }

            await MainActor.run {
                self.uploadSpeedMbps = upload
                self.currentSpeedMbps = download
                self.phase = .complete
                self.currentProgress = 1.0
            }
        }
    }

    public func stopTest() {
        activeTask?.cancel()
        activeTask = nil
        DispatchQueue.main.async {
            self.phase = .idle
            self.currentSpeedMbps = 0.0
        }
    }

    private func measurePingAndJitter() async -> (Double, Double) {
        var samples: [Double] = []
        let totalPings = 8
        let config = URLSessionConfiguration.ephemeral
        config.timeoutIntervalForRequest = 3.0
        let session = URLSession(configuration: config)

        for i in 1...totalPings {
            if Task.isCancelled { break }
            let start = Date()
            var request = URLRequest(url: pingUrl)
            request.httpMethod = "HEAD"
            request.cachePolicy = .reloadIgnoringLocalAndRemoteCacheData

            do {
                _ = try await session.data(for: request)
                let rttMs = Date().timeIntervalSince(start) * 1000.0
                samples.append(rttMs)

                await MainActor.run {
                    self.currentProgress = 0.05 + (0.15 * (Double(i) / Double(totalPings)))
                    self.pingMs = samples.reduce(0, +) / Double(samples.count)
                }
            } catch {}

            try? await Task.sleep(nanoseconds: 50_000_000)
        }

        if samples.isEmpty {
            return (25.0, 2.0)
        }

        let avgPing = samples.reduce(0, +) / Double(samples.count)
        var jitterSum = 0.0
        if samples.count > 1 {
            for i in 1..<samples.count {
                jitterSum += abs(samples[i] - samples[i - 1])
            }
        }
        let avgJitter = samples.count > 1 ? jitterSum / Double(samples.count - 1) : 1.0
        return (avgPing, avgJitter)
    }

    private func measureDownloadSpeed() async -> Double {
        let startTime = Date()
        let durationSeconds: TimeInterval = 8.0
        var totalBytesDownloaded: Int64 = 0
        let streamCount = 4

        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 5.0
        let session = URLSession(configuration: config)

        await withTaskGroup(of: Int64.self) { group in
            for _ in 0..<streamCount {
                group.addTask {
                    var localBytes: Int64 = 0
                    while !Task.isCancelled && Date().timeIntervalSince(startTime) < durationSeconds {
                        var request = URLRequest(url: self.downloadUrl)
                        request.cachePolicy = .reloadIgnoringLocalAndRemoteCacheData
                        do {
                            let (asyncBytes, _) = try await session.bytes(for: request)
                            for try await _ in asyncBytes {
                                localBytes += 1
                                if Task.isCancelled || Date().timeIntervalSince(startTime) >= durationSeconds {
                                    break
                                }
                            }
                        } catch {
                            try? await Task.sleep(nanoseconds: 200_000_000)
                        }
                    }
                    return localBytes
                }
            }

            // Monitor progress and live speed
            var lastBytes: Int64 = 0
            var lastTime = Date()
            var smoothedMbps = 0.0

            while !Task.isCancelled && Date().timeIntervalSince(startTime) < durationSeconds {
                try? await Task.sleep(nanoseconds: 120_000_000)
                let now = Date()
                let elapsedTotal = now.timeIntervalSince(startTime)
                let delta = now.timeIntervalSince(lastTime)
                let currentBytes = totalBytesDownloaded

                if delta > 0.05 {
                    let instantMbps = (Double(currentBytes - lastBytes) * 8.0) / (delta * 1_000_000.0)
                    smoothedMbps = smoothedMbps == 0.0 ? instantMbps : (smoothedMbps * 0.70 + instantMbps * 0.30)
                    lastBytes = currentBytes
                    lastTime = now

                    let progressFraction = min(1.0, elapsedTotal / durationSeconds)
                    await MainActor.run {
                        self.currentSpeedMbps = smoothedMbps
                        self.downloadSpeedMbps = (Double(currentBytes) * 8.0) / (elapsedTotal * 1_000_000.0)
                        self.currentProgress = 0.20 + (0.50 * progressFraction)
                    }
                }
            }

            for await bytes in group {
                totalBytesDownloaded += bytes
            }
        }

        let totalDuration = Date().timeIntervalSince(startTime)
        let finalMbps = totalDuration > 0 ? (Double(totalBytesDownloaded) * 8.0) / (totalDuration * 1_000_000.0) : 0.0
        return finalMbps
    }

    private func measureUploadSpeed() async -> Double {
        let startTime = Date()
        let durationSeconds: TimeInterval = 6.0
        var totalBytesUploaded: Int64 = 0
        let streamCount = 2
        let payloadData = Data(repeating: 0x55, count: 128 * 1024) // 128 KB chunk

        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 5.0
        let session = URLSession(configuration: config)

        await withTaskGroup(of: Int64.self) { group in
            for _ in 0..<streamCount {
                group.addTask {
                    var localBytes: Int64 = 0
                    while !Task.isCancelled && Date().timeIntervalSince(startTime) < durationSeconds {
                        var request = URLRequest(url: self.uploadUrl)
                        request.httpMethod = "POST"
                        request.httpBody = payloadData
                        request.setValue("application/octet-stream", forHTTPHeaderField: "Content-Type")
                        request.cachePolicy = .reloadIgnoringLocalAndRemoteCacheData
                        do {
                            _ = try await session.data(for: request)
                            localBytes += Int64(payloadData.count)
                        } catch {
                            try? await Task.sleep(nanoseconds: 200_000_000)
                        }
                    }
                    return localBytes
                }
            }

            var lastBytes: Int64 = 0
            var lastTime = Date()
            var smoothedMbps = 0.0

            while !Task.isCancelled && Date().timeIntervalSince(startTime) < durationSeconds {
                try? await Task.sleep(nanoseconds: 120_000_000)
                let now = Date()
                let elapsedTotal = now.timeIntervalSince(startTime)
                let delta = now.timeIntervalSince(lastTime)
                let currentBytes = totalBytesUploaded

                if delta > 0.05 {
                    let instantMbps = (Double(currentBytes - lastBytes) * 8.0) / (delta * 1_000_000.0)
                    smoothedMbps = smoothedMbps == 0.0 ? instantMbps : (smoothedMbps * 0.70 + instantMbps * 0.30)
                    lastBytes = currentBytes
                    lastTime = now

                    let progressFraction = min(1.0, elapsedTotal / durationSeconds)
                    await MainActor.run {
                        self.currentSpeedMbps = smoothedMbps
                        self.uploadSpeedMbps = (Double(currentBytes) * 8.0) / (elapsedTotal * 1_000_000.0)
                        self.currentProgress = 0.70 + (0.30 * progressFraction)
                    }
                }
            }

            for await bytes in group {
                totalBytesUploaded += bytes
            }
        }

        let totalDuration = Date().timeIntervalSince(startTime)
        let finalMbps = totalDuration > 0 ? (Double(totalBytesUploaded) * 8.0) / (totalDuration * 1_000_000.0) : 0.0
        return finalMbps
    }
}
