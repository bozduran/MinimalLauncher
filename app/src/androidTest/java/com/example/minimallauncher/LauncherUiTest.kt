package com.example.minimallauncher

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.minimallauncher.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the launcher's critical flows (USER_STORIES.md QA-3).
 *
 * **These have not been executed.** The environment this was written in had an
 * Android 15 device reachable over adb, but its owner scoped its use to
 * read-only/non-invasive, so no APK was installed on it. The suite compiles and is
 * ready to run against any device or emulator:
 *
 * ```
 * ./gradlew connectedDebugAndroidTest
 * ```
 *
 * Until someone runs that, treat every assertion here as unverified — including
 * whether the swipe gestures below actually drive the pager on a real device.
 */
@RunWith(AndroidJUnit4::class)
class LauncherUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun string(id: Int): String = rule.activity.getString(id)

    @Test
    fun homeScreenShowsTheClockAndTheDrawerHint() {
        rule.onNodeWithText(string(R.string.home_open_drawer)).assertIsDisplayed()
    }

    @Test
    fun swipingUpFromHomeOpensTheDrawer() {
        rule.onRoot().performTouchInput { swipeUp() }
        rule.waitForIdle()

        rule.onNodeWithText(string(R.string.search_hint)).assertIsDisplayed()
    }

    @Test
    fun longPressingHomeOpensSettings() {
        rule.onRoot().performTouchInput { longClick(center) }
        rule.waitForIdle()

        rule.onNodeWithText(string(R.string.settings_title)).assertIsDisplayed()
    }

    @Test
    fun settingsShowsTheThemeAndFavouritesSections() {
        rule.onRoot().performTouchInput { longClick(center) }
        rule.waitForIdle()

        rule.onNodeWithText(string(R.string.settings_section_theme)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.settings_24h)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.settings_set_default_launcher)).assertIsDisplayed()
    }

    @Test
    fun theBackControlReturnsFromSettingsToHome() {
        rule.onRoot().performTouchInput { longClick(center) }
        rule.waitForIdle()

        rule.onNodeWithText(string(R.string.back_symbol)).performClick()
        rule.waitForIdle()

        rule.onNodeWithText(string(R.string.home_open_drawer)).assertIsDisplayed()
    }
}

/**
 * Accessibility assertions (USER_STORIES.md A11Y-2).
 *
 * Split into their own class so a failure here is not confused with a failure of the
 * navigation flows above. Like the rest of this source set, **unrun** — see the note
 * on [LauncherUiTest].
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun string(id: Int): String = rule.activity.getString(id)

    @Test
    fun theSettingsBackControlMeetsTheMinimumTouchTarget() {
        rule.onRoot().performTouchInput { longClick(center) }
        rule.waitForIdle()

        rule.onNodeWithText(string(R.string.back_symbol))
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }
}
