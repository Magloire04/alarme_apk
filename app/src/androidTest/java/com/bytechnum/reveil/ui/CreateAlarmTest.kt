package com.bytechnum.reveil.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bytechnum.reveil.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class CreateAlarmTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun createdAlarmAppearsInList() {
        composeRule.onNodeWithContentDescription("Ajouter une alarme").performClick()
        // Le bouton est sous le bas de l'écran : un clic sans défilement tomberait hors de l'écran.
        composeRule.onNodeWithText("Enregistrer").performScrollTo().performClick()

        // L'enregistrement passe par la base, que Compose ne sait pas attendre seul.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("07:00").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("07:00").onFirst().assertIsDisplayed()
    }
}
