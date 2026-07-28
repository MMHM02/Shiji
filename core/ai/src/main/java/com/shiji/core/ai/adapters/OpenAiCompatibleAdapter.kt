package com.shiji.core.ai.adapters

import com.shiji.core.ai.api.*
import com.shiji.core.ai.parser.ResponseParser
import com.shiji.core.common.result.Result as ShiJiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Generic adapter for any OpenAI-compatible API endpoint.
 * Works with: OpenAI, DeepSeek, Kimi (Moonshot), Qwen (DashScope compatible mode),
 * GLM (Zhipu), and self-hosted OpenAI-compatible proxies.
 *
 * - Fully async (enqueue + suspendCancellableCoroutine) — cancellable, never blocks a thread.
 * - Real SSE streaming for chatStream().
 * - HTTP errors are mapped to classified [AiException]s with friendly Chinese messages.
 * - Never logs the API key.
 */
class OpenAiCompatibleAdapter(
    private val apiKey: String,
    private val baseUrl: String,
    override val modelName: String,
    private val isVisionCapable: Boolean = false,
    private val okHttpClient: OkHttpClient = defaultClient()
) : AiService {

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val apiUrl = baseUrl.trimEnd('/') + "/chat/completions"

    override suspend fun analyzeFoodImage(
        imageBytes: ByteArray, mimeType: String, prompt: String?
    ): ShiJiResult<FoodAnalysisResult> {
        if (!isVisionCapable) {
            return ShiJiResult.Error(AiException.FeatureNotSupported("当前模型不支持拍照识别，请在 AI 配置中选择视觉模型"))
        }
        val b64 = Base64.getEncoder().encodeToString(imageBytes)
        val dataUrl = "data:$mimeType;base64,$b64"
        return when (val res = executeChat(
            listOf(ChatMessage(ChatRole.USER, prompt ?: Prompts.FOOD_ANALYSIS, imageBase64 = dataUrl)),
            systemPrompt = Prompts.FOOD_ANALYSIS_SYSTEM
        )) {
            is ShiJiResult.Success ->
                ResponseParser.parseFoodAnalysis(res.data.text).fold(
                    onSuccess = { ShiJiResult.success(it) },
                    onFailure = { ShiJiResult.Error(AiException.ParseError(cause = it)) }
                )
            is ShiJiResult.Error -> res
        }
    }

    override suspend fun parseFoodDescription(text: String): ShiJiResult<List<FoodAnalysisResult.FoodItem>> {
        val prompt = Prompts.FOOD_PARSE.replace("{input}", text)
        return when (val res = executeChat(listOf(ChatMessage(ChatRole.USER, prompt)))) {
            is ShiJiResult.Success ->
                ResponseParser.parseFoodAnalysis(res.data.text).fold(
                    onSuccess = { ShiJiResult.success(it.items) },
                    onFailure = { ShiJiResult.Error(AiException.ParseError(cause = it)) }
                )
            is ShiJiResult.Error -> res
        }
    }

    override suspend fun chat(
        messages: List<ChatMessage>, systemPrompt: String?
    ): ShiJiResult<AiResponse> = executeChat(messages, systemPrompt)

    override fun chatStream(
        messages: List<ChatMessage>, systemPrompt: String?
    ): Flow<String> = callbackFlow {
        val body = buildRequestBody(messages, systemPrompt, stream = true)
        val call = okHttpClient.newCall(buildRequest(body))

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                close(mapNetworkError(e))
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val errBody = runCatching { response.body?.string() }.getOrNull().orEmpty()
                    response.close()
                    close(AiException.fromHttpCode(response.code, errBody))
                    return
                }
                // Read SSE lines on a background thread; emit delta content chunks.
                Thread {
                    try {
                        val source = response.body?.source()
                            ?: throw AiException.Unknown("响应为空")
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: continue
                            if (!line.startsWith("data:")) continue
                            val payload = line.removePrefix("data:").trim()
                            if (payload == "[DONE]") break
                            val delta = runCatching {
                                Json.parseToJsonElement(payload).jsonObject["choices"]
                                    ?.jsonArray?.firstOrNull()?.jsonObject
                                    ?.get("delta")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
                            }.getOrNull()
                            // NOTE: contentOrNull maps JSON null → null. Reasoning models
                            // (e.g. deepseek-v4) stream {"content": null} during the
                            // thinking phase; without this they'd emit literal "null" text.
                            if (!delta.isNullOrEmpty()) trySend(delta)
                        }
                        close()
                    } catch (t: Throwable) {
                        close(if (t is AiException) t else mapNetworkError(t))
                    } finally {
                        runCatching { response.close() }
                    }
                }.start()
            }
        })

        awaitClose { call.cancel() }
    }

    // ==================== internals ====================

    private suspend fun executeChat(
        messages: List<ChatMessage>,
        systemPrompt: String? = null
    ): ShiJiResult<AiResponse> = withContext(Dispatchers.IO) {
        try {
            val body = buildRequestBody(messages, systemPrompt, stream = false)
            val response = awaitCall(buildRequest(body))
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext ShiJiResult.Error(AiException.fromHttpCode(response.code, responseBody))
            }
            val json = runCatching { Json.parseToJsonElement(responseBody).jsonObject }
                .getOrElse { return@withContext ShiJiResult.Error(AiException.ParseError(cause = it)) }

            val content = json["choices"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("message")?.jsonObject?.get("content")
                ?.jsonPrimitive?.content
                ?: return@withContext ShiJiResult.Error(AiException.ParseError("AI 返回内容为空"))

            val usage = json["usage"]?.jsonObject
            ShiJiResult.success(
                AiResponse(
                    text = content,
                    model = json["model"]?.jsonPrimitive?.contentOrNull ?: modelName,
                    inputTokens = usage?.get("prompt_tokens")?.jsonPrimitive?.intOrNull ?: 0,
                    outputTokens = usage?.get("completion_tokens")?.jsonPrimitive?.intOrNull ?: 0
                )
            )
        } catch (t: Throwable) {
            ShiJiResult.Error(t as? AiException ?: mapNetworkError(t))
        }
    }

    private fun buildRequestBody(
        messages: List<ChatMessage>, systemPrompt: String?, stream: Boolean
    ): String {
        val msgArray = buildJsonArray {
            if (systemPrompt != null) {
                add(buildJsonObject { put("role", "system"); put("content", systemPrompt) })
            }
            messages.forEach { msg ->
                add(buildJsonObject {
                    put("role", msg.role.name.lowercase())
                    if (msg.imageBase64 != null) {
                        put("content", buildJsonArray {
                            add(buildJsonObject { put("type", "text"); put("text", msg.content) })
                            add(buildJsonObject {
                                put("type", "image_url")
                                put("image_url", buildJsonObject { put("url", msg.imageBase64) })
                            })
                        })
                    } else {
                        put("content", msg.content)
                    }
                })
            }
        }
        return buildJsonObject {
            put("model", modelName)
            put("messages", msgArray)
            // Omit temperature — reasoning models (Kimi k2, DeepSeek v4) reject
            // non-1 values; every provider defaults to a sensible value anyway.
            put("max_tokens", 2048)
            if (stream) put("stream", true)
        }.toString()
    }

    private fun buildRequest(body: String): Request = Request.Builder()
        .url(apiUrl)
        .addHeader("Authorization", "Bearer $apiKey")
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "application/json")
        .post(body.toRequestBody(jsonMediaType))
        .build()

    /** Await an OkHttp call with proper coroutine cancellation. */
    private suspend fun awaitCall(request: Request): Response =
        suspendCancellableCoroutine { cont ->
            val call = okHttpClient.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    if (cont.isActive) cont.resume(response) else response.close()
                }
            })
        }

    private fun mapNetworkError(t: Throwable): AiException = when (t) {
        is AiException -> t
        is SocketTimeoutException -> AiException.Timeout(cause = t)
        is IOException -> AiException.Network(cause = t)
        else -> AiException.Unknown(cause = t)
    }
}
