/*
 * Copyright (C) 2025 Nocturne Project
 * SPDX-License-Identifier: GPL-3.0
 */

package com.nocturne.player

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

/**
 * Regression test for the bug where tapping the back arrow on the full-screen
 * search page dropped the user straight to the launcher instead of returning to
 * the app's home (songs) page.
 *
 * Root cause was the arrow's 48dp touch target overlapping the ~24dp system
 * back-gesture edge inset, so a tap also started a predictive edge-back that
 * committed after the search bar collapsed. The fix moves the expanded-state
 * icons inward to 28dp. This test drives the same path and asserts the Activity
 * is still alive and the home page is showing after the arrow tap.
 */
@OptIn(ExperimentalTestApi::class)
class NavigationBackTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun screenshot(name: String) {
        // Prefer Compose's captureToImage over UiAutomation.takeScreenshot():
        // the latter returns null on the stripped google_atd image, which
        // silently produced no screenshots. captureToImage reads straight from
        // the Compose graphics layer and works under swiftshader.
        try {
            val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
            val dir = File(
                instrumentation.targetContext.getExternalFilesDir(null),
                "test_screenshots"
            ).apply { mkdirs() }
            val outFile = File(dir, "$name.png")
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            android.util.Log.i("NavTest", "screenshot saved: ${outFile.absolutePath}")
        } catch (t: Throwable) {
            android.util.Log.e("NavTest", "screenshot '$name' failed", t)
        }
    }

    private fun nodeCount(tag: String): Int =
        composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    /**
     * A fresh install lands in the setup wizard (oobeStatus 0). Dismiss it by
     * tapping "Skip", which sets oobeStatus to the current version and pops back
     * to the home page. Retries while the wizard is still animating in.
     */
    private fun dismissWizardIfPresent() {
        repeat(20) {
            composeRule.waitForIdle()
            if (nodeCount("oobe_skip") > 0) {
                composeRule.onAllNodesWithTag("oobe_skip").onFirst().performClick()
                composeRule.waitForIdle()
                return
            }
            if (nodeCount("top_search_icon") > 0 && nodeCount("oobe_skip") == 0) {
                // Already on the home page (e.g. oobe previously completed).
                return
            }
            Thread.sleep(300)
        }
    }

    @Test
    fun backArrowFromSearch_returnsHome_notLauncher() {
        // 1. Get past the setup wizard to the home (songs) page.
        dismissWizardIfPresent()
        composeRule.waitUntil(20_000) { nodeCount("top_search_icon") > 0 }
        composeRule.waitForIdle()
        screenshot("01_home")

        // 2. Open the full-screen search page.
        composeRule.onNodeWithTag("top_search_icon").performClick()
        composeRule.waitUntil(20_000) { nodeCount("search_back_arrow") > 0 }
        composeRule.onNodeWithTag("search_back_arrow").assertIsDisplayed()
        screenshot("02_search_expanded")

        // 3. Tap the back arrow — this used to finish the task to the launcher.
        composeRule.onNodeWithTag("search_back_arrow").performClick()

        // 4. The home page must be showing again and the Activity must survive.
        composeRule.waitUntil(20_000) { nodeCount("top_search_icon") > 0 }
        composeRule.waitForIdle()
        screenshot("03_after_back")

        composeRule.onNodeWithTag("top_search_icon").assertIsDisplayed()
        assertFalse(
            "Tapping the search back arrow finished the Activity (dropped to launcher)",
            composeRule.activity.isFinishing
        )
    }
}
