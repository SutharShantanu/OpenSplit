package com.opensplit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.opensplit.domain.repository.UserPreferencesRepository
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepositoryImpl(private val context: Context) : UserPreferencesRepository {

    companion object {
        const val DEFAULT_OPENAI_KEY = "" // User must provide their own key in Account settings
        const val DEFAULT_GEMINI_KEY = "" // User must provide their own key in Account settings
    }

    private val THEME_KEY = stringPreferencesKey("theme")
    private val PERMISSION_PRIMER_KEY = booleanPreferencesKey("has_completed_permission_primer")
    private val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")
    private val PINNED_GROUPS_KEY = stringSetPreferencesKey("pinned_group_ids")
    private val ARCHIVED_GROUPS_KEY = stringSetPreferencesKey("archived_group_ids")
    private val DISABLED_NOTIF_TYPES_KEY = stringSetPreferencesKey("disabled_notification_types")
    private val MUTED_GROUPS_KEY = stringSetPreferencesKey("muted_group_ids")
    private val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
    private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    private val AI_PROVIDER_KEY = stringPreferencesKey("ai_provider")
    private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
    private val COLOR_PALETTE_KEY = stringPreferencesKey("color_palette")
    private val AMOLED_KEY = booleanPreferencesKey("amoled_pure_black")

    private val CUSTOM_ENDPOINT_KEY = stringPreferencesKey("custom_ai_endpoint")
    private val CUSTOM_MODEL_KEY = stringPreferencesKey("custom_ai_model")

    override val themeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_KEY] ?: "system"
        }

    override suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    override val hasCompletedPermissionPrimer: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PERMISSION_PRIMER_KEY] ?: false
        }

    override suspend fun setHasCompletedPermissionPrimer(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PERMISSION_PRIMER_KEY] = completed
        }
    }

    override val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATIONS_KEY] ?: true
        }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_KEY] = enabled
        }
    }

    // Order matters now, so pins live in an ordered string; the old set is read once as a fallback.
    private val PINNED_ORDER_KEY = androidx.datastore.preferences.core.stringPreferencesKey("pinned_group_order")
    private fun pinnedOrder(prefs: androidx.datastore.preferences.core.Preferences): List<String> =
        prefs[PINNED_ORDER_KEY]?.split(',')?.filter { it.isNotBlank() } ?: (prefs[PINNED_GROUPS_KEY]?.toList() ?: emptyList())
    override val pinnedGroupIdsFlow: Flow<List<String>> = context.dataStore.data.map { pinnedOrder(it).take(UserPreferencesRepository.MAX_PINNED) }

    override val disabledNotificationTypesFlow: Flow<Set<String>> = context.dataStore.data.map { it[DISABLED_NOTIF_TYPES_KEY] ?: emptySet() }
    override suspend fun setNotificationTypeEnabled(typeId: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[DISABLED_NOTIF_TYPES_KEY] ?: emptySet()
            prefs[DISABLED_NOTIF_TYPES_KEY] = if (enabled) current - typeId else current + typeId
        }
    }

    private val BUDGETS_KEY = stringSetPreferencesKey("category_budgets")
    override val categoryBudgetsFlow: Flow<Map<String, Double>> = context.dataStore.data.map { prefs ->
        (prefs[BUDGETS_KEY] ?: emptySet()).mapNotNull { entry ->
            val (cat, amt) = entry.split('=', limit = 2).let { if (it.size == 2) it[0] to it[1] else return@mapNotNull null }
            amt.toDoubleOrNull()?.let { cat to it }
        }.toMap()
    }
    override suspend fun setCategoryBudget(category: String, amount: Double?) {
        context.dataStore.edit { prefs ->
            val others = (prefs[BUDGETS_KEY] ?: emptySet()).filterNot { it.substringBefore('=') == category }.toSet()
            prefs[BUDGETS_KEY] = if (amount == null || amount <= 0) others else others + "$category=$amount"
        }
    }

    override val mutedGroupIdsFlow: Flow<Set<String>> = context.dataStore.data.map { it[MUTED_GROUPS_KEY] ?: emptySet() }
    override suspend fun setGroupMuted(groupId: String, muted: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[MUTED_GROUPS_KEY] ?: emptySet()
            prefs[MUTED_GROUPS_KEY] = if (muted) current + groupId else current - groupId
        }
    }

    override val archivedGroupIdsFlow: Flow<Set<String>> = context.dataStore.data.map { it[ARCHIVED_GROUPS_KEY] ?: emptySet() }

    override suspend fun setGroupArchived(groupId: String, archived: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[ARCHIVED_GROUPS_KEY] ?: emptySet()
            prefs[ARCHIVED_GROUPS_KEY] = if (archived) current + groupId else current - groupId
        }
    }

    override suspend fun togglePinnedGroup(groupId: String): Boolean {
        var ok = true
        context.dataStore.edit { preferences ->
            val current = pinnedOrder(preferences)
            val next = when {
                groupId in current -> current - groupId
                current.size >= UserPreferencesRepository.MAX_PINNED -> { ok = false; current }
                else -> current + groupId
            }
            preferences[PINNED_ORDER_KEY] = next.joinToString(",")
        }
        return ok
    }

    override suspend fun setPinnedGroups(ordered: List<String>) {
        context.dataStore.edit { it[PINNED_ORDER_KEY] = ordered.distinct().take(UserPreferencesRepository.MAX_PINNED).joinToString(",") }
    }

    override val openAiApiKeyFlow: Flow<String> get() = providerApiKeysFlow.map { it["openai"] ?: "" }

    override suspend fun setOpenAiApiKey(key: String) {
        setProviderApiKey("openai", key)
    }

    override val geminiApiKeyFlow: Flow<String> get() = providerApiKeysFlow.map { it["gemini"] ?: "" }

    override suspend fun setGeminiApiKey(key: String) {
        setProviderApiKey("gemini", key)
    }

    override val aiProviderFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[AI_PROVIDER_KEY] ?: "anthropic"
        }

    override suspend fun setAiProvider(provider: String) {
        context.dataStore.edit { preferences ->
            preferences[AI_PROVIDER_KEY] = provider
        }
    }

    override val dynamicColorFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            // Material You by default, like Google's own apps; the brand palette is the opt-out.
            preferences[DYNAMIC_COLOR_KEY] ?: (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
        }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }

    override val colorPaletteFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[COLOR_PALETTE_KEY] ?: "material"
        }

    override suspend fun setColorPalette(palette: String) {
        context.dataStore.edit { preferences ->
            preferences[COLOR_PALETTE_KEY] = palette
            if (palette == "dynamic") {
                preferences[DYNAMIC_COLOR_KEY] = true
            } else {
                preferences[DYNAMIC_COLOR_KEY] = false
            }
        }
    }

    override val amoledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AMOLED_KEY] ?: false
        }

    override suspend fun setAmoled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AMOLED_KEY] = enabled
        }
    }

    /**
     * API keys live in EncryptedSharedPreferences (AES-256, hardware-backed master key), not in
     * the plain DataStore. Keys written by older builds are migrated on first read and wiped.
     */
    private val secureKeys by lazy {
        try {
            openSecureKeys()
        } catch (e: Exception) {
            // Ciphertext restored from backup (or a reinstall) no longer matches the keystore
            // master key -> AEADBadTagException. Drop the unreadable file and start fresh
            // rather than crash on every launch; the user re-enters their keys.
            android.util.Log.w("UserPreferences", "ai_keys unreadable, resetting", e)
            context.deleteSharedPreferences("ai_keys")
            openSecureKeys()
        }
    }

    private fun openSecureKeys(): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            context, "ai_keys", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    private val knownProviders = listOf("openai", "gemini", "anthropic", "groq", "mistral", "deepseek", "custom")
    private val keysState = MutableStateFlow<Map<String, String>?>(null)

    private suspend fun loadKeys(): Map<String, String> {
        keysState.value?.let { return it }
        val legacy = context.dataStore.data.first()
        val migrated = mutableMapOf<String, String>()
        for (provider in knownProviders) {
            val old = legacy[stringPreferencesKey("api_key_$provider")]
                ?: (if (provider == "openai") legacy[OPENAI_API_KEY] else if (provider == "gemini") legacy[GEMINI_API_KEY] else null)
            if (!old.isNullOrBlank() && !secureKeys.contains(provider)) migrated[provider] = old
        }
        if (migrated.isNotEmpty()) {
            secureKeys.edit().apply { migrated.forEach { (k, v) -> putString(k, v) } }.apply()
            context.dataStore.edit { prefs ->
                knownProviders.forEach { prefs.remove(stringPreferencesKey("api_key_$it")) }
                prefs.remove(OPENAI_API_KEY); prefs.remove(GEMINI_API_KEY)
            }
        }
        val current = knownProviders.mapNotNull { id -> secureKeys.getString(id, null)?.takeIf { it.isNotBlank() }?.let { id to it } }.toMap()
        keysState.value = current
        return current
    }

    override val providerApiKeysFlow: Flow<Map<String, String>> = keysState.asStateFlow()
        .map { it ?: loadKeys() }

    override suspend fun setProviderApiKey(providerId: String, key: String) {
        loadKeys()
        secureKeys.edit().apply { if (key.isBlank()) remove(providerId) else putString(providerId, key) }.apply()
        keysState.value = (keysState.value ?: emptyMap()).toMutableMap().apply {
            if (key.isBlank()) remove(providerId) else put(providerId, key)
        }
    }

    override val customEndpointFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[CUSTOM_ENDPOINT_KEY] ?: "http://localhost:11434/v1"
        }

    override suspend fun setCustomEndpoint(endpoint: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_ENDPOINT_KEY] = endpoint
        }
    }

    override val customModelFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[CUSTOM_MODEL_KEY] ?: "llama3:latest"
        }

    override suspend fun setCustomModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_MODEL_KEY] = model
        }
    }

    private val AI_ENABLED_KEY = booleanPreferencesKey("ai_enabled")
    private val AI_DISABLED_FEATURES_KEY = stringSetPreferencesKey("ai_disabled_features")

    override val aiEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[AI_ENABLED_KEY] ?: true }
    override suspend fun setAiEnabled(enabled: Boolean) {
        context.dataStore.edit { it[AI_ENABLED_KEY] = enabled }
    }

    override val aiDisabledFeaturesFlow: Flow<Set<String>> = context.dataStore.data.map { it[AI_DISABLED_FEATURES_KEY] ?: emptySet() }
    override suspend fun setAiFeatureEnabled(featureId: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[AI_DISABLED_FEATURES_KEY] ?: emptySet()
            prefs[AI_DISABLED_FEATURES_KEY] = if (enabled) current - featureId else current + featureId
        }
    }

    override val providerModelsFlow: Flow<Map<String, String>> = context.dataStore.data.map { prefs ->
        knownProviders.mapNotNull { id -> prefs[stringPreferencesKey("ai_model_$id")]?.let { id to it } }.toMap()
    }
    override suspend fun setProviderModel(providerId: String, model: String) {
        context.dataStore.edit { prefs ->
            val key = stringPreferencesKey("ai_model_$providerId")
            if (model.isBlank()) prefs.remove(key) else prefs[key] = model
        }
    }

    override val aiUsageFlow: Flow<Map<String, Int>> = context.dataStore.data.map { prefs ->
        knownProviders.mapNotNull { id -> prefs[intPreferencesKey("ai_usage_$id")]?.let { id to it } }.toMap()
    }
    override val aiTokensFlow: Flow<Map<String, Int>> = context.dataStore.data.map { prefs ->
        knownProviders.mapNotNull { id -> prefs[intPreferencesKey("ai_tokens_$id")]?.let { id to it } }.toMap()
    }
    override suspend fun incrementAiUsage(providerId: String, tokens: Int) {
        context.dataStore.edit { prefs ->
            val key = intPreferencesKey("ai_usage_$providerId")
            prefs[key] = (prefs[key] ?: 0) + 1
            val tKey = intPreferencesKey("ai_tokens_$providerId")
            prefs[tKey] = (prefs[tKey] ?: 0) + tokens
        }
    }

    private val LAST_SEEN_ACTIVITY_KEY = androidx.datastore.preferences.core.longPreferencesKey("last_seen_activity")
    override val lastSeenActivityFlow: Flow<Long> = context.dataStore.data.map { it[LAST_SEEN_ACTIVITY_KEY] ?: 0L }
    override suspend fun setLastSeenActivity(epochSeconds: Long) {
        context.dataStore.edit { it[LAST_SEEN_ACTIVITY_KEY] = epochSeconds }
    }

    override suspend fun exportSettings(): String = SettingsBackup.encode(context.dataStore.data.first())

    override suspend fun importSettings(json: String): Int {
        var restored = 0
        context.dataStore.edit { prefs -> restored = SettingsBackup.decodeInto(json, prefs) }
        return restored
    }
}

