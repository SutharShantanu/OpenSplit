package com.opensplit.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val themeFlow: Flow<String>
    suspend fun setTheme(theme: String)

    val hasCompletedPermissionPrimer: Flow<Boolean>
    suspend fun setHasCompletedPermissionPrimer(completed: Boolean)

    val notificationsEnabledFlow: Flow<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean)

    /** Notification type ids (NotificationType.id) the user switched off. */
    val disabledNotificationTypesFlow: Flow<Set<String>>
    suspend fun setNotificationTypeEnabled(typeId: String, enabled: Boolean)

    /** Monthly budget per category in the user's default currency; absent = no budget. */
    val categoryBudgetsFlow: Flow<Map<String, Double>>
    suspend fun setCategoryBudget(category: String, amount: Double?)

    /** Groups whose pushes are silenced. */
    val mutedGroupIdsFlow: Flow<Set<String>>
    suspend fun setGroupMuted(groupId: String, muted: Boolean)

    /** Pinned groups in display order; at most [MAX_PINNED]. */
    val pinnedGroupIdsFlow: Flow<List<String>>
    /** Returns false when the pin limit is reached (nothing changes). */
    suspend fun togglePinnedGroup(groupId: String): Boolean
    suspend fun setPinnedGroups(ordered: List<String>)

    companion object { const val MAX_PINNED = 3 }

    /** Groups hidden from this user's list; balances still count. */
    val archivedGroupIdsFlow: Flow<Set<String>>
    suspend fun setGroupArchived(groupId: String, archived: Boolean)

    val openAiApiKeyFlow: Flow<String>
    suspend fun setOpenAiApiKey(key: String)

    val geminiApiKeyFlow: Flow<String>
    suspend fun setGeminiApiKey(key: String)

    val aiProviderFlow: Flow<String>
    suspend fun setAiProvider(provider: String)

    val dynamicColorFlow: Flow<Boolean>
    suspend fun setDynamicColor(enabled: Boolean)

    /** Base font: "default" (Google Sans Flex), "system", "serif" or "mono". Amounts always use mono. */
    val fontFlow: Flow<String>
    suspend fun setFont(font: String)

    /** User asked for minimal motion. The theme also reduces motion when system animations are off. */
    val reduceMotionFlow: Flow<Boolean>
    suspend fun setReduceMotion(enabled: Boolean)

    /** Vibration on taps, swipes and confirmations. On by default. */
    val hapticsFlow: Flow<Boolean>
    suspend fun setHaptics(enabled: Boolean)

    /** Payment detection from bank/UPI app notifications (needs notification access too). */
    val paymentDetectionFlow: Flow<Boolean>
    suspend fun setPaymentDetection(enabled: Boolean)
    /** Also detect money received; default on. */
    val detectCreditsFlow: Flow<Boolean>
    suspend fun setDetectCredits(enabled: Boolean)
    /** Also detect refunds; default off. */
    val detectRefundsFlow: Flow<Boolean>
    suspend fun setDetectRefunds(enabled: Boolean)


    val providerApiKeysFlow: Flow<Map<String, String>>
    suspend fun setProviderApiKey(providerId: String, key: String)

    val customEndpointFlow: Flow<String>
    suspend fun setCustomEndpoint(endpoint: String)

    val customModelFlow: Flow<String>
    suspend fun setCustomModel(model: String)

    /** Master switch for every AI feature. Keys stay stored while off. */
    val aiEnabledFlow: Flow<Boolean>
    suspend fun setAiEnabled(enabled: Boolean)

    /** Feature ids (AiFeature.name) the user switched off. Empty = all on. */
    val aiDisabledFeaturesFlow: Flow<Set<String>>
    suspend fun setAiFeatureEnabled(featureId: String, enabled: Boolean)

    /** providerId -> model override; absent = provider default. */
    val providerModelsFlow: Flow<Map<String, String>>
    suspend fun setProviderModel(providerId: String, model: String)

    /** providerId -> number of successful AI calls made from this device. */
    val aiUsageFlow: Flow<Map<String, Int>>
    /** providerId -> tokens billed across those calls (as reported by the provider). */
    val aiTokensFlow: Flow<Map<String, Int>>
    suspend fun incrementAiUsage(providerId: String, tokens: Int = 0)

    /** Epoch seconds of the last time the Activity tab was viewed on this device (0 = never). */
    val lastSeenActivityFlow: Flow<Long>
    suspend fun setLastSeenActivity(epochSeconds: Long)

    /** Settings as portable JSON (API keys excluded). */
    suspend fun exportSettings(): String
    /** Applies a settings backup; returns how many values were restored. Throws on a bad file. */
    suspend fun importSettings(json: String): Int
}
