package com.shiji.core.ai.config

import com.shiji.core.ai.api.ProviderCatalog
import com.shiji.core.common.security.EncryptedKeyStore
import com.shiji.core.data.dao.AiProviderDao
import com.shiji.core.data.datastore.UserPreferences
import com.shiji.core.data.entity.AiProviderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Single source of truth for AI configuration.
 *
 * Three-layer storage:
 *  - Room ([AiProviderDao]): provider metadata (name, baseUrl, models) — NO secrets.
 *  - [EncryptedKeyStore]: API keys only, encrypted via Android Keystore.
 *  - DataStore ([UserPreferences]): which provider+model fills the vision/chat slots.
 *
 * A key never touches Room or logs; it lives only in the encrypted store and in memory.
 */
class AiConfigRepository(
    private val aiProviderDao: AiProviderDao,
    private val encryptedKeyStore: EncryptedKeyStore,
    private val userPreferences: UserPreferences
) {

    val providers: Flow<List<AiProviderEntity>> = aiProviderDao.getAll()

    // ---------- provider CRUD ----------

    /**
     * Save a provider configuration. The API key goes ONLY to the encrypted store;
     * Room keeps metadata with an empty apiKeyEncrypted placeholder.
     */
    suspend fun saveProvider(
        providerId: String,
        displayName: String,
        baseUrl: String,
        chatModel: String,
        visionModel: String?,
        isVisionCapable: Boolean,
        apiKey: String
    ) {
        encryptedKeyStore.saveKey(providerId, apiKey.trim())
        aiProviderDao.upsert(
            AiProviderEntity(
                id = providerId,
                displayName = displayName,
                apiKeyEncrypted = "", // secrets never live in Room
                baseUrl = baseUrl.trim().trimEnd('/'),
                isEnabled = true,
                isVisionCapable = isVisionCapable && visionModel != null,
                defaultVisionModel = visionModel,
                defaultChatModel = chatModel,
                updatedAt = System.currentTimeMillis()
            )
        )
        userPreferences.setApiKeyConfigured(true)

        // Auto-fill empty slots so the feature works right after first setup.
        if (userPreferences.chatProviderId.first().isBlank()) {
            setChatSlot(providerId, chatModel)
        }
        if (isVisionCapable && visionModel != null && userPreferences.visionProviderId.first().isBlank()) {
            setVisionSlot(providerId, visionModel)
        }

        // If this provider already occupies a slot, sync the slot's model name
        // with the freshly saved one — otherwise reconfiguring a provider would
        // leave the slot pointing at a stale (possibly invalid) model.
        if (userPreferences.chatProviderId.first() == providerId) {
            userPreferences.setChatModel(chatModel)
        }
        if (visionModel != null && userPreferences.visionProviderId.first() == providerId) {
            userPreferences.setVisionModel(visionModel)
        }
    }

    suspend fun deleteProvider(providerId: String) {
        encryptedKeyStore.deleteKey(providerId)
        aiProviderDao.deleteById(providerId)
        // Clear slots that pointed at the deleted provider.
        if (userPreferences.chatProviderId.first() == providerId) {
            userPreferences.setChatProviderId("")
            userPreferences.setChatModel("")
        }
        if (userPreferences.visionProviderId.first() == providerId) {
            userPreferences.setVisionProviderId("")
            userPreferences.setVisionModel("")
        }
        if (encryptedKeyStore.getAllProviderIds().isEmpty()) {
            userPreferences.setApiKeyConfigured(false)
        }
    }

    fun getApiKey(providerId: String): String? = encryptedKeyStore.getKey(providerId)

    fun hasAnyProvider(): Boolean = encryptedKeyStore.getAllProviderIds().isNotEmpty()

    // ---------- slot management ----------

    suspend fun setChatSlot(providerId: String, model: String) {
        userPreferences.setChatProviderId(providerId)
        userPreferences.setChatModel(model)
    }

    suspend fun setVisionSlot(providerId: String, model: String) {
        userPreferences.setVisionProviderId(providerId)
        userPreferences.setVisionModel(model)
    }

    suspend fun getChatSlot(): Pair<String, String>? {
        val id = userPreferences.chatProviderId.first()
        val model = userPreferences.chatModel.first()
        return if (id.isNotBlank() && model.isNotBlank()) id to model else null
    }

    suspend fun getVisionSlot(): Pair<String, String>? {
        val id = userPreferences.visionProviderId.first()
        val model = userPreferences.visionModel.first()
        return if (id.isNotBlank() && model.isNotBlank()) id to model else null
    }

    suspend fun getProviderById(providerId: String): AiProviderEntity? =
        aiProviderDao.getById(providerId)

    // ---------- helpers for the setup UI ----------

    /** Providers from the catalog merged with their configured state. */
    suspend fun getCatalogWithState(): List<Pair<ProviderCatalog.ProviderSpec, Boolean>> =
        ProviderCatalog.all.map { spec -> spec to (encryptedKeyStore.hasKey(spec.id)) }
}