/** Versioned JSON form of the DataStore settings, for Backup & Export. */
internal object SettingsBackup {
    private const val FORMAT = "opensplit-settings"
    private const val VERSION = 1

    /** Secrets never leave the device; the primer flag is per-install. */
    private fun portable(key: String) = !key.contains("api_key") && key != "has_completed_permission_primer"

    fun encode(prefs: Preferences): String {
        val values = org.json.JSONObject()
        for ((key, value) in prefs.asMap()) {
            if (!portable(key.name)) continue
            val (type, v) = when (value) {
                is Boolean -> "b" to value
                is Int -> "i" to value
                is Long -> "l" to value
                is Float -> "f" to value.toDouble()
                is Double -> "d" to value
                is String -> "s" to value
                is Set<*> -> "ss" to org.json.JSONArray(value.filterIsInstance<String>())
                else -> continue
            }
            values.put(key.name, org.json.JSONObject().put("t", type).put("v", v))
        }
        return org.json.JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("values", values)
            .toString(2)
    }

    fun decodeInto(json: String, prefs: androidx.datastore.preferences.core.MutablePreferences): Int {
        require(json.length < 1_000_000) { "File is too large to be a settings backup" }
        val root = org.json.JSONObject(json)
        require(root.optString("format") == FORMAT) { "Not an OpenSplit settings backup" }
        require(root.optInt("version") in 1..VERSION) { "Backup is from a newer OpenSplit version" }
        val values = root.getJSONObject("values")
        var count = 0
        for (name in values.keys()) {
            if (!portable(name)) continue
            val entry = values.optJSONObject(name) ?: continue
            when (entry.optString("t")) {
                "b" -> prefs[booleanPreferencesKey(name)] = entry.getBoolean("v")
                "i" -> prefs[intPreferencesKey(name)] = entry.getInt("v")
                "l" -> prefs[androidx.datastore.preferences.core.longPreferencesKey(name)] = entry.getLong("v")
                "f" -> prefs[androidx.datastore.preferences.core.floatPreferencesKey(name)] = entry.getDouble("v").toFloat()
                "d" -> prefs[androidx.datastore.preferences.core.doublePreferencesKey(name)] = entry.getDouble("v")
                "s" -> prefs[stringPreferencesKey(name)] = entry.getString("v")
                "ss" -> {
                    val arr = entry.getJSONArray("v")
                    prefs[stringSetPreferencesKey(name)] = (0 until arr.length()).map { arr.getString(it) }.toSet()
                }
                else -> continue
            }
            count++
        }
        return count
    }
}
