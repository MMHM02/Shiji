package com.shiji.core.ai.manager

import com.shiji.core.ai.adapters.OpenAiCompatibleAdapter
import com.shiji.core.ai.api.*
import com.shiji.core.ai.config.AiConfigRepository
import com.shiji.core.ai.usage.AiUsageTracker
import com.shiji.core.common.result.Result
import com.shiji.core.data.entity.AiProviderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/**
 * Central AI router — the single entry point the app layer talks to.
 *
 * Responsibilities:
 *  - Restore configuration on startup (providers from Room, keys from Keystore, slots from DataStore).
 *  - Hold ready-to-use adapters for the two slots (chat / vision) as a hot [StateFlow].
 *  - Route high-level operations (chat / vision analysis / food parse) and record usage.
 *  - Connection testing with the user's real model.
 */
class AiServiceManager(
    private val configRepository: AiConfigRepository,
    private val usageTracker: AiUsageTracker,
    private val okHttpClient: OkHttpClient,
    private val scope: CoroutineScope
) {

    /** Snapshot of everything the UI needs to know about AI availability. */
    data class AiClients(
        val providers: List<AiProviderEntity> = emptyList(),
        val chatProviderId: String? = null,
        val chatModel: String? = null,
        val visionProviderId: String? = null,
        val visionModel: String? = null,
        val chatAdapter: OpenAiCompatibleAdapter? = null,
        val visionAdapter: OpenAiCompatibleAdapter? = null
    ) {
        val hasChat: Boolean get() = chatAdapter != null
        val hasVision: Boolean get() = visionAdapter != null
        val hasAnyProvider: Boolean get() = providers.isNotEmpty()

        /** Short label for UI chips, e.g. "DeepSeek · deepseek-chat". */
        val chatLabel: String
            get() = if (chatModel != null) "${providerName(chatProviderId)} · $chatModel" else "未配置"

        fun providerName(providerId: String?): String =
            providers.firstOrNull { it.id == providerId }?.displayName
                ?: ProviderCatalog.byId(providerId ?: "")?.displayName
                ?: providerId ?: "未配置"
    }

    private val _clients = MutableStateFlow(AiClients())
    val clients: StateFlow<AiClients> = _clients.asStateFlow()

    init {
        // Restore persisted configuration as soon as the manager is created.
        scope.launch { reload() }
        // Keep providers list live (settings screen edits propagate automatically).
        scope.launch {
            configRepository.providers.collect { providers ->
                _clients.update { it.copy(providers = providers) }
            }
        }
    }

    /** Rebuild adapters from persisted state. Call after any configuration change. */
    suspend fun reload() {
        val providers = configRepository.providers.first()
        val chatSlot = configRepository.getChatSlot()
        val visionSlot = configRepository.getVisionSlot()

        fun buildAdapter(slot: Pair<String, String>?): OpenAiCompatibleAdapter? {
            if (slot == null) return null
            val (providerId, model) = slot
            val key = configRepository.getApiKey(providerId) ?: return null
            val provider = providers.firstOrNull { it.id == providerId } ?: return null
            return OpenAiCompatibleAdapter(
                apiKey = key,
                baseUrl = provider.baseUrl,
                modelName = model,
                isVisionCapable = provider.isVisionCapable,
                okHttpClient = okHttpClient
            )
        }

        _clients.update {
            it.copy(
                providers = providers,
                chatProviderId = chatSlot?.first,
                chatModel = chatSlot?.second,
                visionProviderId = visionSlot?.first,
                visionModel = visionSlot?.second,
                chatAdapter = buildAdapter(chatSlot),
                visionAdapter = buildAdapter(visionSlot)
            )
        }
    }

    // ==================== high-level operations (usage-tracked) ====================

    suspend fun chat(
        messages: List<ChatMessage>,
        systemPrompt: String? = null
    ): Result<AiResponse> {
        val snapshot = _clients.value
        val adapter = snapshot.chatAdapter
            ?: return Result.Error(AiException.NotConfigured())
        val result = adapter.chat(messages, systemPrompt)
        usageTracker.record(
            providerId = snapshot.chatProviderId ?: "unknown",
            model = snapshot.chatModel ?: "unknown",
            feature = AiUsageTracker.Feature.CHAT,
            inputTokens = result.getOrNull()?.inputTokens ?: estimateTokens(messages, systemPrompt),
            outputTokens = result.getOrNull()?.outputTokens ?: 0,
            success = result.isSuccess
        )
        return result
    }

    /**
     * Streaming chat. Token usage for streams is estimated (providers only send
     * usage with stream_options, which is not universally supported).
     */
    fun chatStream(
        messages: List<ChatMessage>,
        systemPrompt: String? = null
    ): Flow<String> {
        val snapshot = _clients.value
        val adapter = snapshot.chatAdapter ?: return kotlinx.coroutines.flow.flow {
            throw AiException.NotConfigured()
        }
        return adapter.chatStream(messages, systemPrompt)
            .onCompletion { cause ->
                usageTracker.record(
                    providerId = snapshot.chatProviderId ?: "unknown",
                    model = snapshot.chatModel ?: "unknown",
                    feature = AiUsageTracker.Feature.CHAT,
                    inputTokens = estimateTokens(messages, systemPrompt),
                    outputTokens = 0,
                    success = cause == null
                )
            }
    }

    suspend fun analyzeFoodImage(
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): Result<FoodAnalysisResult> {
        val snapshot = _clients.value
        val adapter = snapshot.visionAdapter
            ?: return Result.Error(
                AiException.NotConfigured("尚未配置视觉模型，请先在 AI 配置中选择支持视觉的模型")
            )
        val result = adapter.analyzeFoodImage(imageBytes, mimeType)
        usageTracker.record(
            providerId = snapshot.visionProviderId ?: "unknown",
            model = snapshot.visionModel ?: "unknown",
            feature = AiUsageTracker.Feature.FOOD_PHOTO,
            inputTokens = imageBytes.size / 750, // rough vision-token estimate
            outputTokens = 0,
            success = result.isSuccess
        )
        return result
    }

    suspend fun parseFoodDescription(
        text: String
    ): Result<List<FoodAnalysisResult.FoodItem>> {
        val snapshot = _clients.value
        val adapter = snapshot.chatAdapter
            ?: return Result.Error(AiException.NotConfigured())
        val result = adapter.parseFoodDescription(text)
        usageTracker.record(
            providerId = snapshot.chatProviderId ?: "unknown",
            model = snapshot.chatModel ?: "unknown",
            feature = AiUsageTracker.Feature.FOOD_PARSE,
            inputTokens = estimateTokens(listOf(ChatMessage(ChatRole.USER, text)), null),
            outputTokens = 0,
            success = result.isSuccess
        )
        return result
    }

    // ==================== connection test ====================

    /**
     * Real connection test — sends a minimal request with the user's actual model.
     */
    suspend fun testConnection(
        baseUrl: String,
        apiKey: String,
        model: String
    ): Result<Unit> {
        if (apiKey.isBlank()) return Result.Error(AiException.Unauthorized("API Key 不能为空"))
        if (baseUrl.isBlank()) return Result.Error(AiException.NotFound("接口地址不能为空"))
        val probe = OpenAiCompatibleAdapter(
            apiKey = apiKey.trim(),
            baseUrl = baseUrl.trim(),
            modelName = model.trim(),
            isVisionCapable = false,
            okHttpClient = okHttpClient
        )
        return when (val res = probe.chat(listOf(ChatMessage(ChatRole.USER, "hi")))) {
            is Result.Success -> Result.success(Unit)
            is Result.Error -> Result.Error(res.exception, res.message)
        }
    }

    // ==================== helpers ====================

    private fun estimateTokens(messages: List<ChatMessage>, systemPrompt: String?): Int {
        // ~1 token per CJK char, ~0.25 per latin char; good enough for stats.
        val chars = (systemPrompt?.length ?: 0) + messages.sumOf { it.content.length }
        return (chars * 0.6).toInt().coerceAtLeast(1)
    }
}
