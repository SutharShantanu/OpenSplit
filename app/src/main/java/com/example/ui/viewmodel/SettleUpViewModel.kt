package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Activity
import com.example.domain.model.ActivityType
import com.example.domain.model.Settlement
import com.example.domain.model.SettlementMethod
import com.example.domain.model.User
import com.example.domain.repository.ActivityRepository
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.GroupRepository
import com.example.domain.repository.SettlementRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettleUpViewModel(
    private val groupId: String,
    private val suggestedToUid: String?,
    private val suggestedAmount: Double?,
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val settlementRepository: SettlementRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    private val _members = MutableStateFlow<List<User>>(emptyList())
    val members: StateFlow<List<User>> = _members.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _currency = MutableStateFlow<String>("INR")
    val currency: StateFlow<String> = _currency.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId()
            _currentUserId.value = uid
            
            val group = groupRepository.getGroup(groupId)
            if (group != null) {
                _currency.value = group.currency
                val loadedMembers = coroutineScope {
                    group.memberIds.map { memberId ->
                        async { userRepository.getUser(memberId) }
                    }.awaitAll().filterNotNull()
                }
                _members.value = loadedMembers
            }
        }
    }

    fun addSettlement(
        fromUid: String,
        toUid: String,
        amount: Double,
        method: SettlementMethod,
        note: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val settlement = Settlement(
                fromUid = fromUid,
                toUid = toUid,
                amount = amount,
                currency = _currency.value,
                method = method,
                note = note
            )
            val result = settlementRepository.addSettlement(groupId, settlement)
            if (result.isSuccess) {
                activityRepository.logActivity(
                    groupId,
                    Activity(
                        type = ActivityType.SETTLEMENT_ADDED,
                        actorUid = fromUid,
                        message = "recorded a settlement of ${_currency.value} $amount to someone", // ideally we show the names but need user objects
                        relatedExpenseId = result.getOrNull()
                    )
                )
                onSuccess()
            }
        }
    }
}
