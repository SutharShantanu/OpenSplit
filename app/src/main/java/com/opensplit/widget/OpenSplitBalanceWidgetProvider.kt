package com.opensplit.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.opensplit.MainActivity
import com.opensplit.R
import com.opensplit.util.CurrencyFormatter

class OpenSplitBalanceWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_BALANCE = "com.opensplit.widget.ACTION_REFRESH_BALANCE"
        const val EXTRA_WIDGET_ACTION = "com.opensplit.WIDGET_ACTION"
        const val ACTION_ADD_EXPENSE = "add_expense"
        const val ACTION_SETTLE_UP = "settle_up"
        const val ACTION_SCAN_QR = "scan_qr"

        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, OpenSplitBalanceWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                val provider = OpenSplitBalanceWidgetProvider()
                provider.onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val data = WidgetDataHelper.getWidgetData(context)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)

            // Setup Main Root PendingIntent (opens OpenSplit app)
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_balance_root, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_hero_section, openAppPendingIntent)

            // Refresh Button PendingIntent
            val refreshIntent = Intent(context, OpenSplitBalanceWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_BALANCE
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

            // Quick Actions PendingIntents
            val addExpenseIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_WIDGET_ACTION, ACTION_ADD_EXPENSE)
            }
            val addExpensePendingIntent = PendingIntent.getActivity(
                context,
                101,
                addExpenseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_action_add, addExpensePendingIntent)

            val settleUpIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_WIDGET_ACTION, ACTION_SETTLE_UP)
            }
            val settleUpPendingIntent = PendingIntent.getActivity(
                context,
                102,
                settleUpIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_action_settle, settleUpPendingIntent)

            val scanQrIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_WIDGET_ACTION, ACTION_SCAN_QR)
            }
            val scanQrPendingIntent = PendingIntent.getActivity(
                context,
                103,
                scanQrIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_action_qr, scanQrPendingIntent)

            // Populate Content
            if (!data.isLoggedIn) {
                views.setTextViewText(R.id.widget_balance_amount, "Sign In")
                views.setTextViewText(R.id.widget_balance_badge, "AUTH REQUIRED")
                views.setInt(R.id.widget_balance_badge, "setBackgroundResource", R.drawable.widget_badge_settled_bg)
                views.setTextColor(R.id.widget_balance_badge, ContextCompat.getColor(context, R.color.widget_badge_settled_text))
                views.setTextViewText(R.id.widget_balance_subtext, context.getString(R.string.widget_prompt_sign_in))
            } else {
                val currency = data.currencyCode.ifBlank { "INR" }
                val net = data.netBalance

                if (net > 0.01) {
                    val formatted = "+ " + CurrencyFormatter.format(net, currency, showSign = false)
                    views.setTextViewText(R.id.widget_balance_amount, formatted)
                    views.setTextViewText(R.id.widget_balance_badge, context.getString(R.string.widget_status_owed))
                    views.setInt(R.id.widget_balance_badge, "setBackgroundResource", R.drawable.widget_badge_owed_bg)
                    views.setTextColor(R.id.widget_balance_badge, ContextCompat.getColor(context, R.color.widget_badge_owed_text))

                    val sub = buildString {
                        append("Owed: ")
                        append(CurrencyFormatter.format(data.totalOwed, currency))
                        if (data.totalOwe > 0.01) {
                            append("  •  You owe: ")
                            append(CurrencyFormatter.format(data.totalOwe, currency))
                        }
                    }
                    views.setTextViewText(R.id.widget_balance_subtext, sub)
                } else if (net < -0.01) {
                    val formatted = "- " + CurrencyFormatter.format(kotlin.math.abs(net), currency, showSign = false)
                    views.setTextViewText(R.id.widget_balance_amount, formatted)
                    views.setTextViewText(R.id.widget_balance_badge, context.getString(R.string.widget_status_owe))
                    views.setInt(R.id.widget_balance_badge, "setBackgroundResource", R.drawable.widget_badge_owe_bg)
                    views.setTextColor(R.id.widget_balance_badge, ContextCompat.getColor(context, R.color.widget_badge_owe_text))

                    val sub = buildString {
                        append("You owe: ")
                        append(CurrencyFormatter.format(data.totalOwe, currency))
                        if (data.totalOwed > 0.01) {
                            append("  •  Owed: ")
                            append(CurrencyFormatter.format(data.totalOwed, currency))
                        }
                    }
                    views.setTextViewText(R.id.widget_balance_subtext, sub)
                } else {
                    views.setTextViewText(R.id.widget_balance_amount, CurrencyFormatter.format(0.0, currency))
                    views.setTextViewText(R.id.widget_balance_badge, context.getString(R.string.widget_status_settled))
                    views.setInt(R.id.widget_balance_badge, "setBackgroundResource", R.drawable.widget_badge_settled_bg)
                    views.setTextColor(R.id.widget_balance_badge, ContextCompat.getColor(context, R.color.widget_badge_settled_text))
                    views.setTextViewText(R.id.widget_balance_subtext, "All balances are settled up")
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_BALANCE) {
            WidgetDataHelper.refreshData(context)
        }
    }
}
