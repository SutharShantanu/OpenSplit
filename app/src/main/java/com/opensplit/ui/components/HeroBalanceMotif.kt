package com.opensplit.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    currency: String = "$",
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
    val formattedNumber = String.format("%.2f", abs(animatedAmount))

    val prefix = if (isSpendTotal) {
        ""
    } else if (amount > 0.01) {
        "+"
    } else if (amount < -0.01) {
        "-"
    } else {
        ""
    }

    Text(
        text = "$prefix$currency$formattedNumber",
        style = textStyle.copy(fontWeight = FontWeight.Bold, color = color),
        modifier = modifier
    )
}

@Composable
fun HeroBalanceCard(
    amount: Double,
    currency: String = "$",
    title: String = "Total Net Balance",
    subtitle: String? = null,
    isSpendTotal: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            AnimatedAmountText(
                amount = amount,
                currency = currency,
                isSpendTotal = isSpendTotal,
                textStyle = MaterialTheme.typography.displayLarge
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val statusText = if (isSpendTotal) {
                    "Total monthly expenditure"
                } else if (amount > 0.01) {
                    "Overall, you are owed money"
                } else if (amount < -0.01) {
                    "Overall, you owe money"
                } else {
                    "You are all settled up!"
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = getBalanceColor(amount, isSpendTotal)
                )
            }
        }
    }
}

