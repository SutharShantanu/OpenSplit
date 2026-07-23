package com.opensplit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.opensplit.di.AppContainer

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(container.authRepository, container.userRepository) as T
        }
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(container.authRepository, container.groupRepository, container.expenseRepository, container.userRepository, container.friendRepository, container.activityRepository) as T
        }
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(container) as T
        }
        if (modelClass.isAssignableFrom(ActivityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActivityViewModel(container) as T
        }
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalyticsViewModel(container) as T
        }
        if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountViewModel(container.authRepository, container.userRepository, container.groupRepository, container.expenseRepository, container.friendRepository, container.userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SettleUpViewModelFactory(
    private val groupId: String,
    private val suggestedToUid: String?,
    private val suggestedAmount: Double?,
    private val container: AppContainer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettleUpViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettleUpViewModel(
                groupId = groupId,
                suggestedToUid = suggestedToUid,
                suggestedAmount = suggestedAmount,
                authRepository = container.authRepository,
                groupRepository = container.groupRepository,
                userRepository = container.userRepository,
                settlementRepository = container.settlementRepository,
                activityRepository = container.activityRepository
            ) as T
        }
        if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountViewModel(container.authRepository, container.userRepository, container.groupRepository, container.expenseRepository, container.friendRepository, container.userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
class GroupDetailViewModelFactory(
    private val groupId: String,
    private val container: AppContainer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupDetailViewModel(
                groupId = groupId,
                groupRepository = container.groupRepository,
                expenseRepository = container.expenseRepository,
                userRepository = container.userRepository,
                activityRepository = container.activityRepository,
                authRepository = container.authRepository
            ) as T
        }
        if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountViewModel(container.authRepository, container.userRepository, container.groupRepository, container.expenseRepository, container.friendRepository, container.userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
