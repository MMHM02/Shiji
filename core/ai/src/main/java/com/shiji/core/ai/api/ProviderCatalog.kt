package com.shiji.core.ai.api

/**
 * Catalog of supported AI providers (V1: domestic models + custom endpoint).
 * Single source of truth — used by the settings UI and the service manager.
 */
object ProviderCatalog {

    data class ProviderSpec(
        val id: String,                    // stable key: "deepseek", "kimi", ...
        val displayName: String,
        val baseUrl: String,               // empty for custom (user must fill)
        val defaultChatModel: String,
        val defaultVisionModel: String?,   // null = no vision support
        val isVisionCapable: Boolean,
        val description: String,
        val emoji: String,
        val keyHint: String = "sk-..."
    ) {
        val isCustom: Boolean get() = id == CUSTOM_ID
    }

    const val CUSTOM_ID = "custom"

    val all: List<ProviderSpec> = listOf(
        ProviderSpec(
            id = "deepseek",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com/v1",
            defaultChatModel = "deepseek-v4-flash",
            defaultVisionModel = null,
            isVisionCapable = false,
            description = "高性价比文本对话、食物解析推荐",
            emoji = "🦈"
        ),
        ProviderSpec(
            id = "kimi",
            displayName = "Kimi (Moonshot)",
            baseUrl = "https://api.moonshot.cn/v1",
            defaultChatModel = "kimi-k2.6",
            defaultVisionModel = null,
            isVisionCapable = false,
            description = "文本对话，支持 k2.6 / moonshot-v1 等",
            emoji = "🌙"
        ),
        ProviderSpec(
            id = "qwen",
            displayName = "通义千问 (Qwen)",
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            defaultChatModel = "qwen-plus",
            defaultVisionModel = "qwen-vl-plus",
            isVisionCapable = true,
            description = "文本对话 + 视觉模型可选",
            emoji = "☁️"
        ),
        ProviderSpec(
            id = "glm",
            displayName = "智谱 GLM",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4",
            defaultChatModel = "glm-4-flash",
            defaultVisionModel = "glm-4v",
            isVisionCapable = true,
            description = "文本 + 视觉多模态",
            emoji = "🧠"
        ),
        ProviderSpec(
            id = CUSTOM_ID,
            displayName = "自定义端点",
            baseUrl = "",
            defaultChatModel = "gpt-4o-mini",
            defaultVisionModel = "gpt-4o",
            isVisionCapable = true,
            description = "OpenAI 兼容 API（GPT-4o / 中转 / 自部署）",
            emoji = "🔌"
        )
    )

    fun byId(id: String): ProviderSpec? = all.firstOrNull { it.id == id }
}
