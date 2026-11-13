package com.opensplit.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import com.opensplit.R

/** Home-screen row of shortcuts. The class name is kept so widgets placed by older builds keep working. */
class OpenSplitQuickActionsWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickActionsWidget()

    companion object {
        const val ACTION_ACTIVITY = "activity"
    }
}

class QuickActionsWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val colors = widgetColors(context)
        provideContent {
            GlanceTheme(colors) {
                // Labels need about 80dp per button; narrower widgets show icons only.
                val labels = LocalSize.current.width >= 320.dp
                Row(
                    modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(24.dp).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val gap = GlanceModifier.width(8.dp)
                    ActionPill(R.drawable.ic_widget_add, "Add", OpenSplitBalanceWidgetProvider.ACTION_ADD_EXPENSE, GlanceModifier.defaultWeight().fillMaxHeight(), labels)
                    Spacer(gap)
                    ActionPill(R.drawable.ic_widget_settle, "Settle up", OpenSplitBalanceWidgetProvider.ACTION_SETTLE_UP, GlanceModifier.defaultWeight().fillMaxHeight(), labels)
                    Spacer(gap)
                    ActionPill(R.drawable.ic_widget_qr, "Scan QR", OpenSplitBalanceWidgetProvider.ACTION_SCAN_QR, GlanceModifier.defaultWeight().fillMaxHeight(), labels)
                    Spacer(gap)
                    ActionPill(R.drawable.ic_widget_activity, "Activity", OpenSplitQuickActionsWidgetProvider.ACTION_ACTIVITY, GlanceModifier.defaultWeight().fillMaxHeight(), labels)
                }
            }
        }
    }
}
