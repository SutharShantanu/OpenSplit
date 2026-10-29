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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.opensplit.ui.components.responsiveContentWidth
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opensplit.di.AppContainer
import com.opensplit.domain.model.Expense
import com.opensplit.ui.components.LocalSnackbarController
import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.util.CurrencyFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/** Soft-deleted expenses from the last 90 days across the user's groups. */
class RecentlyDeletedViewModel(private val container: AppContainer) : ViewModel() {
    private val uid = container.authRepository.currentUserId ?: ""

    @OptIn(ExperimentalCoroutinesApi::class)
    val deleted: StateFlow<List<Expense>> = container.groupRepository.getGroupsForUser(uid)
        .flatMapLatest { groups -> container.expenseRepository.getDeletedExpensesForGroups(groups.map { it.id }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restore(expense: Expense, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(container.expenseRepository.restoreExpense(expense.groupId, expense.id).isSuccess) }
    }

    fun purge(expense: Expense, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(container.expenseRepository.purgeExpense(expense.groupId, expense.id).isSuccess) }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RecentlyDeletedViewModel(container) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyDeletedScreen(appContainer: AppContainer, onNavigateBack: () -> Unit) {
    val viewModel: RecentlyDeletedViewModel = viewModel(factory = RecentlyDeletedViewModel.Factory(appContainer))
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarController.current
    var pendingPurge by remember { mutableStateOf<Expense?>(null) }
    val dateFmt = SimpleDateFormat("MMM d", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recently deleted", fontWeight = FontWeight.Bold) },
                navigationIcon = { com.opensplit.ui.components.RoundBackButton(onClick = onNavigateBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (deleted.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(OpenSplitTokens.SpaceXL), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SymbolIcon(OpenSplitIcons.Delete, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(OpenSplitTokens.SpaceLG))
                    Text("Nothing deleted recently", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
                    Text("Expenses you delete stay here for 90 days. Restoring one puts it back into everyone's balances.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            return@Scaffold
        }
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
            item {
                Text("Kept for 90 days after deletion. Restoring puts the expense back into everyone's balances.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = OpenSplitTokens.SpaceXS))
            }
            items(deleted, key = { it.id }) { exp ->
                Card(modifier = Modifier.fillMaxWidth(), shape = OpenSplitTokens.BrandCardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(OpenSplitTokens.SpaceLG)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(exp.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Deleted ${dateFmt.format((exp.deletedAt ?: exp.createdAt).toDate())} · ${exp.category}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(CurrencyFormatter.format(exp.amount, exp.currency),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = com.opensplit.ui.theme.MoneyFontFamily), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
                        Row(horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)) {
                            FilledTonalButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = {
                                viewModel.restore(exp) { ok -> snackbar.showMessage(if (ok) "Restored ${exp.description}" else "Couldn't restore") }
                            }) {
                                SymbolIcon(OpenSplitIcons.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp)); Text("Restore")
                            }
                            TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = { pendingPurge = exp }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                Text("Delete forever")
                            }
                        }
                    }
                }
            }
        }
    }
    }

    pendingPurge?.let { exp ->
        AlertDialog(
            onDismissRequest = { pendingPurge = null },
            icon = { SymbolIcon(OpenSplitIcons.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete forever?") },
            text = { Text("\"${exp.description}\" and its comments will be removed permanently. This cannot be undone.") },
            confirmButton = {
                Button(shapes = com.opensplit.ui.theme.appButtonShapes(), 
                    onClick = { viewModel.purge(exp) { ok -> snackbar.showMessage(if (ok) "Deleted forever" else "Couldn't delete") }; pendingPurge = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete forever") }
            },
            dismissButton = { TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = { pendingPurge = null }) { Text("Cancel") } }
        )
    }
}
