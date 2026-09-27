package com.rakshyaa.rakshyaa.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rakshyaa.rakshyaa.ui.components.SOSButtonContent
import com.rakshyaa.rakshyaa.viewmodels.SOSViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SOSCountdownTest {
    @get:Rule val compose = createComposeRule()

    @Test fun countdownReplacesActivationButtonAndCanBeCancelled() {
        var cancelled = false
        compose.setContent {
            MaterialTheme {
                SOSButtonContent(
                    uiState = SOSViewModel.UiState(isSosActivating = true, sosActivationCountdown = 3),
                    onActivateClick = {}, onDeactivateClick = {}, onCancelClick = { cancelled = true }
                )
            }
        }
        compose.onNodeWithText("ACTIVATE SOS").assertDoesNotExist()
        compose.onNodeWithText("3").assertIsDisplayed()
        compose.onNodeWithText("Cancel activation").performClick()
        compose.runOnIdle { assertTrue(cancelled) }
    }
}
