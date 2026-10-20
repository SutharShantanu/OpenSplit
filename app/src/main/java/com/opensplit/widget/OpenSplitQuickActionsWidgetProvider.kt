package com.opensplit.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.opensplit.MainActivity
import com.opensplit.R

class OpenSplitQuickActionsWidgetProvider : AppWidgetProvider() {

    companion object {
        const val EXTRA_WIDGET_ACTION = "com.opensplit.WIDGET_ACTION"
        const val ACTION_ADD_EXPENSE = "add_expense"
        const val ACTION_SETTLE_UP = "settle_up"
        const val ACTION_SCAN_QR = "scan_qr"
        const val ACTION_ACTIVITY = "activity"

        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, OpenSplitQuickActionsWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                val provider = OpenSplitQuickActionsWidgetProvider()
                provider.onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_actions)

            // Setup Root Click
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                200,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_actions_root, openAppPendingIntent)

            // 1. Add Expense
            val addExpenseIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_WIDGET_ACTION, ACTION_ADD_EXPENSE)
            }
            val addPendingIntent = PendingIntent.getActivity(
                context,
                201,
                addExpenseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_bar_add, addPendingIntent)

            // 2. Settle Up
            val settleIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_WIDGET_ACTION, ACTION_SETTLE_UP)
            }
            val settlePendingIntent = PendingIntent.getActivity(
                context,
                202,
                settleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_bar_settle, settlePendingIntent)

            // 3. Scan QR
            val qrIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_WIDGET_ACTION, ACTION_SCAN_QR)
            }
            val qrPendingIntent = PendingIntent.getActivity(
                context,
                203,
                qrIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_bar_qr, qrPendingIntent)

            // 4. Activity
            val activityIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_WIDGET_ACTION, ACTION_ACTIVITY)
            }
            val activityPendingIntent = PendingIntent.getActivity(
                context,
                204,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_bar_activity, activityPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
