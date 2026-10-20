package com.opensplit.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import com.opensplit.OpenSplitApp
import com.opensplit.domain.logic.BalanceCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WidgetBalanceData(
    val isLoggedIn: Boolean = false,
    val userDisplayName: String = "User",
    val currencyCode: String = "INR",
    val netBalance: Double = 0.0,
    val totalOwed: Double = 0.0,
    val totalOwe: Double = 0.0,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

object WidgetDataHelper {

    private const val PREFS_NAME = "opensplit_widget_prefs"
    private const val KEY_LOGGED_IN = "key_logged_in"
    private const val KEY_DISPLAY_NAME = "key_display_name"
    private const val KEY_CURRENCY = "key_currency"
    private const val KEY_NET_BALANCE = "key_net_balance"
    private const val KEY_TOTAL_OWED = "key_total_owed"
    private const val KEY_TOTAL_OWE = "key_total_owe"
    private const val KEY_LAST_UPDATED = "key_last_updated"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getWidgetData(context: Context): WidgetBalanceData {
        val prefs = getPrefs(context)
        return WidgetBalanceData(
            isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false),
            userDisplayName = prefs.getString(KEY_DISPLAY_NAME, "User") ?: "User",
            currencyCode = prefs.getString(KEY_CURRENCY, "INR") ?: "INR",
            netBalance = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_NET_BALANCE, java.lang.Double.doubleToLongBits(0.0))),
            totalOwed = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_TOTAL_OWED, java.lang.Double.doubleToLongBits(0.0))),
            totalOwe = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_TOTAL_OWE, java.lang.Double.doubleToLongBits(0.0))),
            lastUpdatedMillis = prefs.getLong(KEY_LAST_UPDATED, System.currentTimeMillis())
        )
    }

    fun saveWidgetData(context: Context, data: WidgetBalanceData) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, data.isLoggedIn)
            .putString(KEY_DISPLAY_NAME, data.userDisplayName)
            .putString(KEY_CURRENCY, data.currencyCode)
            .putLong(KEY_NET_BALANCE, java.lang.Double.doubleToRawLongBits(data.netBalance))
            .putLong(KEY_TOTAL_OWED, java.lang.Double.doubleToRawLongBits(data.totalOwed))
            .putLong(KEY_TOTAL_OWE, java.lang.Double.doubleToRawLongBits(data.totalOwe))
            .putLong(KEY_LAST_UPDATED, data.lastUpdatedMillis)
            .apply()
    }

    /**
     * Quickly syncs widget data when home screen calculates balances.
     */
    fun updateFromDashboard(
        context: Context,
        isLoggedIn: Boolean,
        displayName: String,
        currencyCode: String,
        netBalance: Double,
        totalOwed: Double,
        totalOwe: Double
    ) {
        val data = WidgetBalanceData(
            isLoggedIn = isLoggedIn,
            userDisplayName = displayName,
            currencyCode = currencyCode,
            netBalance = netBalance,
            totalOwed = totalOwed,
            totalOwe = totalOwe,
            lastUpdatedMillis = System.currentTimeMillis()
        )
        saveWidgetData(context, data)
        notifyAllWidgets(context)
    }

    /**
     * Refreshes balances from Firestore / Local repositories and updates all widgets.
     */
    fun refreshData(context: Context, onComplete: (() -> Unit)? = null) {
        val app = context.applicationContext as? OpenSplitApp ?: return
        val appContainer = app.container

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUid = appContainer.authRepository.getCurrentUserId()
                if (currentUid == null) {
                    val loggedOutData = WidgetBalanceData(isLoggedIn = false)
                    saveWidgetData(context, loggedOutData)
                } else {
                    val user = appContainer.userRepository.getUser(currentUid)
                    val defaultCurrency = user?.defaultCurrency?.ifBlank { null } ?: "INR"
                    val displayName = user?.displayName?.ifBlank { null } ?: "User"

                    val groups = appContainer.groupRepository.getGroupsForUser(currentUid).firstOrNull() ?: emptyList()

                    var netTotal = 0.0
                    var owedTotal = 0.0
                    var oweTotal = 0.0

                    for (group in groups) {
                        val groupCurrency = group.currency.ifBlank { defaultCurrency }
                        if (groupCurrency.equals(defaultCurrency, ignoreCase = true)) {
                            val expenses = appContainer.expenseRepository.getExpensesForGroup(group.id).firstOrNull() ?: emptyList()
                            val settlements = appContainer.settlementRepository.getSettlementsForGroup(group.id).firstOrNull() ?: emptyList()
                            val liveExpenses = expenses.filter { !it.isDeleted }

                            val balances = BalanceCalculator.netBalances(liveExpenses, settlements)
                            val userBal = balances[currentUid] ?: 0.0

                            netTotal += userBal
                            if (userBal > 0.01) {
                                owedTotal += userBal
                            } else if (userBal < -0.01) {
                                oweTotal += kotlin.math.abs(userBal)
                            }
                        }
                    }

                    val data = WidgetBalanceData(
                        isLoggedIn = true,
                        userDisplayName = displayName,
                        currencyCode = defaultCurrency,
                        netBalance = netTotal,
                        totalOwed = owedTotal,
                        totalOwe = oweTotal,
                        lastUpdatedMillis = System.currentTimeMillis()
                    )
                    saveWidgetData(context, data)
                }
            } catch (e: Exception) {
                // Keep existing cached data on network error
            } finally {
                withContext(Dispatchers.Main) {
                    notifyAllWidgets(context)
                    onComplete?.invoke()
                }
            }
        }
    }

    fun notifyAllWidgets(context: Context) {
        OpenSplitBalanceWidgetProvider.updateAll(context)
        OpenSplitQuickActionsWidgetProvider.updateAll(context)
    }
}
