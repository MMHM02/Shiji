package com.shiji.core.ai.api

import com.shiji.core.common.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Unified AI service interface.
 * All AI provider adapters implement this interface.
 */
interface AiService {

    /** The model this adapter instance is bound to. */
    val modelName: String

    /**
     * Analyze food image using a vision-capable model.
     * @param imageBytes Raw image bytes (already compressed)
     * @param mimeType Image MIME type (e.g., "image/jpeg")
     * @param prompt Custom analysis prompt (uses default if null)
     * @return Structured food analysis result
     */
    suspend fun analyzeFoodImage(
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg",
        prompt: String? = null
    ): Result<FoodAnalysisResult>

    /**
     * Parse natural language food description into structured items.
     */
    suspend fun parseFoodDescription(
        text: String
    ): Result<List<FoodAnalysisResult.FoodItem>>

    /**
     * Chat completion (non-streaming). Returns content + token usage.
     */
    suspend fun chat(
        messages: List<ChatMessage>,
        systemPrompt: String? = null
    ): Result<AiResponse>

    /**
     * Chat completion (streaming, SSE). Emits incremental text chunks.
     * The flow completes when the stream ends; throws AiException on failure.
     */
    fun chatStream(
        messages: List<ChatMessage>,
        systemPrompt: String? = null
    ): Flow<String>
}

data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val imageBase64: String? = null
)

enum class ChatRole { SYSTEM, USER, ASSISTANT }

/**
 * Non-streaming chat result with token usage for cost tracking.
 */
data class AiResponse(
    val text: String,
    val model: String,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0
)

data class FoodAnalysisResult(
    val items: List<FoodItem>,
    val totalCalories: Double,
    val confidence: Float,
    val rawResponse: String
) {
    data class FoodItem(
        val name: String,
        val portion: Double,
        val portionUnit: String,
        val calories: Double,
        val proteinGrams: Double,
        val carbsGrams: Double,
        val fatGrams: Double,
        val confidence: Float
    )
}
