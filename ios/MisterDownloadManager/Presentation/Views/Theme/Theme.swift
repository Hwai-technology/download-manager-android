import SwiftUI

public enum MDMTheme {
    // Official Dark & Light Colors (Brand Blue & True Emerald, zero purple tint)
    public static let midnightSlate = Color(red: 0x0B / 255.0, green: 0x11 / 255.0, blue: 0x20 / 255.0)
    public static let deepSlate = Color(red: 0x11 / 255.0, green: 0x1C / 255.0, blue: 0x35 / 255.0)
    public static let surfaceElevated = Color(red: 0x19 / 255.0, green: 0x27 / 255.0, blue: 0x47 / 255.0)
    
    // Official Brand Blue (#1F63D6) & True Emerald (#22C55E)
    public static let electricBlue = Color(red: 0x1F / 255.0, green: 0x63 / 255.0, blue: 0xD6 / 255.0)
    public static let primaryDark = Color(red: 0x18 / 255.0, green: 0x54 / 255.0, blue: 0xBB / 255.0)
    public static let vividEmerald = Color(red: 0x22 / 255.0, green: 0xC5 / 255.0, blue: 0x5E / 255.0)
    public static let speedCyan = Color(red: 0x06 / 255.0, green: 0xB6 / 255.0, blue: 0xD4 / 255.0)
    public static let oceanCyan = Color(red: 0x02 / 255.0, green: 0x84 / 255.0, blue: 0xC7 / 255.0)
    
    // Semantic States (No purple/indigo)
    public static let stateDownloading = Color(red: 0x22 / 255.0, green: 0xC5 / 255.0, blue: 0x5E / 255.0)
    public static let stateDone = Color(red: 0x1F / 255.0, green: 0x63 / 255.0, blue: 0xD6 / 255.0)
    public static let statePaused = Color(red: 0xF5 / 255.0, green: 0x9E / 255.0, blue: 0x0B / 255.0)
    public static let stateFailed = Color(red: 0xEF / 255.0, green: 0x44 / 255.0, blue: 0x44 / 255.0)
    public static let stateProbing = Color(red: 0x02 / 255.0, green: 0x84 / 255.0, blue: 0xC7 / 255.0)
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
