package com.opensplit.domain.model

import com.google.firebase.Timestamp

/**
 * A shareable group join link. Lives at top-level `inviteLinks/{token}` so a non-member can
 * read it before joining; Firestore rules check it when the joiner adds themselves.
 */
data class InviteLink(
    val token: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    /** Null = never expires. */
    val expiresAt: Timestamp? = null,
    /** 0 = unlimited. */
    val maxUses: Int = 0,
    val uses: Int = 0,
    val revoked: Boolean = false
) {
    val url: String get() = "$BASE_URL$token"

    companion object {
        const val BASE_URL = "https://opensplit-fe83f.firebaseapp.com/join/"
        const val FRIEND_BASE_URL = "https://opensplit-fe83f.firebaseapp.com/friend/"

        /** "join" or "friend" for a scanned/pasted URL, null for anything else. */
        fun kindOf(text: String): String? = when {
            text.contains("/join/") -> "join"
            text.contains("/friend/") -> "friend"
            else -> null
        }

        /** Accepts a full URL, a path, or a bare token; null when nothing token-like is there. */
        fun tokenFrom(text: String): String? {
            val t = text.trim()
            if (t.isEmpty()) return null
            val afterJoin = t.substringAfter("/join/", missingDelimiterValue = t.substringAfter("/friend/", missingDelimiterValue = t))
            val token = afterJoin.trim().trimEnd('/').substringBefore('?').substringBefore('#')
            return token.takeIf { it.length in 8..64 && it.all { c -> c.isLetterOrDigit() || c == '-' || c == '_' } }
        }
    }
}

enum class JoinError(val message: String) {
    NOT_FOUND("This invite link is invalid or was revoked."),
    EXPIRED("This invite link has expired. Ask for a fresh one."),
    USED_UP("This invite link has already been used."),
    ALREADY_MEMBER("You're already in this group.")
}

/** Pure validity check shared by the join screen and the repository. */
fun InviteLink.validate(nowSeconds: Long, joinerUid: String, memberIds: List<String>): JoinError? = when {
    revoked -> JoinError.NOT_FOUND
    expiresAt != null && expiresAt.seconds <= nowSeconds -> JoinError.EXPIRED
    maxUses > 0 && uses >= maxUses -> JoinError.USED_UP
    joinerUid in memberIds -> JoinError.ALREADY_MEMBER
    else -> null
}
