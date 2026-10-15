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
    val simplifyDebts: Boolean = true,
    /** Members who left or were removed. Their past expenses keep showing their names. */
    val formerMemberIds: List<String> = emptyList(),
    /** Preselected split method on Add Expense for this group. */
    val defaultSplitType: SplitType = SplitType.EQUAL,
    /** Token of the invite link last used to join; lets Firestore rules verify self-joins. */
    val lastJoinToken: String? = null,
    /** Hidden two-person group that backs one-to-one expenses between friends. */
    val isDirect: Boolean = false
)
