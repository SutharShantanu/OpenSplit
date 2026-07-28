package com.opensplit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.opensplit.di.AppContainer
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import kotlinx.coroutines.launch

private enum class PasswordStrength(val label: String) { WEAK("Weak"), MEDIUM("Medium"), STRONG("Strong") }

private fun computeStrength(password: String): PasswordStrength {
    if (password.length < 8) return PasswordStrength.WEAK
    var score = 0
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return when {
        password.length >= 12 && score >= 3 -> PasswordStrength.STRONG
        score >= 2 -> PasswordStrength.MEDIUM
        else -> PasswordStrength.WEAK
    }
}

/**
 * Shown when the app is opened via a Firebase Auth "resetPassword" action link (see the
 * App Link intent-filter in AndroidManifest.xml + MainActivity's deep-link extraction).
 * Confirms the reset in-app with [oobCode] instead of Firebase's hosted web page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetNewPasswordScreen(
    oobCode: String,
    appContainer: AppContainer,
    onDone: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var repeatPassword by remember { mutableStateOf("") }
    var newVisible by remember { mutableStateOf(false) }
    var repeatVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val strength = computeStrength(newPassword)
    val passwordsMatch = newPassword.isNotEmpty() && newPassword == repeatPassword
    val isValid = newPassword.isNotEmpty() && strength != PasswordStrength.WEAK && passwordsMatch

    Scaffold(
        topBar = { TopAppBar(title = { Text("Set New Password", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(OpenSplitTokens.SpaceLG),
            verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD)
        ) {
            when {
                success -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            OpenSplitIcons.Success,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
                        Text("Password updated", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                        Text(
                            "You can now sign in with your new password.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceLG))
                        Button(onClick = onDone) { Text("Continue") }
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            OpenSplitIcons.ErrorIcon,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
                        Text("Couldn't reset password", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                        Text(
                            "${errorMessage}\n\nThis link may have expired — request a new one from Account & Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceLG))
                        Button(onClick = onDone) { Text("Close") }
                    }
                }
                else -> {
                    Text(
                        "Choose a new password for your account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New password") },
                        singleLine = true,
                        visualTransformation = if (newVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { newVisible = !newVisible }) {
                                Icon(
                                    if (newVisible) OpenSplitIcons.VisibilityOff else OpenSplitIcons.VisibilityOn,
                                    contentDescription = if (newVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (newPassword.isNotEmpty()) {
                        val (barColor, fraction) = when (strength) {
                            PasswordStrength.WEAK -> MaterialTheme.colorScheme.error to 1f / 3f
                            PasswordStrength.MEDIUM -> Color(0xFFE6A400) to 2f / 3f
                            PasswordStrength.STRONG -> Color(0xFF2E7D32) to 1f
                        }
                        Column {
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = barColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                            Text(
                                text = "Strength: ${strength.label}" + if (strength == PasswordStrength.WEAK) " — use 8+ characters with a mix of letters, numbers & symbols" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = barColor
                            )
                        }
                    }

                    OutlinedTextField(
                        value = repeatPassword,
                        onValueChange = { repeatPassword = it },
                        label = { Text("Repeat new password") },
                        singleLine = true,
                        isError = repeatPassword.isNotEmpty() && !passwordsMatch,
                        visualTransformation = if (repeatVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { repeatVisible = !repeatVisible }) {
                                Icon(
                                    if (repeatVisible) OpenSplitIcons.VisibilityOff else OpenSplitIcons.VisibilityOn,
                                    contentDescription = if (repeatVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        supportingText = {
                            if (repeatPassword.isNotEmpty() && !passwordsMatch) {
                                Text("Passwords do not match", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            isSubmitting = true
                            scope.launch {
                                val result = appContainer.authRepository.confirmPasswordReset(oobCode, newPassword)
                                isSubmitting = false
                                if (result.isSuccess) {
                                    success = true
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Something went wrong."
                                }
                            }
                        },
                        enabled = isValid && !isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                            Text("Updating...")
                        } else {
                            Text("Update password", fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
