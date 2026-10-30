package com.opensplit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opensplit.domain.repository.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AccountUiState(
    val user: com.opensplit.domain.model.User,
    val groupCount: Int,
    val friendCount: Int,
    /** Net balance per currency; never summed across currencies. */
    val netByCurrency: Map<String, Double>,
    val defaultCurrency: String,
    val allExpenses: List<com.opensplit.domain.model.Expense>,
    val pendingInvites: List<String>,
    val authProvider: String = "Google"
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
    fun retry() { retryTrigger.value++ }

    val themeFlow = userPreferencesRepository.themeFlow
    val notificationsEnabledFlow = userPreferencesRepository.notificationsEnabledFlow
    val openAiApiKeyFlow = userPreferencesRepository.openAiApiKeyFlow
    val geminiApiKeyFlow = userPreferencesRepository.geminiApiKeyFlow
    val aiProviderFlow = userPreferencesRepository.aiProviderFlow
    val dynamicColorFlow = userPreferencesRepository.dynamicColorFlow

    fun setTheme(theme: String) {
        viewModelScope.launch {
            userPreferencesRepository.setTheme(theme)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDynamicColor(enabled)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationsEnabled(enabled)
        }
    }

    fun setOpenAiApiKey(key: String) {
        viewModelScope.launch {
            userPreferencesRepository.setOpenAiApiKey(key)
        }
    }

    fun setGeminiApiKey(key: String) {
        viewModelScope.launch {
            userPreferencesRepository.setGeminiApiKey(key)
        }
    }

    val providerApiKeysFlow = userPreferencesRepository.providerApiKeysFlow
    val customEndpointFlow = userPreferencesRepository.customEndpointFlow
    val customModelFlow = userPreferencesRepository.customModelFlow
    val aiEnabledFlow = userPreferencesRepository.aiEnabledFlow
    val aiDisabledFeaturesFlow = userPreferencesRepository.aiDisabledFeaturesFlow
    val providerModelsFlow = userPreferencesRepository.providerModelsFlow
    val aiUsageFlow = userPreferencesRepository.aiUsageFlow
    val aiTokensFlow = userPreferencesRepository.aiTokensFlow

    fun setAiEnabled(enabled: Boolean) { viewModelScope.launch { userPreferencesRepository.setAiEnabled(enabled) } }
    fun setAiFeatureEnabled(featureId: String, enabled: Boolean) { viewModelScope.launch { userPreferencesRepository.setAiFeatureEnabled(featureId, enabled) } }
    fun setProviderModel(providerId: String, model: String) { viewModelScope.launch { userPreferencesRepository.setProviderModel(providerId, model) } }
    fun testAiKey(providerId: String, key: String, endpoint: String?, model: String?, onResult: (Result<com.opensplit.data.ai.AiService.AiTestResult>) -> Unit) {
        viewModelScope.launch {
            val result = com.opensplit.data.ai.AiService(userPreferencesRepository).testKey(providerId, key, endpoint, model)
            onResult(result)
        }
    }

    fun listAiModels(providerId: String, key: String, endpoint: String?, onResult: (Result<List<String>>) -> Unit) {
        viewModelScope.launch {
            onResult(com.opensplit.data.ai.AiService(userPreferencesRepository).listModels(providerId, key, endpoint))
        }
    }

    fun setAiProvider(provider: String) {
        viewModelScope.launch {
            userPreferencesRepository.setAiProvider(provider)
        }
    }

    fun setProviderApiKey(providerId: String, key: String) {
        viewModelScope.launch {
            userPreferencesRepository.setProviderApiKey(providerId, key)
        }
    }

    fun setCustomEndpoint(endpoint: String) {
        viewModelScope.launch {
            userPreferencesRepository.setCustomEndpoint(endpoint)
        }
    }

    fun setCustomModel(model: String) {
        viewModelScope.launch {
            userPreferencesRepository.setCustomModel(model)
        }
    }

    val uiState: StateFlow<ScreenState<AccountUiState>> = retryTrigger.flatMapLatest {
        val currentUid = authRepository.currentUserId
        val currentUser = authRepository.currentUser
        if (currentUid == null) {
            flowOf(ScreenState.Error("User not logged in") {})
        } else {
            val userFlow = flow {
                val dbUser = userRepository.getUser(currentUid)
                val user = if (dbUser != null) {
                    if (dbUser.photoUrl.isNullOrBlank() && currentUser?.photoUrl != null) {
                        dbUser.copy(photoUrl = currentUser.photoUrl.toString())
                    } else {
                        dbUser
                    }
                } else {
                    com.opensplit.domain.model.User(
                        uid = currentUid,
                        displayName = currentUser?.displayName ?: "User",
                        email = currentUser?.email ?: "",
                        photoUrl = currentUser?.photoUrl?.toString()
                    )
                }
                emit(user)
            }
            val groupsFlow = groupRepository.getGroupsForUser(currentUid)
            val friendsBalancesFlow = friendRepository.getFriendsBalances(currentUid)

            groupsFlow.flatMapLatest { groups ->
                val groupIds = groups.map { it.id }
                val expenseFlows = groupIds.map { expenseRepository.getExpensesForGroup(it) } +
                    expenseRepository.getExpensesForUser(currentUid)
                val allExpensesFlow: Flow<List<com.opensplit.domain.model.Expense>> = if (expenseFlows.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(expenseFlows) { arrays -> arrays.flatMap { it }.distinctBy { it.id } }
                }

                combine(friendsBalancesFlow, allExpensesFlow, userFlow) { friendsBalances, allExpenses, user ->
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

                    val detectedProvider = when {
                        currentUser?.providerData?.any { it.providerId == "google.com" } == true -> "Google"
                        currentUser?.providerData?.any { it.providerId == "password" } == true -> "Email"
                        currentUser?.isAnonymous == true -> "Guest"
                        else -> "Google"
                    }

                    ScreenState.Success(
                        AccountUiState(
                            user = user,
                            groupCount = groupCount,
                            friendCount = friendCount,
                            netByCurrency = netByCurrency,
                            defaultCurrency = user.defaultCurrency.ifBlank { "INR" },
                            allExpenses = allExpenses,
                            pendingInvites = pendingInvites,
                            authProvider = detectedProvider
                        )
                    ) as ScreenState<AccountUiState>
                }
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
    
    fun updateUpiId(upiId: String?) {
        viewModelScope.launch {
            userRepository.updateUpiId(upiId)
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

    fun reauthenticateWithGoogle(idToken: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.reauthenticateWithGoogle(idToken)
            onResult(result.isSuccess)
        }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(authRepository.sendPasswordResetEmail(email))
        }
    }
}
