package com.example.minimallauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────
// Palette model
// ─────────────────────────────────────────────────────────────────────────

/** The 12 colors the launcher UI needs. */
data class LauncherPalette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val borderMid: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentBg: Color,
    val accentRing: Color,
)

data class ThemeOption(val key: String, val label: String, val palette: LauncherPalette)

const val DEFAULT_THEME_KEY = "warm-dark"

// ── helpers ───────────────────────────────────────────────────────────────

private fun c(rgb: Long): Color = Color(0xFF000000 or rgb)

/** sRGB straight mix: fracA of a, (1-fracA) of b — matches CSS color-mix(in srgb …). */
private fun mix(a: Color, b: Color, fracA: Float): Color {
    val f = fracA
    return Color(
        red = a.red * f + b.red * (1 - f),
        green = a.green * f + b.green * (1 - f),
        blue = a.blue * f + b.blue * (1 - f),
        alpha = a.alpha * f + b.alpha * (1 - f),
    )
}

/** Build a palette from 7 base colors, deriving the rest exactly like the CSS does. */
private fun p(
    bg: Long, surface: Long, border: Long,
    text: Long, text2: Long, accent: Long, accentSoft: Long,
): LauncherPalette {
    val bgC = c(bg); val surfaceC = c(surface); val borderC = c(border)
    val textC = c(text); val text2C = c(text2); val accentC = c(accent)
    return LauncherPalette(
        bg = bgC,
        surface = surfaceC,
        surface2 = mix(surfaceC, bgC, 0.82f),
        border = borderC,
        borderMid = mix(borderC, textC, 0.65f),
        text = textC,
        text2 = text2C,
        text3 = mix(text2C, bgC, 0.62f),
        accent = accentC,
        accentSoft = c(accentSoft),
        accentBg = mix(accentC, surfaceC, 0.14f),
        accentRing = accentC.copy(alpha = 0.35f),
    )
}

// ── hand-tuned defaults (explicit values, as in the source CSS) ─────────────

private val WarmDark = LauncherPalette(
    bg = c(0x1C1A17), surface = c(0x232019), surface2 = c(0x2A2620),
    border = c(0x38332B), borderMid = c(0x4A443A),
    text = c(0xE9E4D8), text2 = c(0xB8B1A3), text3 = c(0x9C9587),
    accent = c(0xD2664A), accentSoft = c(0xE0A45C),
    accentBg = c(0x2E251F), accentRing = Color(0x47D2664A),
)

private val Paper = LauncherPalette(
    bg = c(0xF4F1EA), surface = c(0xFBFAF6), surface2 = c(0xF0ECE2),
    border = c(0xE2DDD1), borderMid = c(0xD2CCBD),
    text = c(0x1C1A17), text2 = c(0x5C574E), text3 = c(0x9C9587),
    accent = c(0xB1442E), accentSoft = c(0xC9633F),
    accentBg = c(0xF3E4DE), accentRing = Color(0x29B1442E),
)

// ─────────────────────────────────────────────────────────────────────────
// All selectable themes
// ─────────────────────────────────────────────────────────────────────────

