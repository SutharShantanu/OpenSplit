@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
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
import com.opensplit.di.AppContainer
import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens

/** Landing for a friend link: accepts immediately and jumps to the new friend's page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptFriendScreen(token: String, appContainer: AppContainer, onNavigateBack: () -> Unit, onAccepted: (uid: String) -> Unit) {
    var error by remember { mutableStateOf<String?>(null) }
    val busy = com.opensplit.ui.components.LocalBusy.current
    LaunchedEffect(token) {
        val me = appContainer.authRepository.getCurrentUserId() ?: return@LaunchedEffect
        val currency = appContainer.userRepository.getUser(me)?.defaultCurrency ?: "INR"
        busy.run("Adding friend…") { appContainer.friendInviteRepository.acceptFriendLink(token, me, currency) }
            .fold(onSuccess = { onAccepted(it) }, onFailure = { error = it.message ?: "This friend link doesn't work." })
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Add friend", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(shapes = IconButtonDefaults.shapes(), onClick = onNavigateBack) { SymbolIcon(OpenSplitIcons.Back, contentDescription = "Back") } })
    }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(OpenSplitTokens.SpaceXL), contentAlignment = Alignment.Center) {
            if (error == null) com.opensplit.ui.components.AppLoadingIndicator() else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SymbolIcon(OpenSplitIcons.Link, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(OpenSplitTokens.SpaceLG))
                Text("This link won't work", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
                Text(error!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(OpenSplitTokens.SpaceXL))
                OutlinedButton(shapes = ButtonDefaults.shapes(), onClick = onNavigateBack) { Text("Back") }
            }
        }
    }
}
