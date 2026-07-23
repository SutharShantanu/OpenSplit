package com.example.di

import android.content.Context
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.ExpenseRepositoryImpl
import com.example.data.repository.GroupRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.ExpenseRepository
import com.example.domain.repository.GroupRepository
import com.example.domain.repository.UserRepository
import com.example.domain.repository.UserPreferencesRepository
import com.example.data.repository.UserPreferencesRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Manual Dependency Injection Container.
 * (Hilt disabled due to AGP 9.1.1 compatibility in this environment)
 */
class AppContainer(private val applicationContext: Context) {
    val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(firebaseAuth)
    }

    val userRepository: UserRepository by lazy {
        UserRepositoryImpl(firestore)
    }

    val activityRepository: com.example.domain.repository.ActivityRepository by lazy {
        com.example.data.repository.ActivityRepositoryImpl(firestore)
    }
    
    val groupRepository: GroupRepository by lazy {
        GroupRepositoryImpl(firestore, activityRepository)
    }

    val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepositoryImpl(firestore, activityRepository)
    }
    val settlementRepository: com.example.domain.repository.SettlementRepository by lazy {
        com.example.data.repository.SettlementRepositoryImpl(firestore, activityRepository)
    }
    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepositoryImpl(applicationContext)
    }
    val friendRepository: com.example.domain.repository.FriendRepository by lazy {
        com.example.data.repository.FriendRepositoryImpl(groupRepository, expenseRepository, settlementRepository)
    }
}

