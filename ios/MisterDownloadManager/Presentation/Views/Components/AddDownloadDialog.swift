import SwiftUI

public struct AddDownloadDialog: View {
    @Environment(\.presentationMode) var presentationMode
    @State private var url: String = ""
    @State private var fileName: String = "download.bin"
    @State private var isCustomName: Bool = false
    let onAdd: (String, String) -> Void

    private var isYoutubePlaylist: Bool {
        let u = url.lowercased()
        return (u.contains("youtube.com") || u.contains("youtu.be")) && (u.contains("playlist") || u.contains("list="))
    }

    public var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Download URL (HTTP/HTTPS)")) {
                    TextField("https://example.com/file.zip", text: $url)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                        .onChange(of: url) { newUrl in
                            if !isCustomName {
                                let detected = sanitizeFileName(extractFileName(from: newUrl))
                                fileName = detected
                            }
                        }

                    if isYoutubePlaylist {
                        HStack(spacing: 8) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(MDMTheme.stateFailed)
                            Text("YouTube playlists cannot be downloaded as raw files. Direct video links or Desktop extraction are required.")
                                .font(.caption)
                                .foregroundColor(MDMTheme.stateFailed)
                        }
                    }
                }

                Section(header: Text("File Name")) {
                    TextField("e.g. video.mp4", text: $fileName)
                        .onChange(of: fileName) { _ in
                            isCustomName = true
                        }
                }

                Section(footer: Text("Saving to: Documents/Mister Download Manager/")) {
                    EmptyView()
                }
            }
            .navigationTitle("New Download")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Start Download") {
                        guard !url.isEmpty else { return }
                        let cleanName = sanitizeFileName(fileName)
                        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
                        let mdmDir = docs.appendingPathComponent("Mister Download Manager", isDirectory: true)
                        try? FileManager.default.createDirectory(at: mdmDir, withIntermediateDirectories: true)
                        let dest = mdmDir.appendingPathComponent(cleanName).path
                        onAdd(url, dest)
                        presentationMode.wrappedValue.dismiss()
                    }
                    .disabled(url.isEmpty)
                }
            }
        }
    }

    private func extractFileName(from urlString: String) -> String {
        let clean = urlString.components(separatedBy: "?")[0].components(separatedBy: "#")[0]
        let name = (clean as NSString).lastPathComponent
        return name.contains(".") ? name : "download.bin"
    }

    private func sanitizeFileName(_ name: String) -> String {
        let illegal = CharacterSet(charactersIn: "/\\:*?\"<>|")
        var cleaned = name.components(separatedBy: illegal).joined(separator: "_")
        cleaned = cleaned.replacingOccurrences(of: "..", with: "")
        cleaned = cleaned.trimmingCharacters(in: .whitespacesAndNewlines)
        return cleaned.isEmpty ? "download.bin" : cleaned
    }
}
