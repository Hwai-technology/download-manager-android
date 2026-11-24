package com.misterdownloadmanager.presentation.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// 1. Dark Theme Colors (Deep Slate, Clean Royal Blue & Emerald)
// ==========================================
val DarkBackground = Color(0xFF0B1120)        // Deep rich slate (--mdm-slate-900: #0b1120)
val DarkSurface = Color(0xFF111C35)           // Card container surface (--mdm-slate-800: #111c35)
val DarkSurfaceElevated = Color(0xFF192747)   // Elevated cards and inputs (--mdm-slate-700: #192747)
val DarkSurfaceBorder = Color(0xFF24355A)     // Crisp border definition (--mdm-slate-600: #24355a)

val DarkPrimary = Color(0xFF1F63D6)           // Official Brand Blue (--board: #1f63d6)
val DarkPrimaryVariant = Color(0xFF1854BB)    // Deep Brand Blue (--board-deep: #1854bb)
val DarkSecondary = Color(0xFF0284C7)         // Ocean Cyan (--mdm-cyan-deep: #0284c7)
val DarkAccentCyan = Color(0xFF06B6D4)        // Speed highlight Cyan (--mdm-cyan: #06b6d4)
val DarkAccentLime = Color(0xFF84CC16)        // Orbit Lime (--mdm-lime: #84cc16)

val DarkTextPrimary = Color(0xFFFFFFFF)       // Crisp pure white
val DarkTextSecondary = Color(0xFF94A3B8)     // Neutral slate text
val DarkTextMuted = Color(0xFF64748B)         // Muted gray labels

// ==========================================
// 2. Light Theme Colors (Thermal Paper, Deep Brand Blue, Label Ink)
// Exact match with MDM Design Tokens & Palette
// ==========================================
val LightBackground = Color(0xFFFCFCFB)       // Thermal Paper stock (--paper: #fcfcfb)
val LightSurface = Color(0xFFFFFFFF)          // Pure white sheet surface
val LightSurfaceElevated = Color(0xFFF0F3F7)  // Paper Grey (--paper-2: #f0f3f7)
val LightSurfaceBorder = Color(0xFFDDE1E8)    // Hairline Line (--line: #dde1e8)

val LightPrimary = Color(0xFF1F63D6)          // Pure Brand Blue (--board: #1f63d6)
val LightPrimaryVariant = Color(0xFF1854BB)   // Deep Brand Blue (--board-deep: #1854bb)
val LightSecondary = Color(0xFF0284C7)        // Ocean Cyan (--mdm-cyan-deep: #0284c7)
val LightAccentCyan = Color(0xFF06B6D4)       // Vibrant Cyan (--mdm-cyan: #06b6d4)
val LightAccentLime = Color(0xFF84CC16)       // Orbit Lime (--mdm-lime: #84cc16)

val LightTextPrimary = Color(0xFF111318)      // Dark Label Ink (--ink: #111318)
val LightTextSecondary = Color(0xFF434956)    // Secondary Ink (--ink-2: #434956)
val LightTextMuted = Color(0xFF5F6676)        // Subtle tertiary Ink (--ink-3: #5f6676)

// ==========================================
// 3. Semantic Download & State Colors
// ==========================================
val StateDownloading = Color(0xFF22C55E)      // True Emerald Green (--mdm-emerald-light: #22c55e / #57cc5a)
val StateDone = Color(0xFF1F63D6)             // Official Brand Blue (--board: #1f63d6)
val StatePaused = Color(0xFFF59E0B)           // Warm Amber (--state-paused: #f59e0b)
val StateFailed = Color(0xFFEF4444)           // Clean Red (--state-failed: #ef4444)
val StateProbing = Color(0xFF0284C7)          // Ocean Cyan (--state-probing: #0284c7)
val StateWaiting = Color(0xFF64748B)          // Slate Gray (--state-waiting: #64748b)

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




