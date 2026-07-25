package com.shiji.core.ai.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException

/**
 * Classified AI errors — drives friendly Chinese messages + fallback strategies.
 * Mapping from HTTP status / network exceptions happens in the adapter layer.
 */
sealed class AiException(
    override val message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /** No provider configured at all / slot empty. */
    class NotConfigured(
        override val message: String = "请先配置 AI API Key"
    ) : AiException(message)

    /** 401 / 403 — key invalid, expired or lacking permission. */
    class Unauthorized(
        override val message: String = "API Key 无效或已过期，请检查后重新配置",
        cause: Throwable? = null
    ) : AiException(message, cause)

    /** 429 — rate limit or quota exhausted. */
    class QuotaExceeded(
        override val message: String = "API 调用次数受限或额度不足，请稍后再试",
        cause: Throwable? = null
    ) : AiException(message, cause)

    /** 400 — the request itself was rejected (bad params, model name, limits). */
    class BadRequest(
        override val message: String = "请求参数有误，请检查模型名称等配置",
        cause: Throwable? = null
    ) : AiException(message, cause)

    /** 404 — usually wrong baseUrl or model name. */
    class NotFound(
        override val message: String = "接口地址或模型不存在，请检查配置",
        cause: Throwable? = null
    ) : AiException(message, cause)

    /** 5xx from the provider. */
    class ServerError(
        override val message: String = "AI 服务暂时不可用，请稍后再试",
        cause: Throwable? = null
    ) : AiException(message, cause)

    /** Socket timeout / connect timeout. */
    class Timeout(
        override val message: String = "请求超时，请检查网络后重试",
        cause: Throwable? = null
    ) : AiException(message, cause)

    /** No connectivity / DNS / TLS failures. */
    class Network(
        override val message: String = "网络连接失败，请检查网络设置",
        cause: Throwable? = null
    ) : AiException(message, cause)

    /** Model does not support the requested capability (e.g. vision). */
    class FeatureNotSupported(
        override val message: String = "当前模型不支持该功能"
    ) : AiException(message)

    /** AI output could not be parsed into the expected structure. */
    class ParseError(
        override val message: String = "AI 返回格式异常，请重试",
        cause: Throwable? = null
    ) : AiException(message, cause)

    /** Anything else. */
    class Unknown(
        override val message: String = "AI 调用失败，请重试",
        cause: Throwable? = null
    ) : AiException(message, cause)

    companion object {
        /** Map an HTTP error code + body to a classified exception. */
        fun fromHttpCode(code: Int, body: String): AiException {
            val detail = extractProviderMessage(body)
            return when (code) {
                400 -> BadRequest(
                    message = detail?.let { "请求被拒绝：$it" } ?: "请求参数有误，请检查模型名称等配置",
                    cause = IOException("HTTP 400: ${body.take(120)}")
                )
                401, 403 -> Unauthorized(
                    message = detail?.let { "API Key 验证失败：$it" } ?: "API Key 无效或已过期，请检查后重新配置",
                    cause = IOException("HTTP $code")
                )
                404 -> NotFound(
                    message = detail?.let { "接口或模型不存在：$it" } ?: "接口地址或模型不存在，请检查配置",
                    cause = IOException("HTTP $code")
                )
                429 -> QuotaExceeded(cause = IOException("HTTP $code: $detail"))
                in 500..599 -> ServerError(cause = IOException("HTTP $code: $detail"))
                else -> Unknown("请求失败 (HTTP $code)${detail?.let { "：$it" } ?: ""}，请重试", IOException(body.take(120)))
            }
        }

        /**
         * Pull the provider's own error message out of the standard
         * OpenAI-style error envelope: {"error": {"message": "..."}}.
         * This is what users need to see (e.g. "max_tokens 参数必须在 1 到 4095 之间").
         */
        private fun extractProviderMessage(body: String): String? =
            runCatching {
                val root = Json.parseToJsonElement(body).jsonObject
                when (val error = root["error"]) {
                    is JsonObject -> error["message"]?.jsonPrimitive?.content
                    is JsonPrimitive -> error.content
                    else -> root["message"]?.jsonPrimitive?.content
                }
            }.getOrNull()?.take(100)
    }
}
