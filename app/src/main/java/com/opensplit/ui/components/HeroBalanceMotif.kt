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
import com.opensplit.ui.theme.MoneyFontFamily
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

    val textToDisplay = "$sign$formattedNumber"
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
                fontFamily = MoneyFontFamily,
                color = color
            ),
            modifier = Modifier.padding(end = 4.dp)
        )

        Text(
            text = textToDisplay,
            style = textStyle.copy(
                fontSize = (textStyle.fontSize.value * fontSizeMultiplier).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MoneyFontFamily,
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
    currency: String = "$",
    youAreOwed: Double = 0.0,
    youOwe: Double = 0.0,
    title: String? = null,
    subtitle: String? = null,
    isSpendTotal: Boolean = false,
    onOwedToYouClick: (() -> Unit)? = null,
    onYouOweClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardTitle = title ?: when {
        isSpendTotal -> "TOTAL EXPENDITURE"
        amount > 0.01 -> "YOU ARE OWED IN TOTAL"
        amount < -0.01 -> "YOU OWE IN TOTAL"
        else -> "ALL SETTLED UP"
    }

    val displayAmount = if (isSpendTotal) amount else abs(amount)
    val symbol = com.opensplit.util.CurrencyFormatter.getCurrencySymbol(currency)
    val formattedNumber = com.opensplit.util.CurrencyFormatter.format(displayAmount, currencyCode = currency, showSymbol = false)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF482D7B)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // Label
            Text(
                text = cardTitle.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = Color(0xFFC8B6FF)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Large Hero Amount
            Text(
                text = "$symbol$formattedNumber",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MoneyFontFamily,
                    color = Color(0xFFEDE0FD)
                )
            )

            if (!isSpendTotal && (youAreOwed > 0.01 || youOwe > 0.01)) {
                Spacer(modifier = Modifier.height(18.dp))

                // Row 1: You are owed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = onOwedToYouClick != null) { onOwedToYouClick?.invoke() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFC8B6FF)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "You are owed",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFC8B6FF),
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                    Text(
                        text = com.opensplit.util.CurrencyFormatter.format(youAreOwed, currency),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = MoneyFontFamily,
                            color = Color(0xFFEDE0FD)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 2: You owe
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = onYouOweClick != null) { onYouOweClick?.invoke() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFC8B6FF)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "You owe",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFC8B6FF),
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                    Text(
                        text = com.opensplit.util.CurrencyFormatter.format(youOwe, currency),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = MoneyFontFamily,
                            color = Color(0xFFFFD8CC)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Ratio Progress Bar
                val totalSum = (youAreOwed + youOwe).toFloat()
                val owedRatio = if (totalSum > 0.001f) (youAreOwed.toFloat() / totalSum).coerceIn(0.04f, 0.96f) else 0.5f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD8CC))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = owedRatio)
                            .clip(CircleShape)
                            .background(Color(0xFFA580FF))
                    )
                }
            } else if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFC8B6FF))
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
