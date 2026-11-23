# Website Architecture & Presentation Design Standards (قواعد وهندسة موقع العرض والتسويق)

This specification defines the clean architecture, modular file organization, Tailwind CSS-compatible design tokens, brand identity adherence, and responsive showcase standards for the **Mister Download Manager (MDM)** public website (`website/`).

---

## 1. Modular Directory & File Architecture (الهيكلة النظيفة للملفات)

The website is engineered as a high-performance, zero-build-step static web application designed to load instantly on any device and be easily deployed via GitHub Pages or static web servers:

```
website/
├── src/                          # Source styles, scripts, and structured data
│   ├── css/
│   │   └── styles.css            # Unified design system, tokens, Tailwind utility variables
│   ├── js/
│   │   ├── main.js               # Reactive client-side runtime (OS detect, i18n, theme switch)
│   │   └── bench.js              # Interactive SVG benchmark graphing engine
│   └── data/
│       └── benchmarks.json       # Structured benchmark datasets
├── pages/                        # HTML Page templates & views
│   ├── index.html                # Main landing page & application showcase
│   ├── download.html             # Multi-platform download portal (Desktop, Mobile, Extensions)
│   ├── benchmark.html            # Live benchmark comparison suite (MDM vs IDM vs aria2)
│   ├── idm-alternative.html      # Feature-by-feature comparative breakdown
│   └── 404.html                  # Custom styled error page matching courier theme
├── assets/                       # Vector graphics, logos, platform badges, barcodes
│   ├── brand/                    # Official logo (Mister Download Manager.png)
│   ├── platforms/                # Windows, Android, Apple, Linux SVGs
│   └── icons/                    # UI symbols, copy icons, status indicators
├── content/                      # Static markdown/HTML articles (Security, FAQs, Guides)
├── vendor/                       # Self-hosted typography (Archivo variable font) & libs
└── bench/                        # Backward-compatibility benchmark data
```

---

## 2. Presentation & Showcase Layout (هندسة تقسيم واجهة العرض والمحاكاة)

Every landing and showcase page must follow a clear vertical hierarchy designed for high conversion, readability, and clean visual flow:

### 1. Waybill Header & Global Navigation (`<header class="mast">`)
- Mascot logo with subtle hover acceleration.
- Platform navigation pills (Android, iOS, Desktop, Extensions).
- Language selector dropdown supporting 21+ languages.
- Light / Dark theme toggle button (`data-theme` switch).

### 2. Hero Section & Instant Launcher (`<section class="hero">`)
- **Impact Statement:** Ultra-condensed bold typography announcing speed, native engines, and zero ads.
- **Smart Primary CTA:** Auto-detects visitor's client OS (`Android`, `Windows`, `macOS`, `Linux`, `iOS`) and presents the matching download binary with SHA-256 checksum tags.
- **Secondary Action:** "View all platforms" direct anchor.

### 3. Live Presentation & Screenshot Carousel (`<section class="showcase">`)
- **Dual-Theme Adaptive Media:** Every app preview must provide both light (`.for-light`) and dark (`.for-dark`) high-DPI screenshots that automatically synchronize with the active website theme.
- **Interactive State Cards:** Highlight the 6 core download states (Downloading, Done, Paused, Probing, Failed, Waiting) with their respective semantic tokens.

### 4. Interactive Benchmark Suite (`<section class="benchmarks">`)
- Direct visual demonstration of multi-connection throughput vs traditional single-threaded downloads.
- Clean vector bar charts styled using brand tokens (`--mdm-emerald` vs fallback grey).

### 5. Multi-Platform Distribution Grid (`<section class="platforms">`)
- Structured cards for each platform variant:
  - **Android:** Direct APK download + Google Play link + F-Droid notice.
  - **iOS:** TestFlight / App Store link + C-ABI core overview.
  - **Desktop:** Windows Inno Setup / Portable, Linux AppImage/deb/rpm, macOS DMG.
  - **Browsers:** Chrome Web Store & Firefox Add-ons one-click installation buttons.

---

## 3. Design Tokens & Tailwind CSS Compatibility

The styling architecture in `website/styles.css` incorporates the complete brand identity from [`.agents/rules/DESIGN_TOKENS_AND_PALETTE.md`](DESIGN_TOKENS_AND_PALETTE.md), organized into utility variables compatible with Tailwind CSS naming conventions:

### Core Palette Tokens
```css
:root {
  /* MDM Brand Primary & Secondary Colors */
  --mdm-blue: #2563eb;          /* bg-blue-600 / primary action */
  --mdm-blue-light: #3b82f6;    /* bg-blue-500 / electric highlight */
  --mdm-blue-deep: #1d4ed8;     /* bg-blue-700 / active hover */
  
  --mdm-emerald: #10b981;       /* bg-emerald-500 / download active */
  --mdm-emerald-light: #22c55e; /* bg-green-500 / speed pulse */
  --mdm-emerald-deep: #059669;  /* bg-emerald-600 / button hover */

  --mdm-cyan: #06b6d4;          /* bg-cyan-500 / speedometer accent */
  --mdm-cyan-deep: #0284c7;     /* bg-sky-600 / stream telemetry */

  --mdm-indigo: #6366f1;        /* bg-indigo-500 / chip secondary */
  --mdm-lime: #84cc16;          /* bg-lime-500 / node active */

  --mdm-slate-900: #0a0f1d;     /* bg-slate-950 / dark background */
  --mdm-slate-800: #131b2e;     /* bg-slate-900 / dark surface card */
  --mdm-slate-700: #1b253d;     /* bg-slate-800 / elevated surface */
  --mdm-slate-600: #223050;     /* border-slate-700 / surface border */

  /* Dynamic Gradients */
  --gradient-speed: linear-gradient(135deg, var(--mdm-emerald) 0%, var(--mdm-cyan) 100%);
  --gradient-orbit: linear-gradient(135deg, var(--mdm-cyan) 0%, var(--mdm-blue-light) 100%);
  --gradient-surface: linear-gradient(180deg, var(--mdm-slate-700) 0%, var(--mdm-slate-900) 100%);

  /* Semantic State Tokens */
  --state-downloading: var(--mdm-emerald);
  --state-done: var(--mdm-blue-light);
  --state-paused: #f59e0b;
  --state-failed: #ef4444;
  --state-probing: #8b5cf6;
  --state-waiting: #64748b;
}
```

---

## 4. Dark & Light Theme System Standards

1. **Automatic Detection:** Respect system preference via `@media (prefers-color-scheme: dark)` as the primary default.
2. **Explicit Override:** Users can toggle themes via the header button, stored atomically in `localStorage.setItem("mdm-theme", "light" | "dark")`.
3. **Zero Flash of Incorrect Theme (FOIT):** A small inline bootstrap script in `<head>` applies `data-theme` before the DOM renders.
4. **Contrast Compliance:** All text must achieve WCAG AA contrast (minimum 4.5:1 ratio against surface background).

---

## 5. Clean Code Rules for Website Development

- **No Framework Overhead:** Do not introduce heavy frontend frameworks (React, Vue, Angular) into `website/`. Keep the site fast, responsive, and deployable as static assets.
- **Zero Inline Styles:** UI components must use CSS utility classes or semantic component selectors defined in `styles.css`.
- **Self-Contained Assets:** External CDNs must not be required for core functionality. Fonts, icons, and scripts must load locally or gracefully degrade.
- **Responsive Fluid Layouts:** Use `clamp()`, CSS Grid (`grid-template-columns: repeat(auto-fit, minmax(...))`), and Flexbox for seamless scaling from 320px mobile viewports up to 4K ultra-wide monitors.
