package com.pocketpet.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pocketpet.core.designsystem.theme.PocketPetTheme
import com.pocketpet.feature.onboarding.WelcomeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Requires a device or emulator (`./gradlew connectedAndroidTest`) — not part of the unit-test-only
 * CI workflow in this repo, since provisioning an emulator in CI is a separate, heavier setup.
 * See the README's "Instrumentation tests" section for how to run this locally.
 */
@RunWith(AndroidJUnit4::class)
class WelcomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tappingGetStarted_invokesCallback() {
        var tapped = false

        composeTestRule.setContent {
            PocketPetTheme {
                WelcomeScreen(onGetStarted = { tapped = true })
            }
        }

        composeTestRule.onNodeWithText("Get started").performClick()

        assert(tapped) { "Expected onGetStarted to be invoked after tapping the button." }
    }

    @Test
    fun welcomeScreen_showsHeadline() {
        composeTestRule.setContent {
            PocketPetTheme {
                WelcomeScreen(onGetStarted = {})
            }
        }

        composeTestRule.onNodeWithText("Meet Pocket Pet").assertExists()
    }
}
