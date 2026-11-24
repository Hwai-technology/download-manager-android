import SwiftUI

public enum MDMTheme {
    // Official Dark & Light Colors (Brand Blue & True Emerald, zero purple/indigo tint)
    public static let midnightSlate = Color(red: 0x0B / 255.0, green: 0x11 / 255.0, blue: 0x20 / 255.0)
    public static let deepSlate = Color(red: 0x11 / 255.0, green: 0x1C / 255.0, blue: 0x35 / 255.0)
    public static let surfaceElevated = Color(red: 0x19 / 255.0, green: 0x27 / 255.0, blue: 0x47 / 255.0)
    public static let surfaceBorder = Color(red: 0x24 / 255.0, green: 0x35 / 255.0, blue: 0x5A / 255.0)
    
    // Light Mode Design Tokens (Thermal Paper stock, Pure White, Paper Grey, Line, Ink)
    public static let paperBackground = Color(red: 0xFC / 255.0, green: 0xFC / 255.0, blue: 0xFB / 255.0)
    public static let paperSurface = Color.white
    public static let paperElevated = Color(red: 0xF0 / 255.0, green: 0xF3 / 255.0, blue: 0xF7 / 255.0)
    public static let paperLine = Color(red: 0xDD / 255.0, green: 0xE1 / 255.0, blue: 0xE8 / 255.0)
    public static let labelInk = Color(red: 0x11 / 255.0, green: 0x13 / 255.0, blue: 0x18 / 255.0)
    public static let labelInk2 = Color(red: 0x43 / 255.0, green: 0x49 / 255.0, blue: 0x56 / 255.0)
    public static let labelInk3 = Color(red: 0x5F / 255.0, green: 0x66 / 255.0, blue: 0x76 / 255.0)
    
    // Official Brand Blue (#1F63D6) & True Emerald (#22C55E)
    public static let electricBlue = Color(red: 0x1F / 255.0, green: 0x63 / 255.0, blue: 0xD6 / 255.0)
    public static let primaryDark = Color(red: 0x18 / 255.0, green: 0x54 / 255.0, blue: 0xBB / 255.0)
    public static let vividEmerald = Color(red: 0x22 / 255.0, green: 0xC5 / 255.0, blue: 0x5E / 255.0)
    public static let speedCyan = Color(red: 0x06 / 255.0, green: 0xB6 / 255.0, blue: 0xD4 / 255.0)
    public static let oceanCyan = Color(red: 0x02 / 255.0, green: 0x84 / 255.0, blue: 0xC7 / 255.0)
    public static let orbitLime = Color(red: 0x84 / 255.0, green: 0xCC / 255.0, blue: 0x16 / 255.0)
    
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
