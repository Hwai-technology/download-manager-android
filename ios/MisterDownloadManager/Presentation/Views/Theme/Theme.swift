import SwiftUI

public enum MDMTheme {
    // Official Dark & Light Colors
    public static let midnightSlate = Color(red: 0x0A / 255.0, green: 0x0F / 255.0, blue: 0x1D / 255.0)
    public static let deepNavy = Color(red: 0x13 / 255.0, green: 0x1B / 255.0, blue: 0x2E / 255.0)
    public static let surfaceElevated = Color(red: 0x1E / 255.0, green: 0x29 / 255.0, blue: 0x3B / 255.0)
    
    public static let electricBlue = Color(red: 0x3B / 255.0, green: 0x82 / 255.0, blue: 0xF6 / 255.0)
    public static let primaryDark = Color(red: 0x25 / 255.0, green: 0x63 / 255.0, blue: 0xEB / 255.0)
    public static let vividEmerald = Color(red: 0x10 / 255.0, green: 0xB9 / 255.0, blue: 0x81 / 255.0)
    public static let speedCyan = Color(red: 0x06 / 255.0, green: 0xB6 / 255.0, blue: 0xD4 / 255.0)
    public static let royalIndigo = Color(red: 0x63 / 255.0, green: 0x66 / 255.0, blue: 0xF1 / 255.0)
    
    // Semantic States
    public static let stateDownloading = Color(red: 0x10 / 255.0, green: 0xB9 / 255.0, blue: 0x81 / 255.0)
    public static let stateDone = Color(red: 0x25 / 255.0, green: 0x63 / 255.0, blue: 0xEB / 255.0)
    public static let statePaused = Color(red: 0xD9 / 255.0, green: 0x77 / 255.0, blue: 0x06 / 255.0)
    public static let stateFailed = Color(red: 0xDC / 255.0, green: 0x26 / 255.0, blue: 0x26 / 255.0)
    public static let stateProbing = Color(red: 0x7C / 255.0, green: 0x3A / 255.0, blue: 0xED / 255.0)
    public static let stateWaiting = Color(red: 0x64 / 255.0, green: 0x74 / 255.0, blue: 0x8B / 255.0)

    // Gradients
    public static let downloadAccelerationGradient = LinearGradient(
        gradient: Gradient(colors: [vividEmerald, speedCyan]),
        startPoint: .leading,
        endPoint: .trailing
    )

    public static let orbitStreamGradient = LinearGradient(
        gradient: Gradient(colors: [speedCyan, electricBlue]),
        startPoint: .leading,
        endPoint: .trailing
    )
}
