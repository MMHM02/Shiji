package com.shiji.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_providers")
data class AiProviderEntity(
    @PrimaryKey
    val id: String,                    // "deepseek", "kimi", "qwen", "glm", "custom"
    val displayName: String,
    val apiKeyEncrypted: String,       // AES encrypted API Key
    val baseUrl: String,
    val isEnabled: Boolean = true,
    val isVisionCapable: Boolean = false,
    val defaultVisionModel: String? = null,
    val defaultChatModel: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
