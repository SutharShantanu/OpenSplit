package com.opensplit.di

import android.content.Context
import com.opensplit.data.repository.AuthRepositoryImpl
import com.opensplit.data.repository.ExpenseRepositoryImpl
import com.opensplit.data.repository.GroupRepositoryImpl
import com.opensplit.data.repository.UserRepositoryImpl
import com.opensplit.domain.repository.AuthRepository
import com.opensplit.domain.repository.ExpenseRepository
import com.opensplit.domain.repository.GroupRepository
import com.opensplit.domain.repository.UserRepository
import com.opensplit.domain.repository.UserPreferencesRepository
import com.opensplit.data.repository.UserPreferencesRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings

/**
 * Manual Dependency Injection Container.
 * (Hilt disabled due to AGP 9.1.1 compatibility in this environment)
 */
class AppContainer(private val applicationContext: Context) {
    val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /**
     * Firestore with offline persistence enabled so the app is usable without a
     * connection and syncs automatically when back online. Settings must be applied
     * before the instance is used for anything else — this lazy block is the single
     * creation point, so it is safe here.
     */
    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = firestoreSettings {
                setLocalCacheSettings(persistentCacheSettings {})
            }
        }
    }
    
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(firebaseAuth)
    }

    val userRepository: UserRepository by lazy {
        UserRepositoryImpl(firestore)
    }

    val activityRepository: com.opensplit.domain.repository.ActivityRepository by lazy {
        com.opensplit.data.repository.ActivityRepositoryImpl(firestore)
    }
    
    val groupRepository: GroupRepository by lazy {
        GroupRepositoryImpl(firestore, activityRepository)
    }

    val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepositoryImpl(firestore, activityRepository)
    }
    val settlementRepository: com.opensplit.domain.repository.SettlementRepository by lazy {
        com.opensplit.data.repository.SettlementRepositoryImpl(firestore, activityRepository)
    }
    val pendingInviteRepository: com.opensplit.domain.repository.PendingInviteRepository by lazy {
        com.opensplit.data.repository.PendingInviteRepositoryImpl(firestore)
    }
    val friendInviteRepository: com.opensplit.domain.repository.FriendInviteRepository by lazy {
        com.opensplit.data.repository.FriendInviteRepositoryImpl(firestore)
    }
    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepositoryImpl(applicationContext)
    }
    val friendRepository: com.opensplit.domain.repository.FriendRepository by lazy {
        com.opensplit.data.repository.FriendRepositoryImpl(groupRepository, expenseRepository, settlementRepository)
    }
}

