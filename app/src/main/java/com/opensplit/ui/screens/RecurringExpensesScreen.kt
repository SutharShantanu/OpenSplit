@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opensplit.di.AppContainer
import com.opensplit.domain.logic.RecurrenceScheduler
import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.Group
import com.opensplit.ui.components.LocalSnackbarController
import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.components.responsiveContentWidth
import com.opensplit.ui.components.getCategoryColor
import com.opensplit.ui.components.getCategoryIcon
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.util.CurrencyFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class RecurringItem(val expense: Expense, val group: Group, val setUpByMe: Boolean)

/** Recurring templates across every group the user belongs to. */
class RecurringExpensesViewModel(private val container: AppContainer) : ViewModel() {
    private val uid = container.authRepository.currentUserId ?: ""

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<RecurringItem>> = container.groupRepository.getGroupsForUser(uid)
        .flatMapLatest { groups ->
            if (groups.isEmpty()) flowOf(emptyList()) else combine(
                groups.map { g -> container.expenseRepository.getExpensesForGroup(g.id).let { f -> combine(f, flowOf(g)) { e, gg -> e to gg } } }
            ) { pairs ->
                pairs.flatMap { (expenses, g) ->
                    expenses.filter { it.recurrence != null && it.recurrence.frequency != com.opensplit.domain.model.RecurrenceFrequency.NONE }
                        .map { RecurringItem(it, g, it.createdBy == uid) }
                }.sortedBy { it.expense.recurrence?.nextOccurrence?.seconds ?: 0 }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setPaused(item: RecurringItem, paused: Boolean) {
        val rule = item.expense.recurrence ?: return
        viewModelScope.launch { container.expenseRepository.updateExpense(item.expense.copy(recurrence = rule.copy(paused = paused)), uid) }
    }

    fun stop(item: RecurringItem) {
        viewModelScope.launch { container.expenseRepository.updateExpense(item.expense.copy(recurrence = null), uid) }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RecurringExpensesViewModel(container) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpensesScreen(
    appContainer: AppContainer,
    onNavigateBack: () -> Unit,
    onOpenExpense: (groupId: String, expenseId: String) -> Unit
) {
    val viewModel: RecurringExpensesViewModel = viewModel(factory = RecurringExpensesViewModel.Factory(appContainer))
    val items by viewModel.items.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarController.current
    val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring bills", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    com.opensplit.ui.components.RoundBackButton(onClick = onNavigateBack)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(OpenSplitTokens.SpaceXL), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SymbolIcon(OpenSplitIcons.Autorenew, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(OpenSplitTokens.SpaceLG))
                    Text("No recurring bills yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
                    Text(
                        "Set \"Repeat\" on any expense to weekly, fortnightly, monthly or yearly and it shows up here.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                    )
                }
            }
            return@Scaffold
        }
        val mine = items.filter { it.setUpByMe }; val others = items.filterNot { it.setUpByMe }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .responsiveContentWidth(maxWidth = 760.dp)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(start = OpenSplitTokens.SpaceLG, end = OpenSplitTokens.SpaceLG, top = OpenSplitTokens.SpaceMD, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)
            ) {
                if (mine.isNotEmpty()) item { SectionLabel("Set up by you") }
                items(mine, key = { it.expense.id }) { RecurringRow(it, dateFmt, viewModel, snackbar::showMessage, onOpenExpense, canEdit = true) }
                if (others.isNotEmpty()) item { SectionLabel("Set up by others") }
                items(others, key = { it.expense.id }) { RecurringRow(it, dateFmt, viewModel, snackbar::showMessage, onOpenExpense, canEdit = false) }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = OpenSplitTokens.SpaceXS))
}

@Composable
private fun RecurringRow(
    item: RecurringItem,
    dateFmt: SimpleDateFormat,
    viewModel: RecurringExpensesViewModel,
    toast: (String) -> Unit,
    onOpenExpense: (String, String) -> Unit,
    canEdit: Boolean
) {
    val exp = item.expense; val rule = exp.recurrence ?: return
    Card(
        onClick = { onOpenExpense(exp.groupId, exp.id) },
        modifier = Modifier.fillMaxWidth(),
        shape = OpenSplitTokens.BrandCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(OpenSplitTokens.SpaceLG)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = OpenSplitTokens.PillShape, color = getCategoryColor(exp.category).copy(alpha = 0.15f), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        SymbolIcon(getCategoryIcon(exp.category), contentDescription = null, tint = getCategoryColor(exp.category), modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(OpenSplitTokens.SpaceMD))
                Column(modifier = Modifier.weight(1f)) {
                    Text(exp.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${item.group.name} · ${RecurrenceScheduler.label(rule.frequency)}" + if (rule.paused) " · Paused" else " · Next on ${dateFmt.format(rule.nextOccurrence.toDate())}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    CurrencyFormatter.format(exp.amount, exp.currency),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = com.opensplit.ui.theme.MoneyFontFamily), fontWeight = FontWeight.Bold
                )
            }
            if (canEdit) {
                Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
                Row(horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)) {
                    AssistChip(
                        onClick = { viewModel.setPaused(item, !rule.paused); toast(if (rule.paused) "Resumed ${exp.description}" else "Paused ${exp.description}") },
                        label = { Text(if (rule.paused) "Resume" else "Pause") },
                        leadingIcon = { SymbolIcon(if (rule.paused) OpenSplitIcons.Autorenew else OpenSplitIcons.Pending, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    AssistChip(
                        onClick = { viewModel.stop(item); toast("Stopped repeating ${exp.description}") },
                        label = { Text("Stop repeating") },
                        leadingIcon = { SymbolIcon(OpenSplitIcons.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }
    }
}
