package com.example.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun SignupScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Error) {
            Toast.makeText(context, (uiState as AuthUiState.Error).message, Toast.LENGTH_LONG).show()
            viewModel.resetState()
        } else if (uiState is AuthUiState.Success) {
            Toast.makeText(context, (uiState as AuthUiState.Success).message, Toast.LENGTH_LONG).show()
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword
        )
        if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
            Text("Passwords do not match", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Start).padding(start = 16.dp, top = 4.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (password == confirmPassword) {
                    viewModel.signUpWithEmail(email, password, displayName)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = uiState !is AuthUiState.Loading && email.isNotBlank() && password.isNotBlank() && displayName.isNotBlank() && password == confirmPassword
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Sign Up")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Google Sign In Button
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    try {
                        val credentialManager = androidx.credentials.CredentialManager.create(context)
                        
                        val serverClientId = context.getString(context.resources.getIdentifier("default_web_client_id", "string", context.packageName))
                        
                        val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(serverClientId)
                            .build()
                            
                        val request = androidx.credentials.GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        val result = credentialManager.getCredential(context, request)
                        val credential = result.credential
                        
                        val googleIdTokenCredential = try {
                            if (credential is com.google.android.libraries.identity.googleid.GoogleIdTokenCredential) {
                                credential
                            } else {
                                com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                            }
                        } catch (e: Exception) {
                            null
                        }

                        if (googleIdTokenCredential != null) {
                            viewModel.signInWithGoogle(
                                idToken = googleIdTokenCredential.idToken,
                                displayName = googleIdTokenCredential.displayName,
                                email = googleIdTokenCredential.id,
                                photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                            )
                        } else {
                            Toast.makeText(context, "Unexpected credential type: ${credential.type}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: androidx.credentials.exceptions.GetCredentialException) {
                        if (e.message?.contains("credentials available", ignoreCase = true) == true) {
                            Toast.makeText(context, "No Google account found. Please add one.", Toast.LENGTH_LONG).show()
                            val intent = android.content.Intent(android.provider.Settings.ACTION_ADD_ACCOUNT).apply {
                                putExtra(android.provider.Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                            }
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "Google Sign-In failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Google Sign-In error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Sign in with Google")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account?")
            TextButton(onClick = onNavigateToLogin) {
                Text("Log In")
            }
        }
    }
}
