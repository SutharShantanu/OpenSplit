package com.opensplit.widget

import android.content.Context
import android.content.SharedPreferences
import androidx.glance.appwidget.updateAll
import com.opensplit.OpenSplitApp
import com.opensplit.domain.logic.BalanceCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class WidgetBalanceData(
    val isLoggedIn: Boolean = false,
    val userDisplayName: String = "User",
    val currencyCode: String = "INR",
    val netBalance: Double = 0.0,
    val totalOwed: Double = 0.0,
    val totalOwe: Double = 0.0,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

/**
 * The balance widget's data, cached in SharedPreferences so the widget renders without
 * touching Firestore. The dashboard writes it whenever it computes balances; [refresh]
 * recomputes it from the repositories (widget refresh button, periodic update, push).
 */
object WidgetDataHelper {

    private const val PREFS_NAME = "opensplit_widget_prefs"
    private const val KEY_LOGGED_IN = "key_logged_in"
    private const val KEY_DISPLAY_NAME = "key_display_name"
    private const val KEY_CURRENCY = "key_currency"
    private const val KEY_NET_BALANCE = "key_net_balance"
    private const val KEY_TOTAL_OWED = "key_total_owed"
    private const val KEY_TOTAL_OWE = "key_total_owe"
    private const val KEY_LAST_UPDATED = "key_last_updated"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun SharedPreferences.getDouble(key: String) =
        java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToLongBits(0.0)))

    fun getWidgetData(context: Context): WidgetBalanceData {
        val prefs = getPrefs(context)
        return WidgetBalanceData(
            isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false),
            userDisplayName = prefs.getString(KEY_DISPLAY_NAME, "User") ?: "User",
            currencyCode = prefs.getString(KEY_CURRENCY, "INR") ?: "INR",
            netBalance = prefs.getDouble(KEY_NET_BALANCE),
            totalOwed = prefs.getDouble(KEY_TOTAL_OWED),
            totalOwe = prefs.getDouble(KEY_TOTAL_OWE),
            lastUpdatedMillis = prefs.getLong(KEY_LAST_UPDATED, System.currentTimeMillis())
        )
    }

    /** Emits the cached data now and again on every save, so a running widget session redraws. */
    fun dataFlow(context: Context): Flow<WidgetBalanceData> = callbackFlow {
        val prefs = getPrefs(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(getWidgetData(context)) }
        trySend(getWidgetData(context))
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun saveWidgetData(context: Context, data: WidgetBalanceData) {
        getPrefs(context).edit()
            .putBoolean(KEY_LOGGED_IN, data.isLoggedIn)
            .putString(KEY_DISPLAY_NAME, data.userDisplayName)
            .putString(KEY_CURRENCY, data.currencyCode)
            .putLong(KEY_NET_BALANCE, java.lang.Double.doubleToRawLongBits(data.netBalance))
            .putLong(KEY_TOTAL_OWED, java.lang.Double.doubleToRawLongBits(data.totalOwed))
            .putLong(KEY_TOTAL_OWE, java.lang.Double.doubleToRawLongBits(data.totalOwe))
            .putLong(KEY_LAST_UPDATED, data.lastUpdatedMillis)
            .apply()
    }

    /** Called by the dashboard after it computes balances. */
    fun updateFromDashboard(
        context: Context,
        isLoggedIn: Boolean,
        displayName: String,
        currencyCode: String,
        netBalance: Double,
        totalOwed: Double,
        totalOwe: Double
    ) {
        saveWidgetData(
            context,
            WidgetBalanceData(isLoggedIn, displayName, currencyCode, netBalance, totalOwed, totalOwe, System.currentTimeMillis())
        )
        notifyAllWidgets(context)
    }

    /** Recomputes balances from the repositories (Firestore cache or network) and redraws the widgets. */
    suspend fun refresh(context: Context) {
        val container = (context.applicationContext as? OpenSplitApp)?.container ?: return
        try {
            val uid = container.authRepository.getCurrentUserId()
            if (uid == null) {
                saveWidgetData(context, WidgetBalanceData(isLoggedIn = false))
            } else {
                val user = container.userRepository.getUser(uid)
                val defaultCurrency = user?.defaultCurrency?.ifBlank { null } ?: "INR"
                val groups = container.groupRepository.getGroupsForUser(uid).firstOrNull().orEmpty()
                var net = 0.0
                var owed = 0.0
                var owe = 0.0
                // ponytail: only groups in the default currency count; a multi-currency widget needs per-currency rows.
                for (group in groups) {
                    if (!group.currency.ifBlank { defaultCurrency }.equals(defaultCurrency, ignoreCase = true)) continue
                    val expenses = container.expenseRepository.getExpensesForGroup(group.id).firstOrNull().orEmpty()
                    val settlements = container.settlementRepository.getSettlementsForGroup(group.id).firstOrNull().orEmpty()
                    val bal = BalanceCalculator.netBalances(expenses.filter { !it.isDeleted }, settlements)[uid] ?: 0.0
                    net += bal
                    if (bal > 0.01) owed += bal else if (bal < -0.01) owe -= bal
                }
                saveWidgetData(
                    context,
                    WidgetBalanceData(true, user?.displayName?.ifBlank { null } ?: "User", defaultCurrency, net, owed, owe, System.currentTimeMillis())
                )
            }
        } catch (e: Exception) {
            // Keep the cached numbers when offline or signed out mid-refresh.
        }
        BalanceWidget().updateAll(context)
    }

    /** Fire-and-forget [refresh] for callers that are not coroutines (receivers, services). */
    fun refreshData(context: Context) {
        CoroutineScope(Dispatchers.IO).launch { refresh(context) }
    }

    fun notifyAllWidgets(context: Context) {
        CoroutineScope(Dispatchers.Default).launch { BalanceWidget().updateAll(context) }
    }
}
