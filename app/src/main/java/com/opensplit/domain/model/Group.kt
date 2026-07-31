package com.opensplit.domain.model

import com.google.firebase.Timestamp

data class Group(
    val id: String = "",
    val name: String = "",
    val imageUrl: String? = null,
    /** Key into GroupAvatarPresets (ui/components/GroupAvatar.kt); null falls back to the name's first letter. */
    val avatarKey: String? = null,
    val createdBy: String = "",
    val memberIds: List<String> = emptyList(),
    val currency: String = "INR",
    val createdAt: Timestamp = Timestamp.now(),
    val simplifyDebts: Boolean = true
)
