package com.opensplit.ui.screens.auth

import com.opensplit.ui.components.LocalSnackbarController

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.opensplit.R
import com.opensplit.ui.components.GoogleSignInButton
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.viewmodel.AuthUiState
import com.opensplit.ui.viewmodel.AuthViewModel

/**
 * First screen for signed-out users. Branding is anchored to the top, a single
 * "Continue with Google" button is pinned to the bottom — Google is the only
 * supported sign-in method.
 */
@Composable
fun WelcomeScreen(viewModel: AuthViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Error) {
            snackbar.showMessage((uiState as AuthUiState.Error).message)
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ---- Top: branding & AI pill ----
        Spacer(modifier = Modifier.height(16.dp))
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "OpenSplit",
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome to OpenSplit",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = "✨ Powered by Gemini AI & Local-First Engine",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Split expenses fairly with anyone — zero fees, local-first speed, smart AI insights.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.95f)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ---- Middle: feature highlights ----
        FeatureRow(
            icon = OpenSplitIcons.Groups,
            title = "Fair & Flexible Splitting",
            subtitle = "Equal, exact, percentage, shares or itemized."
        )
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow(
            icon = OpenSplitIcons.Settle,
            title = "Settle Up in a Tap",
            subtitle = "Smart debt simplification keeps payments minimal."
        )
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow(
            icon = OpenSplitIcons.Analytics,
            title = "AI Receipts & Smart Insights",
            subtitle = "Instant receipt scanning and Gemini spending tips."
        )

        // ---- Bottom: Google sign-in ----
        Spacer(modifier = Modifier.weight(1f))

        GoogleSignInButton(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "By continuing you agree to split things fairly.\nFree & open-source.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(44.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
