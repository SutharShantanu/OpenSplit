package com.opensplit.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import kotlin.math.abs

@Composable
fun getBalanceColor(amount: Double, isSpendTotal: Boolean = false): Color {
    return if (isSpendTotal) {
        MaterialTheme.colorScheme.primary
    } else if (amount > 0.01) {
        OpenSplitTokens.OwedPositive
    } else if (amount < -0.01) {
        OpenSplitTokens.OwedNegative
    } else {
        OpenSplitTokens.OwedNeutral
    }
}

@Composable
fun AnimatedAmountText(
    amount: Double,
    currency: String = "₹",
    isSpendTotal: Boolean = false,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.displayLarge,
    modifier: Modifier = Modifier
) {
    val animatedAmount by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = spring(stiffness = 300f),
        label = "AmountAnimation"
    )

    val color = getBalanceColor(amount, isSpendTotal)
    val symbol = com.opensplit.util.CurrencyFormatter.getCurrencySymbol(currency)
    val formattedNumber = com.opensplit.util.CurrencyFormatter.format(
        abs(animatedAmount.toDouble()),
        currencyCode = currency,
        showSymbol = false
    )

    val sign = if (isSpendTotal) {
        ""
    } else if (amount > 0.01) {
        "+"
    } else if (amount < -0.01) {
        "-"
    } else {
        ""
    }

    val textToDisplay = "$sign$formattedNumber/-"
    var fontSizeMultiplier by androidx.compose.runtime.remember(textToDisplay) { androidx.compose.runtime.mutableStateOf(1.0f) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = color
            ),
            modifier = Modifier.padding(end = 4.dp)
        )

        Text(
            text = textToDisplay,
            style = textStyle.copy(
                fontSize = (textStyle.fontSize.value * fontSizeMultiplier).sp,
                fontWeight = FontWeight.Bold,
                color = color
            ),
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if ((result.hasVisualOverflow || result.didOverflowWidth) && fontSizeMultiplier > 0.5f) {
                    fontSizeMultiplier *= 0.88f
                }
            }
        )
    }
}

@Composable
fun HeroBalanceCard(
    amount: Double,
    currency: String = "₹",
    youAreOwed: Double = 0.0,
    youOwe: Double = 0.0,
    title: String = "TOTAL NET BALANCE",
    subtitle: String? = null,
    isSpendTotal: Boolean = false,
    onOwedToYouClick: (() -> Unit)? = null,
    onYouOweClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (icon, badgeBg, badgeFg, statusLabel) = when {
        isSpendTotal -> Quadruple(
            Icons.AutoMirrored.Rounded.TrendingUp,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Total Expenditure"
        )
        amount > 0.01 -> Quadruple(
            Icons.Rounded.Add,
            OpenSplitTokens.OwedPositive.copy(alpha = 0.15f),
            OpenSplitTokens.OwedPositive,
            "You are owed money"
        )
        amount < -0.01 -> Quadruple(
            Icons.Rounded.Remove,
            OpenSplitTokens.OwedNegative.copy(alpha = 0.15f),
            OpenSplitTokens.OwedNegative,
            "You owe money"
        )
        else -> Quadruple(
            Icons.Rounded.CheckCircle,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "You are all settled up!"
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Header Row with Title and Dynamic Plus/Minus Badge (Responsive layout)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(1.0f, androidx.compose.ui.unit.TextUnitType.Sp),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp)
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = badgeBg
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = badgeFg
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeFg,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Big Net Amount — Justified to End / Right-Aligned
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                AnimatedAmountText(
                    amount = amount,
                    currency = currency,
                    isSpendTotal = isSpendTotal,
                    textStyle = MaterialTheme.typography.displayLarge
                )
            }

            // Owed vs You Owe Breakdown Row (Clean & Minimal)
            if (!isSpendTotal && (youAreOwed > 0.01 || youOwe > 0.01)) {
                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Owed to you (Justified Center)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.small)
                            .clickable(enabled = onOwedToYouClick != null) { onOwedToYouClick?.invoke() }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(OpenSplitTokens.OwedPositive.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = OpenSplitTokens.OwedPositive
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Owed to you",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = com.opensplit.util.CurrencyFormatter.format(youAreOwed, currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = OpenSplitTokens.OwedPositive,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .height(26.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    )

                    // Right Column: You owe (Justified Center)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.small)
                            .clickable(enabled = onYouOweClick != null) { onYouOweClick?.invoke() }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(OpenSplitTokens.OwedNegative.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = OpenSplitTokens.OwedNegative
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "You owe",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = com.opensplit.util.CurrencyFormatter.format(youOwe, currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = OpenSplitTokens.OwedNegative,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