val AppThemes: List<ThemeOption> = listOf(
    ThemeOption("warm-dark", "Warm Dark", WarmDark),
    ThemeOption("paper", "Paper", Paper),
    //                       bg        surface   border    text      text2     accent    accentSoft
    ThemeOption("tokyo-night", "Tokyo Night", p(0x1A1B26, 0x24283B, 0x2F3549, 0xC0CAF5, 0x565F89, 0x7AA2F7, 0xBB9AF7)),
    ThemeOption("nord", "Nord", p(0x2E3440, 0x3B4252, 0x434C5E, 0xD8DEE9, 0x7B88A1, 0x88C0D0, 0xA3BE8C)),
    ThemeOption("solarized-dark", "Solarized Dark", p(0x002B36, 0x073642, 0x0A4A5A, 0x93A1A1, 0x586E75, 0x268BD2, 0x2AA198)),
    ThemeOption("gruvbox-dark", "Gruvbox Dark", p(0x282828, 0x3C3836, 0x504945, 0xEBDBB2, 0xA89984, 0xFABD2F, 0x8EC07C)),
    ThemeOption("dracula", "Dracula", p(0x282A36, 0x343746, 0x44475A, 0xF8F8F2, 0x7A82A8, 0xBD93F9, 0xFF79C6)),
    ThemeOption("one-dark", "One Dark", p(0x282C34, 0x2F343F, 0x3B4048, 0xABB2BF, 0x727885, 0x61AFEF, 0x98C379)),
    ThemeOption("catppuccin-mocha", "Catppuccin Mocha", p(0x1E1E2E, 0x313244, 0x45475A, 0xCDD6F4, 0xA6ADC8, 0x89B4FA, 0xF5C2E7)),
    ThemeOption("ayu-dark", "Ayu Dark", p(0x0F1419, 0x1C2128, 0x23282E, 0xBFBDB6, 0x6B7178, 0xE6B450, 0x59C2FF)),
    ThemeOption("night-owl", "Night Owl", p(0x011627, 0x0B2942, 0x1D3B53, 0xD6DEEB, 0x7E8FA3, 0x82AAFF, 0xC792EA)),
    ThemeOption("rose-pine", "Rosé Pine", p(0x191724, 0x1F1D2E, 0x26233A, 0xE0DEF4, 0x908CAA, 0xEBBCBA, 0xC4A7E7)),
    ThemeOption("catppuccin-macchiato", "Catppuccin Macchiato", p(0x24273A, 0x363A4F, 0x494D64, 0xCAD3F5, 0xA5ADCB, 0x8AADF4, 0xF5BDE6)),
    ThemeOption("kanagawa-wave", "Kanagawa Wave", p(0x1F1F28, 0x2A2A37, 0x363646, 0xDCD7BA, 0x727169, 0x7E9CD8, 0x98BB6C)),
    ThemeOption("everforest-dark", "Everforest Dark", p(0x2D353B, 0x343F44, 0x3D484D, 0xD3C6AA, 0x859289, 0xA7C080, 0x7FBBB3)),
    ThemeOption("palenight", "Palenight", p(0x292D3E, 0x333851, 0x3A3F58, 0xA6ACCD, 0x676E95, 0x82AAFF, 0xC792EA)),
    ThemeOption("material-ocean", "Material Ocean", p(0x0F111A, 0x1A1C25, 0x1F2233, 0xA6ACCD, 0x4B526D, 0x82AAFF, 0xC3E88D)),
    ThemeOption("monokai-pro", "Monokai Pro", p(0x2D2A2E, 0x353236, 0x403E41, 0xFCFCFA, 0x939293, 0xFFD866, 0xA9DC76)),
    ThemeOption("rose-pine-moon", "Rosé Pine Moon", p(0x232136, 0x2A273F, 0x393552, 0xE0DEF4, 0x908CAA, 0xEA9A97, 0xC4A7E7)),
    ThemeOption("flexoki-dark", "Flexoki Dark", p(0x100F0F, 0x1C1B1A, 0x282726, 0xCECDC3, 0x878580, 0x4385BE, 0x879A39)),
    ThemeOption("solarized-light", "Solarized Light", p(0xFDF6E3, 0xEEE8D5, 0xE3DCC4, 0x586E75, 0x93A1A1, 0x268BD2, 0x2AA198)),
    ThemeOption("gruvbox-light", "Gruvbox Light", p(0xFBF1C7, 0xF2E5BC, 0xE6D8A8, 0x3C3836, 0x7C6F64, 0xB57614, 0x427B58)),
    ThemeOption("atom-one-light", "Atom One Light", p(0xFAFAFA, 0xF0F0F0, 0xE1E1E3, 0x383A42, 0x8B8D94, 0x4078F2, 0x50A14F)),
    ThemeOption("catppuccin-latte", "Catppuccin Latte", p(0xEFF1F5, 0xE6E9EF, 0xCCD0DA, 0x4C4F69, 0x8C8FA1, 0x1E66F5, 0xEA76CB)),
    ThemeOption("sepia-paper", "Sepia Paper", p(0xF4ECD8, 0xECE2C8, 0xDDD0B0, 0x5B4636, 0x8D7D63, 0xA8541F, 0x6B8E23)),
    ThemeOption("github-light", "GitHub Light", p(0xFFFFFF, 0xF6F8FA, 0xD0D7DE, 0x24292F, 0x6E7781, 0x0969DA, 0x1A7F37)),
    ThemeOption("ayu-light", "Ayu Light", p(0xFAFAFA, 0xF3F4F5, 0xE7E8E9, 0x5C6166, 0x8A9199, 0xF2952C, 0x399EE6)),
    ThemeOption("tokyo-day", "Tokyo Day", p(0xE1E2E7, 0xD4D6E4, 0xC4C8DA, 0x343B58, 0x6C739A, 0x2E7DE9, 0x9854F1)),
    ThemeOption("rose-pine-dawn", "Rosé Pine Dawn", p(0xFAF4ED, 0xFFFAF3, 0xDFDAD9, 0x575279, 0x9893A5, 0xD7827E, 0x907AA9)),
    ThemeOption("everforest-light", "Everforest Light", p(0xFFFBEF, 0xF4F0D9, 0xE6E2CC, 0x5C6A72, 0x939F91, 0x8DA101, 0x3A94C5)),
    ThemeOption("quiet-light", "Quiet Light", p(0xF5F5F5, 0xECECEC, 0xDCDCDC, 0x333333, 0x888888, 0x4B83CD, 0x7A3E9D)),
    ThemeOption("kanagawa-lotus", "Kanagawa Lotus", p(0xF2ECBC, 0xE7DBA0, 0xDDD5A8, 0x545464, 0x8A8980, 0x4D699B, 0x6E915F)),
    ThemeOption("flexoki-light", "Flexoki Light", p(0xFFFCF0, 0xF2F0E5, 0xE6E4D9, 0x343331, 0x6F6E69, 0x205EA6, 0x66800B)),
    ThemeOption("nord-light", "Nord Light", p(0xECEFF4, 0xE5E9F0, 0xD8DEE9, 0x2E3440, 0x6C7589, 0x5E81AC, 0x5B8A5A)),
    ThemeOption("material-light", "Material Light", p(0xFAFAFA, 0xEEF0F1, 0xE0E2E4, 0x2F3337, 0x8A9196, 0x4071B8, 0x5A8F2F)),
    ThemeOption("papercolor-light", "PaperColor Light", p(0xEEEEEE, 0xE4E4E4, 0xD0D0D0, 0x444444, 0x878787, 0x0087AF, 0x008700)),
    ThemeOption("tomorrow-light", "Tomorrow Light", p(0xFFFFFF, 0xEFEFEF, 0xD6D6D6, 0x4D4D4C, 0x8E908C, 0x4271AE, 0x4F7A28)),
    ThemeOption("modus-operandi", "Modus Operandi", p(0xFFFFFF, 0xF2F2F2, 0xD7D7D7, 0x000000, 0x595959, 0x0031A9, 0x006800)),
)

private val themesByKey: Map<String, LauncherPalette> = AppThemes.associate { it.key to it.palette }

fun paletteFor(key: String): LauncherPalette = themesByKey[key] ?: WarmDark

// ─────────────────────────────────────────────────────────────────────────
// Active palette + color accessors
// The composable getters keep the old names working, but now resolve to the
// currently selected theme instead of fixed constants.
// ─────────────────────────────────────────────────────────────────────────

val LocalPalette = staticCompositionLocalOf { WarmDark }

val Bg: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.bg
val SurfaceCol: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface
val Surface2: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface2
val BorderCol: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.border
val BorderMid: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.borderMid
val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.text
val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.text2
val TextTertiary: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.text3
val Accent: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accent
val AccentSoft: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accentSoft
val AccentBg: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accentBg
val AccentRing: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accentRing