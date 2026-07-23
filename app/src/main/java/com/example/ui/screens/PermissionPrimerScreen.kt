package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.PermissionPrimerViewModel
import com.example.ui.viewmodel.PermissionStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionPrimerScreen(
    viewModel: PermissionPrimerViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val pendingSteps = remember { viewModel.getPendingSteps(context) }
    var currentStepIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(pendingSteps) {
        if (pendingSteps.isEmpty()) {
            viewModel.markPrimerCompleted()
            onComplete()
        }
    }

    if (pendingSteps.isEmpty() || currentStepIndex >= pendingSteps.size) {
        LaunchedEffect(Unit) {
            viewModel.markPrimerCompleted()
            onComplete()
        }
        return
    }

    val currentStep = pendingSteps[currentStepIndex]

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        if (currentStepIndex + 1 < pendingSteps.size) {
            currentStepIndex++
        } else {
            viewModel.markPrimerCompleted()
            onComplete()
        }
    }

    fun advanceStep() {
        if (currentStepIndex + 1 < pendingSteps.size) {
            currentStepIndex++
        } else {
            viewModel.markPrimerCompleted()
            onComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Setup", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = {
                        viewModel.markPrimerCompleted()
                        onComplete()
                    }) {
                        Text("Skip All")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier
                        .padding(28.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Step indicator
                    Text(
                        text = "Step ${currentStepIndex + 1} of ${pendingSteps.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Step Icon
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val icon = when (currentStep) {
                                PermissionStep.NOTIFICATIONS -> Icons.Rounded.Notifications
                                PermissionStep.CONTACTS -> Icons.Rounded.Contacts
                                PermissionStep.CAMERA -> Icons.Rounded.CameraAlt
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = currentStep.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = currentStep.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Primary button: Allow
                    Button(
                        onClick = {
                            val perm = currentStep.permission
                            if (perm != null) {
                                permissionLauncher.launch(perm)
                            } else {
                                advanceStep()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Allow", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary button: Not now
                    OutlinedButton(
                        onClick = { advanceStep() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Not now", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
