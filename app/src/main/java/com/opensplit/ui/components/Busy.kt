package com.opensplit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.OpenSplitTokens

/**
 * App-wide "something is happening" state. Any screen calls `busy.run("Saving…") { … }` (or
 * show/hide around a callback) and a small pill with the expressive loading indicator and the
 * label appears above the snackbar until the work finishes.
 */
class BusyController {
    var label by mutableStateOf<String?>(null)
        private set

    fun show(text: String) { label = text }
    fun hide() { label = null }

    suspend fun <T> run(text: String, block: suspend () -> T): T {
        show(text)
        try { return block() } finally { hide() }
    }
}

val LocalBusy = staticCompositionLocalOf<BusyController> { BusyController() }

/** The pill itself. Rendered once, by MainActivity, above the snackbar host. */
@Composable
fun BusyPill(controller: BusyController, modifier: Modifier = Modifier) {
    val label = controller.label
    AnimatedVisibility(
        visible = label != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        Surface(
            shape = OpenSplitTokens.PillShape,
            color = MaterialTheme.colorScheme.inverseSurface,
            tonalElevation = 3.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                AppLoadingIndicator(size = 20.dp, color = MaterialTheme.colorScheme.inversePrimary)
                Spacer(Modifier.width(10.dp))
                Text(label ?: "", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.inverseOnSurface)
            }
        }
    }
}

/** Inline variant for a row inside a form: spinner + text, same look everywhere. */
@Composable
fun InlineBusy(text: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        AppLoadingIndicator(size = 18.dp)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
