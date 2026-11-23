import SwiftUI

public struct SettingsView: View {
    @AppStorage("pref_concurrent_downloads") private var maxConcurrent: Int = 3
    @AppStorage("pref_notifications_enabled") private var notificationsEnabled: Bool = true
    @State private var showingLicenseSheet = false

    private var storageLocation: String {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        return docs.path
    }

    public var body: some View {
        NavigationView {
            Form {
                // Downloads Section
                Section(header: Text("Downloads & Storage")) {
                    Stepper(value: $maxConcurrent, in: 1...8) {
                        HStack {
                            Text("Concurrent Downloads")
                            Spacer()
                            Text("\(maxConcurrent)")
                                .foregroundColor(.secondary)
                        }
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        Text("Default Storage Location")
                            .font(.body)
                        Text(storageLocation)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .lineLimit(2)
                    }
                }

                // Notifications Section
                Section(header: Text("Notifications")) {
                    Toggle("Show Transfer Alerts", isOn: $notificationsEnabled)
                }

                // About & Legal Section
                Section(header: Text("About & Legal")) {
                    HStack {
                        Image(systemName: "info.circle")
                            .foregroundColor(MDMTheme.electricBlue)
                        Text("Mister Download Manager")
                        Spacer()
                        Text("v0.5.2")
                            .foregroundColor(.secondary)
                    }

                    Link(destination: URL(string: "https://github.com/Hwai-technology/download-manager-android")!) {
                        HStack {
                            Image(systemName: "link")
                                .foregroundColor(MDMTheme.electricBlue)
                            Text("GitHub Repository")
                            Spacer()
                            Image(systemName: "arrow.up.right.square")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }

                    Button(action: { showingLicenseSheet = true }) {
                        HStack {
                            Image(systemName: "doc.text")
                                .foregroundColor(MDMTheme.electricBlue)
                            Text("Open-source License (GPLv3)")
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("Settings")
            .sheet(isPresented: $showingLicenseSheet) {
                LicenseDetailSheet()
            }
        }
    }
}

struct LicenseDetailSheet: View {
    @Environment(\.presentationMode) var presentationMode

    private let licenseSummary = """
    GNU GENERAL PUBLIC LICENSE
    Version 3, 29 June 2007

    Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
    Everyone is permitted to copy and distribute verbatim copies
    of this license document, but changing it is not allowed.

    Preamble
    The GNU General Public License is a free, copyleft license for software and other kinds of works.
    The licenses for most software and other practical works are designed to take away your freedom to share and change the works. By contrast, the GNU General Public License is intended to guarantee your freedom to share and change all versions of a program--to make sure it remains free software for all its users.

    Full license text available at:
    https://www.gnu.org/licenses/gpl-3.0.en.html
    """

    var body: some View {
        NavigationView {
            ScrollView {
                Text(licenseSummary)
                    .font(.system(.footnote, design: .monospaced))
                    .padding(16)
            }
            .navigationTitle("License (GPLv3)")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Close") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
            }
        }
    }
}
