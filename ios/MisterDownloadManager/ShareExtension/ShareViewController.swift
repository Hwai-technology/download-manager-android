import UIKit
import Social
import UniformTypeIdentifiers

/**
 * ShareViewController for Mister Download Manager iOS Share Extension.
 *
 * Allows users to share URLs directly from Safari, Chrome, and other apps
 * into Mister Download Manager.
 *
 * It extracts the URL, saves it to an App Group UserDefaults container,
 * and launches the main app via custom URL scheme (mdm://import?url=...).
 */
class ShareViewController: UIViewController {

    private let appGroupIdentifier = "group.com.misterdownloadmanager.shared"
    private let urlScheme = "mdm"

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.black.withAlphaComponent(0.6)
        processSharedItems()
    }

    private func processSharedItems() {
        guard let extensionItem = extensionContext?.inputItems.first as? NSExtensionItem,
              let attachments = extensionItem.attachments else {
            completeRequest()
            return
        }

        let contentTypeURL = UTType.url.identifier
        let contentTypePlainText = UTType.plainText.identifier

        for provider in attachments {
            if provider.hasItemConformingToTypeIdentifier(contentTypeURL) {
                provider.loadItem(forTypeIdentifier: contentTypeURL, options: nil) { [weak self] (item, error) in
                    if let url = item as? URL {
                        self?.handleExtractedUrl(url.absoluteString)
                    } else if let urlString = item as? String {
                        self?.handleExtractedUrl(urlString)
                    } else {
                        self?.completeRequest()
                    }
                }
                return
            } else if provider.hasItemConformingToTypeIdentifier(contentTypePlainText) {
                provider.loadItem(forTypeIdentifier: contentTypePlainText, options: nil) { [weak self] (item, error) in
                    if let text = item as? String,
                       let url = self?.extractFirstUrl(from: text) {
                        self?.handleExtractedUrl(url)
                    } else {
                        self?.completeRequest()
                    }
                }
                return
            }
        }

        completeRequest()
    }

    private func extractFirstUrl(from text: String) -> String? {
        let pattern = "https?://[^\\s\"'<>]+"
        if let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive),
           let match = regex.firstMatch(in: text, options: [], range: NSRange(location: 0, length: text.utf16.count)),
           let range = Range(match.range, in: text) {
            let found = String(text[range])
            return found.trimmingCharacters(in: CharacterSet(charactersIn: ".,);]"))
        }
        return nil
    }

    private func handleExtractedUrl(_ urlString: String) {
        guard !urlString.isEmpty else {
            completeRequest()
            return
        }

        // Store into App Group shared defaults for cold starts or background synchronization
        if let sharedDefaults = UserDefaults(suiteName: appGroupIdentifier) {
            sharedDefaults.set(urlString, forKey: "pending_shared_url")
            sharedDefaults.synchronize()
        }

        // Open main app via deep link
        if let encoded = urlString.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
           let deepLink = URL(string: "\(urlScheme)://import?url=\(encoded)") {
            var responder: UIResponder? = self
            while responder != nil {
                if let application = responder as? UIApplication {
                    application.open(deepLink, options: [:], completionHandler: { [weak self] _ in
                        self?.completeRequest()
                    })
                    return
                }
                responder = responder?.next
            }
        }

        completeRequest()
    }

    private func completeRequest() {
        extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
    }
}

