package com.example.domain.model

import com.google.firebase.Timestamp

data class Group(
    val id: String = "",
    val name: String = "",
    val imageUrl: String? = null,
    val createdBy: String = "",
    val memberIds: List<String> = emptyList(),
    val currency: String = "INR",
    val createdAt: Timestamp = Timestamp.now(),
    val simplifyDebts: Boolean = true
)
