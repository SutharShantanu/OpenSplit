package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.repository.SplitRepository
import com.example.ui.screens.MainDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.SplitViewModel
import com.example.ui.viewmodel.SplitViewModelFactory
import kotlinx.coroutines.delay
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.workers.SyncWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase. This handles cases where google-services plugin is missing during dev.
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueue(syncRequest)

        // Initialize local persistence architecture
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = SplitRepository(database.splitDao())
        
        // Setup state holder viewmodel
        val factory = SplitViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[SplitViewModel::class.java]

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf("login") }
                
                if (currentScreen == "login") {
                    LoginScreen(onLoginSuccess = { currentScreen = "dashboard" })
                } else {
                    MainDashboard(viewModel = viewModel)
                }
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val _authState = kotlinx.coroutines.flow.MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    fun signInWithGoogle(context: Context, onLoginSuccess: () -> Unit) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val clientId = context.getString(com.example.R.string.default_web_client_id)
                if (clientId.contains("YOUR_CLIENT_ID")) {
                    _authState.value = AuthState.Error("Web Client ID missing. Configure Google Sign-in in Firebase Console and update strings.xml or google-services.json")
                    return@launch
                }

                val auth = FirebaseAuth.getInstance()
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId) 
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential
                
                if (credential is com.google.android.libraries.identity.googleid.GoogleIdTokenCredential) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                    auth.signInWithCredential(firebaseCredential)
                    _authState.value = AuthState.Authenticated
                    onLoginSuccess()
                } else {
                    _authState.value = AuthState.Error("Unexpected credential type")
                }
            } catch (e: androidx.credentials.exceptions.GetCredentialException) {
                e.printStackTrace()
                bypassLoginForEmulator(onLoginSuccess)
            } catch (e: Exception) {
                e.printStackTrace()
                _authState.value = AuthState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun bypassLoginForEmulator(onLoginSuccess: () -> Unit) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _authState.value = AuthState.Authenticated
            onLoginSuccess()
        }
    }
}

@Composable
fun LoginScreen(authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(), onLoginSuccess: () -> Unit) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            Toast.makeText(context, "Sign-In Failed: ${(authState as AuthState.Error).message}", Toast.LENGTH_LONG).show()
            authViewModel.resetState()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "App Logo",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Welcome to SplitIt",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Sign in to manage your expenses seamlessly",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = {
                        authViewModel.signInWithGoogle(context, onLoginSuccess)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Mock Google Icon
                        Text("G", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFFDB4437))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Sign in with Google", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.material3.TextButton(
                    onClick = { authViewModel.bypassLoginForEmulator(onLoginSuccess) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Emulator Bypass (Dev Mode)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (authState is AuthState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) { },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
