package com.opensplit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.viewmodel.ScreenState

@Composable
fun <T> StateLayout(
    state: ScreenState<T>,
    modifier: Modifier = Modifier,
    emptyTitle: String = "No items found",
    emptySubtitle: String? = null,
    emptyIllustration: @Composable (() -> Unit)? = null,
    onSuccess: @Composable (T) -> Unit
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (state) {
            is ScreenState.Loading -> AppLoadingIndicator()
            is ScreenState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(OpenSplitTokens.SpaceXL),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (emptyIllustration != null) {
                            emptyIllustration()
                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceLG))
                        } else {
                            WalletIllustration(size = 130.dp)
                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceLG))
                        }
                        Text(
                            text = if (state.message.isNotBlank()) state.message else emptyTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (!emptySubtitle.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                            Text(
                                text = emptySubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            is ScreenState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(OpenSplitTokens.SpaceXL),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CloudOfflineIllustration(size = 140.dp)
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXL))

                        val isConnectionTimeout = state.message.contains("taking too long", ignoreCase = true) ||
                                state.message.contains("connection", ignoreCase = true)

                        Text(
                            text = if (isConnectionTimeout) "Connection Timeout" else "Something Went Wrong",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXL))
                        Button(
                            onClick = state.retry,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = OpenSplitIcons.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                            Text("Try Again", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            is ScreenState.Success -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                    onSuccess(state.data)
                }
            }
        }
    }
}

