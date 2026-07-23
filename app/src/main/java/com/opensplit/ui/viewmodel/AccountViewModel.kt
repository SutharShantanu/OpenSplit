package com.opensplit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opensplit.domain.repository.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AccountUiState(
    val user: FirebaseUser,
    val groupCount: Int,
    val friendCount: Int,
    /** Net balance per currency; never summed across currencies. */
    val netByCurrency: Map<String, Double>,
    val defaultCurrency: String,
    val allExpenses: List<com.opensplit.domain.model.Expense>,
    val pendingInvites: List<String>
)

class AccountViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val friendRepository: FriendRepository,
    val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)

    val themeFlow = userPreferencesRepository.themeFlow
    val notificationsEnabledFlow = userPreferencesRepository.notificationsEnabledFlow

    fun setTheme(theme: String) {
        viewModelScope.launch {
            userPreferencesRepository.setTheme(theme)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationsEnabled(enabled)
        }
    }

    val uiState: StateFlow<ScreenState<AccountUiState>> = retryTrigger.flatMapLatest {
        val currentUser = authRepository.currentUser
        if (currentUser == null) {
            flowOf(ScreenState.Error("User not logged in") {})
        } else {
            combine(
                groupRepository.getGroupsForUser(currentUser.uid),
                friendRepository.getFriendsBalances(currentUser.uid),
                expenseRepository.getExpensesForUser(currentUser.uid),
                flow {
                    val user = userRepository.getUser(currentUser.uid)
                    emit(user?.defaultCurrency ?: "INR")
                }
            ) { groups, friendsBalances, expenses, currency ->
                val groupCount = groups.size
                // A friend counts if they have any non-zero balance in any currency.
                val friendCount = friendsBalances.count { (_, byCurrency) ->
                    byCurrency.values.any { Math.abs(it) > 0.01 }
                }
                // Aggregate per currency, keeping currencies separate.
                val netByCurrency = mutableMapOf<String, Double>()
                for ((_, byCurrency) in friendsBalances) {
                    for ((c, amt) in byCurrency) {
                        netByCurrency[c] = (netByCurrency[c] ?: 0.0) + amt
                    }
                }
                val pendingInvites = emptyList<String>()
                val allExpenses = expenses

                ScreenState.Success(
                    AccountUiState(
                        user = currentUser,
                        groupCount = groupCount,
                        friendCount = friendCount,
                        netByCurrency = netByCurrency,
                        defaultCurrency = currency,
                        allExpenses = allExpenses,
                        pendingInvites = pendingInvites
                    )
                ) as ScreenState<AccountUiState>
            }.catch { e ->
                emit(ScreenState.Error(e.message ?: "Unknown error") { retryTrigger.value++ })
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScreenState.Loading)

    fun updateDisplayName(newName: String) {
        viewModelScope.launch {
            authRepository.updateProfile(newName)
            val currentUser = authRepository.currentUser
            if (currentUser != null) {
                userRepository.updateUser(com.opensplit.domain.model.User(currentUser.uid, newName, currentUser.email ?: ""))
            }
            retryTrigger.value++
        }
    }
    
    fun updateDefaultCurrency(newCurrency: String) {
        viewModelScope.launch {
            userRepository.updateCurrency(newCurrency)
            retryTrigger.value++
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
    
    fun deleteAccount() {
        viewModelScope.launch {
            authRepository.deleteAccount()
            authRepository.signOut()
        }
    }
    
    fun reauthenticate(password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.reauthenticateWithEmail(password)
            onResult(result.isSuccess)
        }
    }
}
