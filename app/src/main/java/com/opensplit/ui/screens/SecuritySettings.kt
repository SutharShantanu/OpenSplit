@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import com.opensplit.ui.theme.Symbol
import com.opensplit.data.ai.AI_PROVIDERS_CATALOG
import com.opensplit.data.ai.AiProviderInfo
import com.opensplit.data.ai.AiFeature
import com.opensplit.ui.components.SettingsGroupCard
import com.opensplit.ui.components.SettingsGroupDivider
import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.components.responsiveContentWidth
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.opensplit.di.AppContainer
import com.opensplit.ui.components.ExportBottomSheet
import com.opensplit.ui.components.LocalSnackbarController
import com.opensplit.ui.components.StateLayout
import com.opensplit.ui.components.appHazeHeader
import com.opensplit.ui.components.appHazeSource
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.viewmodel.AccountUiState
import com.opensplit.ui.viewmodel.AccountViewModel
import com.opensplit.util.CurrencyFormatter
import dev.chrisbanes.haze.HazeState

/* =========================================================================================
 * 7. SUB-SCREEN: ACCOUNT & SECURITY
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSecuritySubScreen(
    data: AccountUiState,
    onUpdateDisplayName: (String) -> Unit,
    onUpdateUpiId: (String?) -> Unit = {},
    onSendPasswordReset: (String) -> Unit,
    onDeleteAccountClick: () -> Unit,
    onBack: () -> Unit
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showUpiDialog by remember { mutableStateOf(false) }
    var editUpi by remember(data.user.upiId) { mutableStateOf(data.user.upiId.orEmpty()) }
    var showPasswordResetDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(data.user.displayName ?: "") }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .responsiveContentWidth(maxWidth = 760.dp)
        ) {
            androidx.compose.material3.TopAppBar(
                title = { Text("Account & Security", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    com.opensplit.ui.components.RoundBackButton(onClick = onBack)
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item {
                SettingsGroupCard(title = "Profile Information", icon = OpenSplitIcons.Person) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNameDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Display Name", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(data.user.displayName ?: "Not set", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                        SymbolIcon(OpenSplitIcons.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }

                    SettingsGroupDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showUpiDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("UPI ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                data.user.upiId ?: "Add so friends can pay you via UPI",
                                style = if (data.user.upiId != null) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                                fontWeight = if (data.user.upiId != null) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        SymbolIcon(OpenSplitIcons.Edit, contentDescription = "Edit UPI ID", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }

                    SettingsGroupDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Linked Email", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(data.user.email ?: "", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                        SymbolIcon(OpenSplitIcons.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Password reset only means something for email/password accounts, not Google sign-in.
            if (data.authProvider == "Email") item {
                SettingsGroupCard(title = "Password & Access", icon = OpenSplitIcons.Key) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPasswordResetDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reset Password", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Send a secure password reset link to your email", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        SymbolIcon(OpenSplitIcons.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDeleteAccountClick,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SymbolIcon(OpenSplitIcons.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account Permanently", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

    if (showUpiDialog) {
        val valid = editUpi.isBlank() || com.opensplit.util.Upi.isValidVpa(editUpi.trim())
        AlertDialog(
            onDismissRequest = { showUpiDialog = false },
            title = { Text("UPI ID", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editUpi,
                    onValueChange = { editUpi = it },
                    label = { Text("name@bank") },
                    singleLine = true,
                    isError = !valid,
                    supportingText = { Text(if (valid) "Leave empty to remove" else "Looks like name@bank") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), enabled = valid, onClick = {
                    onUpdateUpiId(editUpi.trim().ifBlank { null })
                    showUpiDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = { showUpiDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Edit Display Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(shapes = com.opensplit.ui.theme.appButtonShapes(), 
                    onClick = {
                        if (editName.isNotBlank()) {
                            onUpdateDisplayName(editName.trim())
                            showNameDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = { showNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPasswordResetDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordResetDialog = false },
            title = { Text("Send Password Reset?", fontWeight = FontWeight.Bold) },
            text = { Text("A secure password reset link will be sent to ${data.user.email}") },
            confirmButton = {
                Button(shapes = com.opensplit.ui.theme.appButtonShapes(), 
                    onClick = {
                        onSendPasswordReset(data.user.email ?: "")
                        showPasswordResetDialog = false
                    }
                ) {
                    Text("Send Email")
                }
            },
            dismissButton = {
                TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = { showPasswordResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

