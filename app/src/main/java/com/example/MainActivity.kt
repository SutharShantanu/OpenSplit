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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
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
            val clientId = context.getString(com.example.R.string.default_web_client_id)
            if (clientId.contains("YOUR_CLIENT_ID")) {
                _authState.value = AuthState.Error("Web Client ID missing. Configure Google Sign-in in Firebase Console and update strings.xml or google-services.json")
                return@launch
            }

            tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
                is android.app.Activity -> this
                is android.content.ContextWrapper -> baseContext.findActivity()
                else -> null
            }

            val activity = context.findActivity()
            if (activity == null) {
                _authState.value = AuthState.Error("Activity context not found")
                return@launch
            }

            try {
                val auth = FirebaseAuth.getInstance()
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId) 
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context = activity, request = request)
                val credential = result.credential
                
                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    auth.signInWithCredential(firebaseCredential)
                        .addOnSuccessListener {
                            _authState.value = AuthState.Authenticated
                            onLoginSuccess()
                        }
                        .addOnFailureListener { ex ->
                            _authState.value = AuthState.Error(ex.message ?: "Firebase Auth Failed")
                        }
                } else {
                    _authState.value = AuthState.Error("Unexpected credential type")
                }
            } catch (e: androidx.credentials.exceptions.GetCredentialException) {
                e.printStackTrace()
                if (activity != null) {
                    val provider = com.google.firebase.auth.OAuthProvider.newBuilder("google.com").build()
                    val auth = FirebaseAuth.getInstance()
                    auth.startActivityForSignInWithProvider(activity, provider)
                        .addOnSuccessListener {
                            _authState.value = AuthState.Authenticated
                            onLoginSuccess()
                        }
                        .addOnFailureListener { ex ->
                            val msg = ex.message ?: "Web SignIn Failed"
                            if (msg.contains("package certificate hash", ignoreCase = true)) {
                                _authState.value = AuthState.Error("SHA-1 hash missing in Firebase. Add 70:E0:23:2F:0E:46:6B:07:B0:29:97:5D:CE:F9:DA:07:DA:9A:FB:FD to Firebase Console and update google-services.json")
                            } else {
                                _authState.value = AuthState.Error(msg)
                            }
                        }
                } else {
                    val msg = e.message ?: "SignIn Failed"
                    if (msg.contains("package certificate hash", ignoreCase = true)) {
                        _authState.value = AuthState.Error("SHA-1 hash missing in Firebase. Add 70:E0:23:2F:0E:46:6B:07:B0:29:97:5D:CE:F9:DA:07:DA:9A:FB:FD to Firebase Console and update google-services.json")
                    } else {
                        _authState.value = AuthState.Error(msg)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = e.message ?: "Unknown error occurred"
                if (msg.contains("package certificate hash", ignoreCase = true)) {
                    _authState.value = AuthState.Error("SHA-1 hash missing in Firebase. Add 70:E0:23:2F:0E:46:6B:07:B0:29:97:5D:CE:F9:DA:07:DA:9A:FB:FD to Firebase Console and update google-services.json")
                } else {
                    _authState.value = AuthState.Error(msg)
                }
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

// Auth Screens and Options
enum class AuthMode {
    OPTIONS, EMAIL_LOGIN, EMAIL_SIGNUP, FORGOT_PASSWORD, PHONE_OTP
}

@Composable
fun LoginScreen(authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(), onLoginSuccess: () -> Unit) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current

    var currentAuthMode by remember { mutableStateOf(AuthMode.OPTIONS) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+91") }
    var isOtpSent by remember { mutableStateOf(false) }
    var otpValue by remember { mutableStateOf("") }
    var countryMenuExpanded by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            Toast.makeText(context, "Sign-In Failed: ${(authState as AuthState.Error).message}", Toast.LENGTH_LONG).show()
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            authViewModel.resetState()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.background
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, gradientOffset),
                    end = androidx.compose.ui.geometry.Offset(gradientOffset, 1500f)
                )
            )
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Section (Branding & Logo)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "App Logo",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "SplitShare",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The modern expense-sharing app",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bottom Section (Forms & Auth choices)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            ) {
                when (currentAuthMode) {
                    AuthMode.OPTIONS -> {
                        Button(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                authViewModel.signInWithGoogle(context, onLoginSuccess)
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text("G", fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Continue with Google", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { currentAuthMode = AuthMode.EMAIL_LOGIN },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Continue with Email", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { currentAuthMode = AuthMode.PHONE_OTP },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Continue with Phone", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    AuthMode.EMAIL_LOGIN -> {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { onLoginSuccess() /* Mock */ },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Log In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { currentAuthMode = AuthMode.FORGOT_PASSWORD }) {
                                Text("Forgot Password?")
                            }
                            TextButton(onClick = { currentAuthMode = AuthMode.EMAIL_SIGNUP }) {
                                Text("Sign Up")
                            }
                        }
                        TextButton(onClick = { currentAuthMode = AuthMode.OPTIONS }) {
                            Text("Back")
                        }
                    }
                    
                    AuthMode.EMAIL_SIGNUP -> {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            }
                        )
                        
                        if (password.isNotEmpty()) {
                            val passwordStrength = when {
                                password.length < 6 -> 0.3f
                                password.length >= 8 && password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() } -> 1f
                                else -> 0.6f
                            }
                            val passwordColor = when {
                                passwordStrength <= 0.3f -> Color.Red
                                passwordStrength <= 0.6f -> Color(0xFFFFA500)
                                else -> Color.Green
                            }
                            val passwordText = when {
                                passwordStrength <= 0.3f -> "Weak"
                                passwordStrength <= 0.6f -> "Medium"
                                else -> "Strong"
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                                LinearProgressIndicator(
                                    progress = { passwordStrength },
                                    modifier = Modifier.weight(1f).height(4.dp),
                                    color = passwordColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(passwordText, color = passwordColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { onLoginSuccess() /* Mock */ },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { currentAuthMode = AuthMode.OPTIONS }) {
                            Text("Back")
                        }
                    }

                    AuthMode.FORGOT_PASSWORD -> {
                        Text("Reset your password", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { currentAuthMode = AuthMode.EMAIL_LOGIN },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Send Reset Link", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { currentAuthMode = AuthMode.OPTIONS }) {
                            Text("Back")
                        }
                    }

                    AuthMode.PHONE_OTP -> {
                        if (!isOtpSent) {
                            Text("Enter your phone number", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("We will send you an OTP to verify", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                // Country Code Dropdown
                                @OptIn(ExperimentalMaterial3Api::class)
                                ExposedDropdownMenuBox(
                                    expanded = countryMenuExpanded,
                                    onExpandedChange = { countryMenuExpanded = it },
                                    modifier = Modifier.width(110.dp)
                                ) {
                                    OutlinedTextField(
                                        value = countryCode,
                                        onValueChange = {},
                                        readOnly = true,
                                        singleLine = true,
                                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryMenuExpanded)
                                        }
                                    )
                                    ExposedDropdownMenu(
                                        expanded = countryMenuExpanded,
                                        onDismissRequest = { countryMenuExpanded = false }
                                    ) {
                                        val codes = listOf("+1", "+44", "+91", "+61", "+81", "+49")
                                        codes.forEach { code ->
                                            DropdownMenuItem(
                                                text = { Text(code) },
                                                onClick = {
                                                    countryCode = code
                                                    countryMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                // Phone Number Field
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text("Phone Number") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = { 
                                    if (phone.isNotEmpty()) {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        isOtpSent = true 
                                    } else {
                                        Toast.makeText(context, "Please enter phone number", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Send OTP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("Verify Phone Number", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Auto-fetching OTP from SMS...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Mocking the OTP arrival
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(1500)
                                otpValue = "123456"
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                Toast.makeText(context, "OTP Auto-filled from SMS!", Toast.LENGTH_SHORT).show()
                            }
                            
                            OutlinedTextField(
                                value = otpValue,
                                onValueChange = { otpValue = it },
                                label = { Text("6-digit OTP") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { 
                                    if (otpValue.length == 6) {
                                        onLoginSuccess() 
                                    } else {
                                        Toast.makeText(context, "Please enter 6-digit OTP", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Verify & Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { 
                            currentAuthMode = AuthMode.OPTIONS
                            isOtpSent = false
                            otpValue = ""
                            phone = ""
                        }) {
                            Text("Back")
                        }
                    }
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
