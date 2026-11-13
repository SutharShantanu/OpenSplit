package com.opensplit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.opensplit.ui.components.ActionFabMenu
import com.opensplit.ui.components.ConnectedChoiceGroup
import com.opensplit.ui.components.FabAction
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Golden images for the M3 Expressive building blocks; `./gradlew recordRoborazziDebug` refreshes them. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExpressiveComponentsScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun connectedChoiceGroup() {
        compose.setContent {
            OpenSplitTheme(darkTheme = false, dynamicColor = false) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ConnectedChoiceGroup(
                        options = listOf("System", "Light", "Dark"),
                        icons = listOf(OpenSplitIcons.AutoAwesome, OpenSplitIcons.WbSunny, OpenSplitIcons.DarkMode),
                        selectedIndex = 1,
                        onSelect = {},
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/connected_choice_group.png")
    }

    @Test
    fun fabMenuExpanded() {
        compose.setContent {
            OpenSplitTheme(darkTheme = false, dynamicColor = false) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize()) {
                        ActionFabMenu(
                            icon = OpenSplitIcons.AddExpense,
                            contentDescription = "Create",
                            actions = listOf(
                                FabAction("Add expense", OpenSplitIcons.AddExpense) {},
                                FabAction("Settle up", OpenSplitIcons.Settle) {},
                                FabAction("New group", OpenSplitIcons.GroupAdd) {},
                                FabAction("Add friend", OpenSplitIcons.PersonAdd) {}
                            ),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                        )
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Create").performClick()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/fab_menu_expanded.png")
    }
}
