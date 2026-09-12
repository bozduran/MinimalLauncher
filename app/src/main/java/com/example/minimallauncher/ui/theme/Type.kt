package com.example.minimallauncher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.minimallauncher.R

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_light, FontWeight.Light),
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

// Apply JetBrains Mono to every Material text style so anything we
// don't style explicitly still inherits the font.
private val base = Typography()
val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = JetBrainsMono),
    displayMedium = base.displayMedium.copy(fontFamily = JetBrainsMono),
    displaySmall = base.displaySmall.copy(fontFamily = JetBrainsMono),
    headlineLarge = base.headlineLarge.copy(fontFamily = JetBrainsMono),
    headlineMedium = base.headlineMedium.copy(fontFamily = JetBrainsMono),
    headlineSmall = base.headlineSmall.copy(fontFamily = JetBrainsMono),
    titleLarge = base.titleLarge.copy(fontFamily = JetBrainsMono),
    titleMedium = base.titleMedium.copy(fontFamily = JetBrainsMono),
    titleSmall = base.titleSmall.copy(fontFamily = JetBrainsMono),
    bodyLarge = base.bodyLarge.copy(fontFamily = JetBrainsMono),
    bodyMedium = base.bodyMedium.copy(fontFamily = JetBrainsMono),
    bodySmall = base.bodySmall.copy(fontFamily = JetBrainsMono),
    labelLarge = base.labelLarge.copy(fontFamily = JetBrainsMono),
    labelMedium = base.labelMedium.copy(fontFamily = JetBrainsMono),
    labelSmall = base.labelSmall.copy(fontFamily = JetBrainsMono),
)

/**
 * The text styles the launcher UI actually uses.
 *
 * Screens previously repeated the same `fontFamily`/`fontWeight`/`fontSize`
 * triples inline, so a typographic change meant editing every call site — and
 * `AppTypography` (the Material scale) was bypassed entirely. These keep the
 * exact values the design already had while giving each role one definition.
 */
object AppTextStyles {

    val Clock = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Light,
        fontSize = 66.sp,
        lineHeight = 70.sp,
    )

    val Date = TextStyle(fontFamily = JetBrainsMono, fontSize = 15.sp)

    val Favorite = TextStyle(fontFamily = JetBrainsMono, fontSize = 23.sp)

    /** Onboarding/error hints on the home screen. */
    val Hint = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, lineHeight = 20.sp)

    /** The "…and N more" overflow row and other 18sp list rows. */
    val ListRow = TextStyle(fontFamily = JetBrainsMono, fontSize = 18.sp)

    /** The "↑ apps" footer. */
    val Footer = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp)

    /** Drawer rows, and the search field's own text. */
    val DrawerItem = TextStyle(fontFamily = JetBrainsMono, fontSize = 20.sp)

    val SearchClear = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Light,
        fontSize = 26.sp,
    )

    val MenuHeader = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
    )

    val MenuRow = TextStyle(fontFamily = JetBrainsMono, fontSize = 16.sp)

    val ScreenTitle = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
    )

    val BackSymbol = TextStyle(fontFamily = JetBrainsMono, fontSize = 30.sp)

    val SectionLabel = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp)

    val ThemeLabel = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp)
}
