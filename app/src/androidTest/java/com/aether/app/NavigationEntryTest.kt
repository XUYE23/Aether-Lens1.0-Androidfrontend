package com.aether.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aether.app.ui.screens.AetherWorkspaceScreen
import com.aether.app.ui.screens.PersonalSpaceScreen
import org.junit.Rule
import org.junit.Test

class NavigationEntryTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun workspacePersonalChipInvokesNavigation() {
        var opened = false

        composeRule.setContent {
            AetherWorkspaceScreen(
                userName = "Aether",
                onNavigateToPersonal = { opened = true }
            )
        }

        composeRule.onNodeWithContentDescription("Open personal space").performClick()

        composeRule.runOnIdle {
            assert(opened)
        }
    }

    @Test
    fun personalSpaceBackArrowInvokesBackNavigation() {
        var returned = false

        composeRule.setContent {
            PersonalSpaceScreen(
                userName = "Aether",
                onBack = { returned = true }
            )
        }

        composeRule.onNodeWithText("YOUR SPACE").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back to workspace").performClick()

        composeRule.runOnIdle {
            assert(returned)
        }
    }
}
