import SwiftUI
import UIKit

public struct AddDownloadDialog: View {
    @Environment(\.presentationMode) var presentationMode
    @State private var url: String
    @State private var fileName: String = "download.bin"
    @State private var isCustomName: Bool = false

    // Video metadata & quality selection state
    @State private var isExtracting: Bool = false
    @State private var extractedMetadata: VideoMetadata? = nil
    @State private var selectedDownloadType: String = "VIDEO" // "VIDEO" or "AUDIO"
    @State private var selectedFormat: FormatChoice? = nil

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
                // Section A: URL Input
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
                                triggerExtraction(newUrl)
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
                                triggerExtraction(candidate)
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

                    if isExtracting {
                        HStack(spacing: 8) {
                            ProgressView()
                                .scaleEffect(0.8)
                            Text("Inspecting video metadata and formats...")
                                .font(.caption)
                                .foregroundColor(MDMTheme.electricBlue)
                        }
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

                // Section B: Live Metadata Preview
                if let meta = extractedMetadata {
                    Section(header: Text("Video Metadata Preview")) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(meta.title)
                                .font(.system(size: 15, weight: .bold))
                                .foregroundColor(.primary)

                            if let uploader = meta.uploader {
                                Text("Channel: \(uploader)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }

                        // Section C: Download Type Selector (Video vs Audio Only)
                        Picker("Download Type", selection: $selectedDownloadType) {
                            Text("Video (MP4)").tag("VIDEO")
                            Text("Audio Only").tag("AUDIO")
                        }
                        .pickerStyle(SegmentedPickerStyle())
                        .onChange(of: selectedDownloadType) { newType in
                            let ext = newType == "AUDIO" ? "mp3" : "mp4"
                            fileName = "\(meta.title).\(ext)"
                            if newType == "AUDIO" {
                                selectedFormat = meta.formats.first(where: { $0.isAudio && !$0.isVideo }) ?? meta.formats.first
                            } else {
                                selectedFormat = meta.formats.first(where: { $0.isVideo }) ?? meta.formats.first
                            }
                        }

                        // Section D: Available Quality Selector
                        let choices = meta.formats.filter { selectedDownloadType == "AUDIO" ? $0.isAudio : $0.isVideo }
                        if !choices.isEmpty {
                            Picker("Quality", selection: Binding(
                                get: { selectedFormat?.id ?? choices.first?.id ?? "" },
                                set: { newId in
                                    if let fmt = choices.first(where: { $0.id == newId }) {
                                        selectedFormat = fmt
                                        fileName = "\(meta.title).\(fmt.ext)"
                                    }
                                }
                            )) {
                                ForEach(choices) { fmt in
                                    Text(fmt.label).tag(fmt.id)
                                }
                            }
                        }
                    }
                }

                // Section E: Output File / Folder Name
                Section(header: Text(isPlaylist ? "Playlist Folder / Title" : "File Name")) {
                    TextField(isPlaylist ? "e.g. My Playlist" : "e.g. video.mp4", text: $fileName)
                        .onChange(of: fileName) { _ in
                            isCustomName = true
                        }
                }

                // Section F: Download Location
                Section(header: Text("Download Location")) {
                    HStack {
                        Image(systemName: "folder.fill")
                            .foregroundColor(MDMTheme.electricBlue)
                        Text("Documents/Mister Download Manager")
                            .font(.system(size: 14))
                            .lineLimit(1)
                        Spacer()
                        Button("Change") {
                            // System document folder selection
                        }
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(MDMTheme.electricBlue)
                    }
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
                        let finalUrl = selectedFormat?.downloadUrl ?? url.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard isValidScheme else { return }
                        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
                        let mdmDir = docs.appendingPathComponent("Mister Download Manager", isDirectory: true)
                        try? FileManager.default.createDirectory(at: mdmDir, withIntermediateDirectories: true)

                        if isPlaylist, let addPl = onAddPlaylist {
                            let playlistTitle = fileName.isEmpty ? nil : fileName
                            addPl(finalUrl, mdmDir.path, playlistTitle)
                        } else {
                            let cleanName = sanitizeFileName(fileName)
                            let dest = mdmDir.appendingPathComponent(cleanName).path
                            onAdd(finalUrl, dest)
                        }
                        presentationMode.wrappedValue.dismiss()
                    }
                    .disabled(!isValidScheme)
                }
            }
            .onAppear {
                if !url.isEmpty {
                    triggerExtraction(url)
                }
            }
        }
    }

    private func triggerExtraction(_ inputUrl: String) {
        let trimmed = inputUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.hasPrefix("http://") || trimmed.hasPrefix("https://") else { return }
        guard !isPlaylist else { return }

        isExtracting = true
        DispatchQueue.global(qos: .userInitiated).async {
            let meta = MdmMobileFFI.extractVideoMetadata(url: trimmed)
            DispatchQueue.main.async {
                self.isExtracting = false
                if let m = meta {
                    self.extractedMetadata = m
                    if !self.isCustomName && !m.title.isEmpty {
                        let ext = self.selectedDownloadType == "AUDIO" ? "mp3" : "mp4"
                        self.fileName = "\(m.title).\(ext)"
                    }
                    self.selectedFormat = m.formats.first(where: { self.selectedDownloadType == "AUDIO" ? $0.isAudio && !$0.isVideo : $0.isVideo })
                        ?? m.formats.first
                }
            }
        }
    }

    private func sanitizeFileName(_ name: String) -> String {
        let invalidCharacters = CharacterSet(charactersIn: "\\/:*?\"<>|")
        var clean = name.components(separatedBy: invalidCharacters).joined()
        clean = clean.replacingOccurrences(of: "..", with: "")
        clean = clean.trimmingCharacters(in: .whitespacesAndNewlines)
        return clean.isEmpty ? "download.bin" : clean
    }

    private func extractFileName(from urlString: String) -> String {
        guard let url = URL(string: urlString) else { return "download.bin" }
        let last = url.lastPathComponent
        return last.isEmpty || last == "/" ? "download.bin" : last
    }

    private func extractFirstUrl(from text: String) -> String? {
        let types: NSTextCheckingResult.CheckingType = .link
        guard let detector = try? NSDataDetector(types: types.rawValue) else { return nil }
        let matches = detector.matches(in: text, options: [], range: NSRange(location: 0, length: text.utf16.count))
        for match in matches {
            if let matchRange = Range(match.range, in: text) {
                let candidate = String(text[matchRange])
                if candidate.lowercased().hasPrefix("http://") || candidate.lowercased().hasPrefix("https://") {
                    return candidate
                }
            }
        }
        return nil
    }
}
