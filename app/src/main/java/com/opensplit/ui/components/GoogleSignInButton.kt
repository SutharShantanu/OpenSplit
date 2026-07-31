package com.opensplit.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.opensplit.R
import com.opensplit.ui.viewmodel.AuthUiState
import com.opensplit.ui.viewmodel.AuthViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

/**
 * Reusable "Continue with Google" button. Encapsulates the Credential Manager flow so
 * every entry point stays in sync.
 *
 * Requires a valid `default_web_client_id` string, which the google-services plugin only
 * generates when `app/google-services.json` contains a **web** OAuth client (type 3).
 * The CI/nightly placeholder config has no OAuth clients, so Google sign-in cannot succeed
 * on that build — a real Firebase config (with the signing SHA-1 registered) is required.
 */
@Composable
fun GoogleSignInButton(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    text: String = "Continue with Google"
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current
    val scope = rememberCoroutineScope()
    val loading = uiState is AuthUiState.Loading

    Button(
        onClick = {
            scope.launch {
                // Resolve the web client id the google-services plugin generates. Missing/blank
                // means the build wasn't configured with a real Firebase web client.
                val serverClientId = runCatching {
                    val resId = context.resources.getIdentifier(
                        "default_web_client_id", "string", context.packageName
                    )
                    if (resId == 0) null else context.getString(resId)
                }.getOrNull()

                if (serverClientId.isNullOrBlank()) {
                    snackbar.showMessage("Google sign-in isn't configured for this build (missing web client). " +
                            "Rebuild with a real google-services.json.")
                    return@launch
                }

                val credentialManager = CredentialManager.create(context)

                // Runs one credential request and extracts a Google ID token credential from it.
                suspend fun request(option: CredentialOption): GoogleIdTokenCredential? {
                    val req = GetCredentialRequest.Builder()
                        .addCredentialOption(option)
                        .build()
                    val credential = credentialManager.getCredential(context, req).credential
                    return when {
                        credential is GoogleIdTokenCredential -> credential
                        credential is CustomCredential &&
                            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ->
                            GoogleIdTokenCredential.createFrom(credential.data)
                        else -> null
                    }
                }

                try {
                    // 1) One-tap over any Google account already on the device.
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setAutoSelectEnabled(false)
                        .setServerClientId(serverClientId)
                        .build()

                    val credential = try {
                        request(googleIdOption)
                    } catch (e: NoCredentialException) {
                        // 2) Fall back to the explicit "Sign in with Google" chooser, which
                        //    surfaces the account picker even when one-tap has nothing to offer
                        //    (e.g. no previously-authorized account for this app).
                        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
                        request(signInOption)
                    }

                    if (credential != null) {
                        viewModel.signInWithGoogle(
                            idToken = credential.idToken,
                            displayName = credential.displayName,
                            email = credential.id,
                            photoUrl = credential.profilePictureUri?.toString()
                        )
                    } else {
                        snackbar.showMessage("Unexpected credential type from Google.")
                    }
                } catch (e: NoCredentialException) {
                    // Both flows found nothing. This is a genuine "no usable Google credential"
                    // state — either no Google account is signed in on the device, or the app's
                    // signing SHA-1 isn't registered for this Firebase OAuth client.
                    snackbar.showMessage("Couldn't get a Google account. Add a Google account in device Settings, " +
                            "and make sure this build's SHA-1 is registered in Firebase.")
                } catch (e: GetCredentialException) {
                    snackbar.showMessage("Google Sign-In failed: ${e.message}")
                } catch (e: Exception) {
                    snackbar.showMessage("Google Sign-In error: ${e.message}")
                }
            }
        },
        enabled = !loading,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        if (loading) {
            AppLoadingIndicator(size = 22.dp)
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}
