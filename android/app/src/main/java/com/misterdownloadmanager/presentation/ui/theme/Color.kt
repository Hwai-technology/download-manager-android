package com.misterdownloadmanager.presentation.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// 1. Dark Theme Colors (Deep Slate, Clean Royal Blue & Emerald)
// Completely free from purple/indigo tint. Matches official Logo.
// ==========================================
val DarkBackground = Color(0xFF0B1120)        // Deep rich slate (Neutral dark, no purple)
val DarkSurface = Color(0xFF111C35)           // Card container surface
val DarkSurfaceElevated = Color(0xFF192747)   // Elevated cards and inputs
val DarkSurfaceBorder = Color(0xFF24355A)     // Crisp border definition

val DarkPrimary = Color(0xFF1F63D6)           // Official Brand Blue (#1F63D6)
val DarkPrimaryVariant = Color(0xFF1854BB)    // Deep Brand Blue
val DarkSecondary = Color(0xFF0284C7)         // Deep Cyan / Ocean Accent (replacing purple/indigo)
val DarkAccentCyan = Color(0xFF06B6D4)        // Speed highlight Cyan
val DarkAccentLime = Color(0xFF84CC16)        // Orbit Lime

val DarkTextPrimary = Color(0xFFFFFFFF)       // Crisp pure white
val DarkTextSecondary = Color(0xFF94A3B8)     // Neutral slate text
val DarkTextMuted = Color(0xFF64748B)         // Muted gray labels

// ==========================================
// 2. Light Theme Colors (Crisp Thermal Paper, Deep Brand Blue)
// ==========================================
val LightBackground = Color(0xFFF0F3F7)       // Clean soft paper background
val LightSurface = Color(0xFFFFFFFF)          // Pure white card surface
val LightSurfaceElevated = Color(0xFFE2E8F0)  // Selected chip / elevated surface
val LightSurfaceBorder = Color(0xFFCBD5E1)    // Distinct border line

val LightPrimary = Color(0xFF1F63D6)          // Pure Brand Blue (#1F63D6)
val LightPrimaryVariant = Color(0xFF1854BB)
val LightSecondary = Color(0xFF0284C7)        // Ocean Cyan
val LightAccentCyan = Color(0xFF0096C7)       // High contrast Cyan
val LightAccentLime = Color(0xFF65A30D)       // Crisp Lime

val LightTextPrimary = Color(0xFF111318)      // Dark thermal ink text
val LightTextSecondary = Color(0xFF434956)    // Secondary neutral ink
val LightTextMuted = Color(0xFF5F6676)        // Subtle tertiary ink

// ==========================================
// 3. Semantic Download & State Colors
// ==========================================
val StateDownloading = Color(0xFF22C55E)      // True Emerald Green (#22C55E)
val StateDone = Color(0xFF1F63D6)             // Official Brand Blue (#1F63D6)
val StatePaused = Color(0xFFF59E0B)           // Warm Amber
val StateFailed = Color(0xFFEF4444)           // Clean Red
val StateProbing = Color(0xFF0284C7)          // Ocean Blue/Cyan (replaces purple)
val StateWaiting = Color(0xFF64748B)          // Slate Gray

// ==========================================
// 4. Official Brand Gradients (Compose Brushes)
// ==========================================
val DownloadAccelerationBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF22C55E), Color(0xFF06B6D4))
)

val OrbitStreamBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF06B6D4), Color(0xFF1F63D6))
)

val MidnightSurfaceBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF192747), Color(0xFF0B1120))
)



