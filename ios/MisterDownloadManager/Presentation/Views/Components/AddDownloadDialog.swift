import SwiftUI
import UIKit

public struct AddDownloadDialog: View {
    @Environment(\.presentationMode) var presentationMode
    @State private var url: String
    @State private var fileName: String = "download.bin"
    @State private var isCustomName: Bool = false
    let onAdd: (String, String) -> Void
    var onAddPlaylist: ((String, String, String?) -> Void)? = nil

    public init(
        initialUrl: String = "",
        onAdd: @escaping (String, String) -> Void,
        onAddPlaylist: ((String, String, String?) -> Void)? = nil
    ) {
        _url = State(initialValue: initialUrl)
        self.onAdd = onAdd
        self.onAddPlaylist = onAddPlaylist
    }

    private var isPlaylist: Bool {
        let u = url.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return u.contains("list=") || u.contains("/playlist") || u.hasSuffix(".m3u8") || u.hasSuffix(".m3u")
    }

    private var isValidScheme: Bool {
        let trimmed = url.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return trimmed.hasPrefix("http://") || trimmed.hasPrefix("https://")
    }

    public var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Download URL (HTTP/HTTPS)")) {
                    HStack {
                        Image(systemName: "link")
                            .foregroundColor(isValidScheme ? MDMTheme.electricBlue : .secondary)

                        TextField("https://example.com/file.mp4", text: $url)
                            .autocapitalization(.none)
                            .disableAutocorrection(true)
                            .onChange(of: url) { newUrl in
                                if !isCustomName {
                                    let detected = sanitizeFileName(extractFileName(from: newUrl))
                                    fileName = detected
                                }
                            }

                        if !url.isEmpty {
                            Button(action: { url = "" }) {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.secondary)
                            }
                            .buttonStyle(BorderlessButtonStyle())
                        }

                        Button(action: {
                            if let pasteString = UIPasteboard.general.string {
                                let candidate = extractFirstUrl(from: pasteString) ?? pasteString.trimmingCharacters(in: .whitespacesAndNewlines)
                                url = candidate
                            }
                        }) {
                            Image(systemName: "doc.on.clipboard")
                                .foregroundColor(MDMTheme.electricBlue)
                        }
                        .buttonStyle(BorderlessButtonStyle())
                    }

                    if !url.isEmpty && !isValidScheme {
                        Text("Please enter a valid HTTP or HTTPS URL")
                            .font(.caption)
                            .foregroundColor(MDMTheme.stateFailed)
                    }

                    if isPlaylist {
                        HStack(spacing: 8) {
                            Image(systemName: "list.bullet.rectangle.fill")
                                .foregroundColor(MDMTheme.electricBlue)
                            Text("Playlist detected! All items will be downloaded into a dedicated folder.")
                                .font(.caption)
                                .foregroundColor(MDMTheme.electricBlue)
                        }
                    }
                }

                Section(header: Text(isPlaylist ? "Playlist Folder / Title" : "File Name")) {
                    TextField(isPlaylist ? "e.g. My Playlist" : "e.g. video.mp4", text: $fileName)
                        .onChange(of: fileName) { _ in
                            isCustomName = true
                        }
                }

                Section(footer: Text("Saving to: Documents/Mister Download Manager/")) {
                    EmptyView()
                }
            }
            .navigationTitle(isPlaylist ? "New Playlist" : "New Download")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Start Download") {
                        let trimmedUrl = url.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard isValidScheme else { return }
                        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
                        let mdmDir = docs.appendingPathComponent("Mister Download Manager", isDirectory: true)
                        try? FileManager.default.createDirectory(at: mdmDir, withIntermediateDirectories: true)

                        if isPlaylist, let addPl = onAddPlaylist {
                            let playlistTitle = fileName.isEmpty ? nil : fileName
                            addPl(trimmedUrl, mdmDir.path, playlistTitle)
                        } else {
                            let cleanName = sanitizeFileName(fileName)
                            let dest = mdmDir.appendingPathComponent(cleanName).path
                            onAdd(trimmedUrl, dest)
                        }
                        presentationMode.wrappedValue.dismiss()
                    }
                    .disabled(!isValidScheme)
                }
            }
        }
    }

    public static func extractFirstUrl(from text: String?) -> String? {
        guard let text = text, !text.isEmpty else { return nil }
        let pattern = "https?://[^\\s\"'<>]+"
        if let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive),
           let match = regex.firstMatch(in: text, options: [], range: NSRange(location: 0, length: text.utf16.count)),
           let range = Range(match.range, in: text) {
            let found = String(text[range])
            return found.trimmingCharacters(in: CharacterSet(charactersIn: ".,);]"))
        }
        return nil
    }

    private func extractFirstUrl(from text: String?) -> String? {
        AddDownloadDialog.extractFirstUrl(from: text)
    }

    private func extractFileName(from urlString: String) -> String {
        guard let components = URLComponents(string: urlString) else {
            return "download.bin"
        }

        // Check query items for name/title/filename
        if let queryItems = components.queryItems {
            for item in queryItems {
                let nameKey = item.name.lowercased()
                if (nameKey == "title" || nameKey == "filename" || nameKey == "file" || nameKey == "name"),
                   let val = item.value, !val.isEmpty {
                    let cleaned = val.removingPercentEncoding ?? val
                    return cleaned.contains(".") ? cleaned : "\(cleaned).mp4"
                }
                if nameKey == "v", let val = item.value, !val.isEmpty {
                    return "YouTube_Video_\(val).mp4"
                }
            }
        }

        if let host = components.host, host.contains("youtu.be") {
            let pathSeg = components.path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
            if !pathSeg.isEmpty {
                return "YouTube_Video_\(pathSeg).mp4"
            }
        }

        let clean = urlString.components(separatedBy: "?")[0].components(separatedBy: "#")[0]
        let name = (clean as NSString).lastPathComponent.removingPercentEncoding ?? (clean as NSString).lastPathComponent
        if name.contains(".") {
            return name
        } else if !name.isEmpty && name != "/" {
            return "\(name).mp4"
        }
        return "download.bin"
    }

    private func sanitizeFileName(_ name: String) -> String {
        let illegal = CharacterSet(charactersIn: "/\\:*?\"<>|")
        var cleaned = name.components(separatedBy: illegal).joined(separator: "_")
        cleaned = cleaned.replacingOccurrences(of: "..", with: "")
        cleaned = cleaned.trimmingCharacters(in: .whitespacesAndNewlines)
        return cleaned.isEmpty ? "download.bin" : cleaned
    }
}
