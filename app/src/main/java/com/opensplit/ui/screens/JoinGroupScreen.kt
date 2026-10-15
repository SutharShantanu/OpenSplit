@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.opensplit.data.repository.JoinException
import com.opensplit.di.AppContainer
import com.opensplit.domain.model.InviteLink
import com.opensplit.domain.model.JoinError
import com.opensplit.domain.model.validate
import com.opensplit.ui.components.GroupAvatar
import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import java.text.SimpleDateFormat
import java.util.Locale

/** Landing screen for an invite link: shows the group, validates the link, joins on tap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupScreen(
    token: String,
    appContainer: AppContainer,
    onNavigateBack: () -> Unit,
    onJoined: (groupId: String) -> Unit
) {
    var link by remember { mutableStateOf<InviteLink?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<JoinError?>(null) }
    var joining by remember { mutableStateOf(false) }
    val uid = appContainer.authRepository.currentUser?.uid ?: ""
    val busy = com.opensplit.ui.components.LocalBusy.current

    LaunchedEffect(token) {
        val found = appContainer.inviteLinkRepository.get(token)
        link = found
        error = found?.validate(System.currentTimeMillis() / 1000, uid, emptyList()) ?: JoinError.NOT_FOUND
        loading = false
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Group invite", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(shapes = IconButtonDefaults.shapes(), onClick = onNavigateBack) { SymbolIcon(OpenSplitIcons.Back, contentDescription = "Back") } }
        )
    }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(OpenSplitTokens.SpaceXL), contentAlignment = Alignment.Center) {
            when {
                loading -> com.opensplit.ui.components.AppLoadingIndicator()
                error != null && error != JoinError.ALREADY_MEMBER -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SymbolIcon(OpenSplitIcons.Link, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(OpenSplitTokens.SpaceLG))
                    Text("This link won't work", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
                    Text(error!!.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(OpenSplitTokens.SpaceXL))
                    OutlinedButton(shapes = ButtonDefaults.shapes(), onClick = onNavigateBack) { Text("Back to groups") }
                }
                else -> {
                    val l = link!!
                    Card(shape = OpenSplitTokens.BrandCardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(OpenSplitTokens.SpaceXL), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD)) {
                            GroupAvatar(name = l.groupName, avatarKey = null, size = 72.dp)
                            Text("You're invited to", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(l.groupName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            val expiry = l.expiresAt?.let { "Link valid until " + SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(it.toDate()) } ?: "Link does not expire"
                            Text(expiry, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (error == JoinError.ALREADY_MEMBER) {
                                Button(shapes = ButtonDefaults.shapes(), onClick = { onJoined(l.groupId) }, modifier = Modifier.fillMaxWidth()) { Text("Open group") }
                            } else {
                                Button(shapes = ButtonDefaults.shapes(), 
                                    enabled = !joining,
                                    onClick = { joining = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(if (joining) "Joining…" else "Join group") }
                            }
                        }
                    }
                    if (joining) {
                        LaunchedEffect(Unit) {
                            val result = busy.run("Joining group…") { appContainer.inviteLinkRepository.join(token, uid) }
                            joining = false
                            result.fold(
                                onSuccess = { onJoined(it) },
                                onFailure = { e -> error = (e as? JoinException)?.error ?: JoinError.NOT_FOUND }
                            )
                        }
                    }
                }
            }
        }
    }
}
