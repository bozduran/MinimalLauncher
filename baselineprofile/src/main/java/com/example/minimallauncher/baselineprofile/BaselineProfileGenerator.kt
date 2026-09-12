package com.example.minimallauncher.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the launcher's critical start-up path (USER_STORIES.md PERF-3).
 *
 * A launcher's headline performance metric is how fast it paints after Home is
 * pressed, so the profile covers exactly that: press Home, wait for the activity,
 * then open the drawer — the second-coldest path a user hits, and the one that pays
 * for the search field and its focus handling.
 *
 * This is generated on a device, never hand-written:
 *     ./gradlew :app:generateBaselineProfile
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(packageName = TARGET_PACKAGE) {
            pressHome()
            startActivityAndWait()

            // Drawer: swipe up from the lower third to the upper third.
            val centreX = device.displayWidth / 2
            device.swipe(
                centreX,
                (device.displayHeight * 0.8f).toInt(),
                centreX,
                (device.displayHeight * 0.2f).toInt(),
                20,
            )
            device.waitForIdle()
        }
    }

    private companion object {
        const val TARGET_PACKAGE = "com.example.minimallauncher"
    }
}
