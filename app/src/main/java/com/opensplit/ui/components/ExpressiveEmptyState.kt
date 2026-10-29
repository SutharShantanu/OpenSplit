package com.opensplit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.ShapeCache
import com.opensplit.ui.theme.Symbol
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.components.SymbolIcon

/**
 * Material 3 Expressive styled empty state container with a tonal burst/circle container,
 * rounded typography, contextual subtitle, and optional CTA button.
 */
@Composable
fun ExpressiveEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    symbol: Symbol? = null,
    icon: ImageVector? = null,
    iconDrawableRes: Int? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = ShapeCache.expressiveHero,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                tonalElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (symbol != null) {
                        SymbolIcon(
                            symbol = symbol,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    } else if (iconDrawableRes != null) {
                        Icon(
                            painter = painterResource(id = iconDrawableRes),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    } else {
                        SymbolIcon(
                            symbol = OpenSplitIcons.PieChart,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (!actionLabel.isNullOrBlank() && onActionClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                FilledTonalButton(
                    onClick = onActionClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(
                        text = actionLabel,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
