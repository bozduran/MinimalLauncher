package com.example.minimallauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────
// Palette model
// ─────────────────────────────────────────────────────────────────────────

/** The 12 colors the launcher UI needs. */
@Immutable
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
    ThemeOption("tokyo-night", "Tokyo Night", p(
            bg = 0x1A1B26, surface = 0x24283B, border = 0x2F3549,
            text = 0xC0CAF5, text2 = 0x565F89, accent = 0x7AA2F7, accentSoft = 0xBB9AF7,
        )),
    ThemeOption("nord", "Nord", p(
            bg = 0x2E3440, surface = 0x3B4252, border = 0x434C5E,
            text = 0xD8DEE9, text2 = 0x7B88A1, accent = 0x88C0D0, accentSoft = 0xA3BE8C,
        )),
    ThemeOption("solarized-dark", "Solarized Dark", p(
            bg = 0x002B36, surface = 0x073642, border = 0x0A4A5A,
            text = 0x93A1A1, text2 = 0x586E75, accent = 0x268BD2, accentSoft = 0x2AA198,
        )),
    ThemeOption("gruvbox-dark", "Gruvbox Dark", p(
            bg = 0x282828, surface = 0x3C3836, border = 0x504945,
            text = 0xEBDBB2, text2 = 0xA89984, accent = 0xFABD2F, accentSoft = 0x8EC07C,
        )),
    ThemeOption("dracula", "Dracula", p(
            bg = 0x282A36, surface = 0x343746, border = 0x44475A,
            text = 0xF8F8F2, text2 = 0x7A82A8, accent = 0xBD93F9, accentSoft = 0xFF79C6,
        )),
    ThemeOption("one-dark", "One Dark", p(
            bg = 0x282C34, surface = 0x2F343F, border = 0x3B4048,
            text = 0xABB2BF, text2 = 0x727885, accent = 0x61AFEF, accentSoft = 0x98C379,
        )),
    ThemeOption("catppuccin-mocha", "Catppuccin Mocha", p(
            bg = 0x1E1E2E, surface = 0x313244, border = 0x45475A,
            text = 0xCDD6F4, text2 = 0xA6ADC8, accent = 0x89B4FA, accentSoft = 0xF5C2E7,
        )),
    ThemeOption("ayu-dark", "Ayu Dark", p(
            bg = 0x0F1419, surface = 0x1C2128, border = 0x23282E,
            text = 0xBFBDB6, text2 = 0x6B7178, accent = 0xE6B450, accentSoft = 0x59C2FF,
        )),
    ThemeOption("night-owl", "Night Owl", p(
            bg = 0x011627, surface = 0x0B2942, border = 0x1D3B53,
            text = 0xD6DEEB, text2 = 0x7E8FA3, accent = 0x82AAFF, accentSoft = 0xC792EA,
        )),
    ThemeOption("rose-pine", "Rosé Pine", p(
            bg = 0x191724, surface = 0x1F1D2E, border = 0x26233A,
            text = 0xE0DEF4, text2 = 0x908CAA, accent = 0xEBBCBA, accentSoft = 0xC4A7E7,
        )),
    ThemeOption("catppuccin-macchiato", "Catppuccin Macchiato", p(
            bg = 0x24273A, surface = 0x363A4F, border = 0x494D64,
            text = 0xCAD3F5, text2 = 0xA5ADCB, accent = 0x8AADF4, accentSoft = 0xF5BDE6,
        )),
    ThemeOption("kanagawa-wave", "Kanagawa Wave", p(
            bg = 0x1F1F28, surface = 0x2A2A37, border = 0x363646,
            text = 0xDCD7BA, text2 = 0x727169, accent = 0x7E9CD8, accentSoft = 0x98BB6C,
        )),
    ThemeOption("everforest-dark", "Everforest Dark", p(
            bg = 0x2D353B, surface = 0x343F44, border = 0x3D484D,
            text = 0xD3C6AA, text2 = 0x859289, accent = 0xA7C080, accentSoft = 0x7FBBB3,
        )),
    ThemeOption("palenight", "Palenight", p(
            bg = 0x292D3E, surface = 0x333851, border = 0x3A3F58,
            text = 0xA6ACCD, text2 = 0x676E95, accent = 0x82AAFF, accentSoft = 0xC792EA,
        )),
    ThemeOption("material-ocean", "Material Ocean", p(
            bg = 0x0F111A, surface = 0x1A1C25, border = 0x1F2233,
            text = 0xA6ACCD, text2 = 0x4B526D, accent = 0x82AAFF, accentSoft = 0xC3E88D,
        )),
    ThemeOption("monokai-pro", "Monokai Pro", p(
            bg = 0x2D2A2E, surface = 0x353236, border = 0x403E41,
            text = 0xFCFCFA, text2 = 0x939293, accent = 0xFFD866, accentSoft = 0xA9DC76,
        )),
    ThemeOption("rose-pine-moon", "Rosé Pine Moon", p(
            bg = 0x232136, surface = 0x2A273F, border = 0x393552,
            text = 0xE0DEF4, text2 = 0x908CAA, accent = 0xEA9A97, accentSoft = 0xC4A7E7,
        )),
    ThemeOption("flexoki-dark", "Flexoki Dark", p(
            bg = 0x100F0F, surface = 0x1C1B1A, border = 0x282726,
            text = 0xCECDC3, text2 = 0x878580, accent = 0x4385BE, accentSoft = 0x879A39,
        )),
    ThemeOption("solarized-light", "Solarized Light", p(
            bg = 0xFDF6E3, surface = 0xEEE8D5, border = 0xE3DCC4,
            text = 0x586E75, text2 = 0x93A1A1, accent = 0x268BD2, accentSoft = 0x2AA198,
        )),
    ThemeOption("gruvbox-light", "Gruvbox Light", p(
            bg = 0xFBF1C7, surface = 0xF2E5BC, border = 0xE6D8A8,
            text = 0x3C3836, text2 = 0x7C6F64, accent = 0xB57614, accentSoft = 0x427B58,
        )),
    ThemeOption("atom-one-light", "Atom One Light", p(
            bg = 0xFAFAFA, surface = 0xF0F0F0, border = 0xE1E1E3,
            text = 0x383A42, text2 = 0x8B8D94, accent = 0x4078F2, accentSoft = 0x50A14F,
        )),
    ThemeOption("catppuccin-latte", "Catppuccin Latte", p(
            bg = 0xEFF1F5, surface = 0xE6E9EF, border = 0xCCD0DA,
            text = 0x4C4F69, text2 = 0x8C8FA1, accent = 0x1E66F5, accentSoft = 0xEA76CB,
        )),
    ThemeOption("sepia-paper", "Sepia Paper", p(
            bg = 0xF4ECD8, surface = 0xECE2C8, border = 0xDDD0B0,
            text = 0x5B4636, text2 = 0x8D7D63, accent = 0xA8541F, accentSoft = 0x6B8E23,
        )),
    ThemeOption("github-light", "GitHub Light", p(
            bg = 0xFFFFFF, surface = 0xF6F8FA, border = 0xD0D7DE,
            text = 0x24292F, text2 = 0x6E7781, accent = 0x0969DA, accentSoft = 0x1A7F37,
        )),
    ThemeOption("ayu-light", "Ayu Light", p(
            bg = 0xFAFAFA, surface = 0xF3F4F5, border = 0xE7E8E9,
            text = 0x5C6166, text2 = 0x8A9199, accent = 0xF2952C, accentSoft = 0x399EE6,
        )),
    ThemeOption("tokyo-day", "Tokyo Day", p(
            bg = 0xE1E2E7, surface = 0xD4D6E4, border = 0xC4C8DA,
            text = 0x343B58, text2 = 0x6C739A, accent = 0x2E7DE9, accentSoft = 0x9854F1,
        )),
    ThemeOption("rose-pine-dawn", "Rosé Pine Dawn", p(
            bg = 0xFAF4ED, surface = 0xFFFAF3, border = 0xDFDAD9,
            text = 0x575279, text2 = 0x9893A5, accent = 0xD7827E, accentSoft = 0x907AA9,
        )),
    ThemeOption("everforest-light", "Everforest Light", p(
            bg = 0xFFFBEF, surface = 0xF4F0D9, border = 0xE6E2CC,
            text = 0x5C6A72, text2 = 0x939F91, accent = 0x8DA101, accentSoft = 0x3A94C5,
        )),
    ThemeOption("quiet-light", "Quiet Light", p(
            bg = 0xF5F5F5, surface = 0xECECEC, border = 0xDCDCDC,
            text = 0x333333, text2 = 0x888888, accent = 0x4B83CD, accentSoft = 0x7A3E9D,
        )),
    ThemeOption("kanagawa-lotus", "Kanagawa Lotus", p(
            bg = 0xF2ECBC, surface = 0xE7DBA0, border = 0xDDD5A8,
            text = 0x545464, text2 = 0x8A8980, accent = 0x4D699B, accentSoft = 0x6E915F,
        )),
    ThemeOption("flexoki-light", "Flexoki Light", p(
            bg = 0xFFFCF0, surface = 0xF2F0E5, border = 0xE6E4D9,
            text = 0x343331, text2 = 0x6F6E69, accent = 0x205EA6, accentSoft = 0x66800B,
        )),
    ThemeOption("nord-light", "Nord Light", p(
            bg = 0xECEFF4, surface = 0xE5E9F0, border = 0xD8DEE9,
            text = 0x2E3440, text2 = 0x6C7589, accent = 0x5E81AC, accentSoft = 0x5B8A5A,
        )),
    ThemeOption("material-light", "Material Light", p(
            bg = 0xFAFAFA, surface = 0xEEF0F1, border = 0xE0E2E4,
            text = 0x2F3337, text2 = 0x8A9196, accent = 0x4071B8, accentSoft = 0x5A8F2F,
        )),
    ThemeOption("papercolor-light", "PaperColor Light", p(
            bg = 0xEEEEEE, surface = 0xE4E4E4, border = 0xD0D0D0,
            text = 0x444444, text2 = 0x878787, accent = 0x0087AF, accentSoft = 0x008700,
        )),
    ThemeOption("tomorrow-light", "Tomorrow Light", p(
            bg = 0xFFFFFF, surface = 0xEFEFEF, border = 0xD6D6D6,
            text = 0x4D4D4C, text2 = 0x8E908C, accent = 0x4271AE, accentSoft = 0x4F7A28,
        )),
    ThemeOption("modus-operandi", "Modus Operandi", p(
            bg = 0xFFFFFF, surface = 0xF2F2F2, border = 0xD7D7D7,
            text = 0x000000, text2 = 0x595959, accent = 0x0031A9, accentSoft = 0x006800,
        )),
)

private val themesByKey: Map<String, LauncherPalette> = AppThemes.associate { it.key to it.palette }

fun paletteFor(key: String): LauncherPalette = themesByKey[key] ?: WarmDark

// ─────────────────────────────────────────────────────────────────────────
// Active palette + color accessors
// The composable getters keep the old names working, but now resolve to the
// currently selected theme instead of fixed constants.
// ─────────────────────────────────────────────────────────────────────────

/**
 * The active palette.
 *
 * [compositionLocalOf] rather than `staticCompositionLocalOf`: the palette changes
 * when the user picks a theme, and tracking reads means only the ~12 colour readers
 * recompose instead of the entire composition.
 */
val LocalPalette = compositionLocalOf { WarmDark }

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