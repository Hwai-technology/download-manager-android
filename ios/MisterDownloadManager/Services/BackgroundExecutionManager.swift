import Foundation
import BackgroundTasks

public final class BackgroundExecutionManager {
    public static let shared = BackgroundExecutionManager()
    private let taskIdentifier = "com.misterdownloadmanager.refresh"

    private init() {}

    public func registerBackgroundTasks() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: taskIdentifier, using: nil) { task in
            self.handleAppRefresh(task: task as! BGAppRefreshTask)
        }
    }

    public func scheduleAppRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: taskIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            print("Could not schedule app refresh: \(error)")
        }
    }

    private func handleAppRefresh(task: BGAppRefreshTask) {
        scheduleAppRefresh()
        task.expirationHandler = {
            // Cleanup on expiration
        }
        Task {
            _ = MdmMobileFFI.getAllSnapshots()
            task.setTaskCompleted(success: true)
        }
    }
}

