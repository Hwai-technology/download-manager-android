# Design Tokens, Palette & Visual System (قواعد وتدرجات الألوان والهوية)

This specification defines the authoritative color palette, dynamic gradients, semantic states, and theming standards for **Mister Download Manager (MDM)** across native mobile frontends (Android Jetpack Compose, iOS SwiftUI) and Desktop / Web.

---

## 1. Brand Identity & Logo Analysis

The visual identity is anchored by the **Globe Gentleman** mascot and orbit stream:
- **The Globe:** Deep Ocean Blue (`#0B63C5` / `#0072CE`) paired with energetic lime landmasses (`#8EE041`).
- **The Sweeping Orbit Arrow & Digital Packets:** High-speed download acceleration transitioning from crisp emerald green (`#22C55E` / `#10B981`) to bright cyan/teal (`#06B6D4` / `#00B4D8`).
- **The Top Hat & Moustache:** Midnight Obsidian (`#0F172A` / `#1A1D24`) accented with a vivid emerald hat-band (`#3CD070`).

--- 

## 2. Core Color Palette

### Primary Colors (الألوان الأساسية المعتمدة)
| Name | Hex Code | Compose / Token | Purpose |
| :--- | :--- | :--- | :--- |
| **MDM Brand Blue** | `#1F63D6` / `#1854BB` | `DarkPrimary` / `LightPrimary` | Primary brand actions, active buttons, title bar accents |
| **MDM True Emerald** | `#22C55E` / `#10B981` | `StateDownloading` | Active download state, download progress indicators, success markers |

### Secondary & Accent Colors (الألوان الثانوية والمتناسقة)
| Name | Hex Code | Compose / Token | Purpose |
| :--- | :--- | :--- | :--- |
| **MDM Speed Cyan** | `#06B6D4` / `#0096C7` | `DarkAccentCyan` / `LightAccentCyan` | Speedometer stats, network throughput charts, segment active blocks |
| **MDM Ocean Blue** | `#0284C7` / `#0369A1` | `DarkSecondary` / `LightSecondary` | Secondary chips, selection tags, subtle interactive highlights |
| **MDM Orbit Lime** | `#84CC16` / `#65A30D` | `AccentLime` | Speed burst indicators, high-efficiency connection nodes |
| **MDM Neutral Slate** | `#0B1120` / `#111C35` | `DarkBackground` / `DarkSurface` | Pure dark slate background, cards, and elevated surfaces (Zero purple tint) |

---

## 3. Dynamic Brand Gradients (تدرجات الألوان الرسمية)

Use these calibrated gradients for progress bars, hero headers, floating action buttons, and animated download sweeps:

### A. Download Acceleration Gradient (الأخضر الزمردي إلى السماوي)
*Represents active data streaming, high speed, and packet integrity.*
```css
/* CSS / Web */
background: linear-gradient(135deg, #10B981 0%, #06B6D4 100%);
```
```kotlin
// Android Jetpack Compose Brush
val DownloadProgressBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF10B981), Color(0xFF06B6D4))
)
```

### B. High-Speed Orbit Stream (السماوي إلى الأزرق الكهربائي)
*Represents multi-connection pooling and cloud transfer.*
```css
background: linear-gradient(135deg, #06B6D4 0%, #3B82F6 100%);
```
```kotlin
val OrbitStreamBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))
)
```

### C. Gentleman's Midnight Surface (تدرج الخلفية الداكنة الأنيقة)
*Represents deep background surfaces, modals, and navigation drawers.*
```css
background: linear-gradient(180deg, #1B253D 0%, #0A0F1D 100%);
```
```kotlin
val MidnightSurfaceBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF1B253D), Color(0xFF0A0F1D))
)
```

---

## 4. Semantic State Colors (ألوان الحالات الوظيفية)

Every download state has a strictly designated semantic color:

| State | Color Name | Hex Code | Android Theme Token | Meaning |
| :--- | :--- | :--- | :--- | :--- |
| **Downloading** | Emerald Green | `#10B981` | `StateDownloading` | Active TCP segments receiving bytes |
| **Done** | Solid Blue | `#3B82F6` | `StateDone` | Checksum verified, file finalized |
| **Paused** | Amber Warning | `#F59E0B` | `StatePaused` | User or queue paused, state saved in `.mdm.json` |
| **Failed** | Clean Coral Red | `#EF4444` | `StateFailed` | HTTP error, network drop, disk full |
| **Probing** | Deep Purple | `#8B5CF6` | `StateProbing` | Resolving DNS, TLS handshake, inspecting headers |
| **Waiting** | Slate Gray | `#64748B` | `StateWaiting` | Queued, waiting for concurrency slot |

---

## 5. UI Contrast & Accessibility Guidelines

1. **Text Contrast Ratio:** Ensure a minimum 4.5:1 contrast ratio against background surfaces for body text, and 3:1 for large headers (WCAG AA compliant).
2. **Never Hardcode Hex in Composable Views:** Always consume colors from `MaterialTheme.colorScheme` or the defined semantic state tokens in `Color.kt`.
3. **Progress Bar Animation:** Active transfers should pair the **Download Acceleration Gradient** (`#10B981` -> `#06B6D4`) with a subtle shimmer effect on the leading edge.
