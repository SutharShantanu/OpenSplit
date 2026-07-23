package com.opensplit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.opensplit.di.AppContainer
import com.opensplit.domain.model.Comment
import com.opensplit.domain.model.Expense
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ExpenseDetailUiState(
    val expense: Expense? = null,
    val comments: List<Comment> = emptyList(),
    val currency: String = "$"
)

class ExpenseDetailViewModel(
    val groupId: String,
    val expenseId: String,
    private val appContainer: AppContainer
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState<ExpenseDetailUiState>>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState<ExpenseDetailUiState>> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            appContainer.expenseRepository.getExpensesForGroup(groupId)
                .combine(appContainer.expenseRepository.getCommentsForExpense(groupId, expenseId)) { expenses, comments ->
                    val expense = expenses.find { it.id == expenseId }
                    if (expense != null) {
                        ScreenState.Success(
                            ExpenseDetailUiState(
                                expense = expense,
                                comments = comments
                            )
                        )
                    } else {
                        ScreenState.Error("Expense not found")
                    }
                }.collect {
                    _uiState.value = it
                }
        }
    }

    fun addComment(text: String) {
        val uid = appContainer.authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            val comment = Comment(
                uid = uid,
                text = text,
                timestamp = Timestamp.now()
            )
            appContainer.expenseRepository.addComment(groupId, expenseId, comment)
        }
    }

    fun deleteExpense(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = appContainer.expenseRepository.deleteExpense(expenseId)
            if (result.isSuccess) {
                onSuccess()
            }
        }
    }
}

class ExpenseDetailViewModelFactory(
    private val groupId: String,
    private val expenseId: String,
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseDetailViewModel(groupId, expenseId, appContainer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
