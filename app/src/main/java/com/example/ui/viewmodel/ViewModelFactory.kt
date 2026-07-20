package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.di.AppContainer

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(container.authRepository, container.userRepository) as T
        }
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(container.authRepository, container.groupRepository, container.expenseRepository, container.userRepository) as T
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
                userRepository = container.userRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
