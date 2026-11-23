package com.misterdownloadmanager.presentation.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// 1. Dark Theme Colors (Deep Midnight Slate & Vibrant Accents)
// ==========================================
val DarkBackground = Color(0xFF0A0F1D)        // Deep Midnight Navy background
val DarkSurface = Color(0xFF131B2E)           // Card / Container surface
val DarkSurfaceElevated = Color(0xFF1E293B)   // Elevated cards, input fields, and bottom bars
val DarkSurfaceBorder = Color(0xFF334155)     // Clear, distinct border contrast

val DarkPrimary = Color(0xFF3B82F6)           // Vibrant Electric Blue for primary buttons/accents
val DarkPrimaryVariant = Color(0xFF2563EB)
val DarkSecondary = Color(0xFF6366F1)         // Royal Indigo for badges & tags
val DarkAccentCyan = Color(0xFF06B6D4)        // Speedometer & speed highlight cyan
val DarkAccentLime = Color(0xFF84CC16)        // Orbit lime highlight

val DarkTextPrimary = Color(0xFFF8FAFC)       // Crisp off-white for high legibility
val DarkTextSecondary = Color(0xFF94A3B8)     // Slate secondary text
val DarkTextMuted = Color(0xFF64748B)         // Subtle muted labels

// ==========================================
// 2. Light Theme Colors (Clean Pure White, Subtle Slate & Deep Ink Contrast)
// ==========================================
val LightBackground = Color(0xFFF1F5F9)       // Soft clean slate background
val LightSurface = Color(0xFFFFFFFF)          // Pure white elevated card surfaces
val LightSurfaceElevated = Color(0xFFE2E8F0)  // Selected chip / hover state
val LightSurfaceBorder = Color(0xFFCBD5E1)    // Sharp, crisp border definition

val LightPrimary = Color(0xFF1D4ED8)          // Deep Royal Electric Blue (WCAG AA compliant contrast on white)
val LightPrimaryVariant = Color(0xFF1E40AF)
val LightSecondary = Color(0xFF4F46E5)        // Vivid Indigo
val LightAccentCyan = Color(0xFF0284C7)       // Deep Cyan for light theme contrast
val LightAccentLime = Color(0xFF65A30D)       // Darker lime for readability on light

val LightTextPrimary = Color(0xFF0F172A)      // Deep Midnight Ink text (maximum readability)
val LightTextSecondary = Color(0xFF334155)    // Dark slate secondary text
val LightTextMuted = Color(0xFF64748B)        // Subtle tertiary text

// ==========================================
// 3. Semantic Download & State Colors
// ==========================================
val StateDownloading = Color(0xFF10B981)      // Crisp Emerald Green (active transfer)
val StateDone = Color(0xFF2563EB)             // Solid Brand Blue (completed)
val StatePaused = Color(0xFFD97706)           // Warm Amber (paused)
val StateFailed = Color(0xFFDC2626)           // Vivid Crimson Red (error/failed)
val StateProbing = Color(0xFF7C3AED)          // Deep Purple (connecting/resolving)
val StateWaiting = Color(0xFF64748B)          // Slate Gray (queued)

// ==========================================
// 4. Official Brand Gradients (Compose Brushes)
// ==========================================
val DownloadAccelerationBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF10B981), Color(0xFF06B6D4))
)

val OrbitStreamBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))
)

val MidnightSurfaceBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF1B253D), Color(0xFF0A0F1D))
)


