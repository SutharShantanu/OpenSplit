package com.opensplit.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * App-wide snackbar access.
 *
 * Exposed through [LocalSnackbarController] so any composable — including ones nested deep in
 * dialogs and bottom sheets — can confirm an action without prop-drilling a host state down.
 */
@Immutable
class SnackbarController(
    val hostState: SnackbarHostState,
    private val scope: CoroutineScope
) {
    /** Plain confirmation, e.g. "Expense saved". */
    fun showMessage(message: String) {
        scope.launch {
            // Replace any in-flight snackbar so rapid actions don't queue up behind each other.
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    /**
     * Confirmation with an Undo affordance for reversible/destructive actions.
     * [onUndo] runs only if the user actually taps Undo.
     */
    fun showUndo(message: String, actionLabel: String = "Undo", onUndo: () -> Unit) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }
}

/**
 * Defaults to a no-op-ish controller so previews and tests don't crash; the real one is
 * provided once at the top of the app in MainActivity.
 */
val LocalSnackbarController = staticCompositionLocalOf<SnackbarController> {
    error("No SnackbarController provided — wrap content in ProvideSnackbarController()")
}
