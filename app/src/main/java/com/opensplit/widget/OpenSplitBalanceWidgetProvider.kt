package com.opensplit.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.opensplit.MainActivity
import com.opensplit.OpenSplitApp
import com.opensplit.R
import com.opensplit.ui.theme.currentWallpaperColors
import com.opensplit.ui.theme.pickColorScheme
import com.opensplit.util.CurrencyFormatter
import kotlinx.coroutines.flow.first
import kotlin.math.abs

/** Home-screen balance widget. The class name is kept so widgets placed by older builds keep working. */
class OpenSplitBalanceWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceWidget()

    // Placement and every updatePeriodMillis: recompute from the repositories, then redraw.
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetDataHelper.refreshData(context)
    }

    companion object {
        /** Intent extra MainActivity reads to run a widget, shortcut, tile or notification action. */
        const val EXTRA_WIDGET_ACTION = "com.opensplit.WIDGET_ACTION"
        const val ACTION_ADD_EXPENSE = "add_expense"
        const val ACTION_SETTLE_UP = "settle_up"
        const val ACTION_SCAN_QR = "scan_qr"
    }
}

class BalanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val colors = widgetColors(context)
        provideContent {
            val data by WidgetDataHelper.dataFlow(context).collectAsState(WidgetDataHelper.getWidgetData(context))
            GlanceTheme(colors) { BalanceContent(data) }
        }
    }

    companion object {
        val SMALL = DpSize(110.dp, 80.dp)
        val WIDE = DpSize(220.dp, 80.dp)
        val TALL = DpSize(220.dp, 130.dp)
    }
}

class RefreshBalanceAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) =
        WidgetDataHelper.refresh(context)
}

@Composable
private fun BalanceContent(data: WidgetBalanceData) {
    val context = LocalContext.current
    val size = LocalSize.current
    val c = GlanceTheme.colors
    val net = data.netBalance
    val currency = data.currencyCode.ifBlank { "INR" }
    val (status, amount, amountColor) = when {
        !data.isLoggedIn -> Triple("Sign in to see balances", "OpenSplit", c.onSurface)
        net > 0.01 -> Triple("You are owed", "+" + CurrencyFormatter.format(net, currency), c.primary)
        net < -0.01 -> Triple("You owe", "−" + CurrencyFormatter.format(abs(net), currency), c.error)
        else -> Triple("All settled up", CurrencyFormatter.format(0.0, currency), c.onSurface)
    }
    val detail = when {
        !data.isLoggedIn -> "Tap to open OpenSplit"
        data.totalOwed > 0.01 && data.totalOwe > 0.01 ->
            "Owed ${CurrencyFormatter.format(data.totalOwed, currency)} · You owe ${CurrencyFormatter.format(data.totalOwe, currency)}"
        else -> "Across your $currency groups"
    }

    // A 2-row widget is only ~140dp tall: with the buttons showing, the detail line only fits from 180dp.
    val showActions = data.isLoggedIn && size.height >= BalanceWidget.TALL.height
    val showDetail = size.width >= BalanceWidget.WIDE.width && (!showActions || size.height >= 180.dp)
    Column(
        modifier = GlanceModifier.fillMaxSize().background(c.widgetBackground).cornerRadius(24.dp)
            .padding(if (showActions) 12.dp else 16.dp).clickable(actionStartActivity(widgetIntent(context, null)))
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                ImageProvider(R.drawable.ic_notification), contentDescription = null,
                colorFilter = ColorFilter.tint(c.primary), modifier = GlanceModifier.size(18.dp)
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                status, maxLines = 1, modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = c.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            )
            if (data.isLoggedIn) {
                Image(
                    ImageProvider(R.drawable.ic_widget_refresh), contentDescription = "Refresh balance",
                    colorFilter = ColorFilter.tint(c.onSurfaceVariant),
                    modifier = GlanceModifier.size(32.dp).padding(6.dp).cornerRadius(16.dp)
                        .clickable(actionRunCallback<RefreshBalanceAction>())
                )
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        Text(
            amount, maxLines = 1,
            style = TextStyle(
                color = amountColor, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                fontSize = when {
                    size.width < BalanceWidget.WIDE.width -> 22.sp
                    showActions -> 26.sp
                    else -> 30.sp
                }
            )
        )
        if (showDetail) {
            Text(detail, maxLines = 2, style = TextStyle(color = c.onSurfaceVariant, fontSize = 12.sp))
        }
        if (showActions) {
            Spacer(GlanceModifier.height(6.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                ActionPill(R.drawable.ic_widget_add, "Add", OpenSplitBalanceWidgetProvider.ACTION_ADD_EXPENSE, GlanceModifier.defaultWeight().height(44.dp))
                Spacer(GlanceModifier.width(8.dp))
                ActionPill(R.drawable.ic_widget_settle, "Settle", OpenSplitBalanceWidgetProvider.ACTION_SETTLE_UP, GlanceModifier.defaultWeight().height(44.dp))
                Spacer(GlanceModifier.width(8.dp))
                ActionPill(R.drawable.ic_widget_qr, "QR", OpenSplitBalanceWidgetProvider.ACTION_SCAN_QR, GlanceModifier.defaultWeight().height(44.dp))
            }
        }
    }
}

/** A tonal pill that opens the app with [action]; the caller sets its size. Without [showLabel] it is icon-only but still labelled for TalkBack. */
@Composable
internal fun ActionPill(icon: Int, label: String, action: String, modifier: GlanceModifier, showLabel: Boolean = true) {
    val c = GlanceTheme.colors
    Row(
        modifier = modifier.background(c.secondaryContainer).cornerRadius(24.dp)
            .clickable(actionStartActivity(widgetIntent(LocalContext.current, action)))
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            ImageProvider(icon), contentDescription = null,
            colorFilter = ColorFilter.tint(c.onSecondaryContainer), modifier = GlanceModifier.size(20.dp)
        )
        if (showLabel) {
            Spacer(GlanceModifier.width(6.dp))
            Text(label, maxLines = 1, style = TextStyle(color = c.onSecondaryContainer, fontSize = 13.sp, fontWeight = FontWeight.Medium))
        }
    }
}

internal fun widgetIntent(context: Context, action: String?) = Intent(context, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    if (action != null) putExtra(OpenSplitBalanceWidgetProvider.EXTRA_WIDGET_ACTION, action)
}

/** The app's own colour scheme (same "Wallpaper colours" switch and fallbacks), light and dark. */
internal suspend fun widgetColors(context: Context): androidx.glance.color.ColorProviders {
    val dynamic = (context.applicationContext as? OpenSplitApp)?.container?.userPreferencesRepository?.dynamicColorFlow?.first() ?: true
    val wallpaper = currentWallpaperColors(context)
    return ColorProviders(
        light = pickColorScheme(context, false, dynamic, wallpaper),
        dark = pickColorScheme(context, true, dynamic, wallpaper)
    )
}
