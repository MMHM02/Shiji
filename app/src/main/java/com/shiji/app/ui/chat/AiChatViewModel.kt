package com.shiji.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiji.app.domain.ai.AdvisorContextBuilder
import com.shiji.core.ai.api.AiException
import com.shiji.core.ai.api.ChatMessage
import com.shiji.core.ai.api.ChatRole
import com.shiji.core.ai.api.Prompts
import com.shiji.core.ai.manager.AiServiceManager
import com.shiji.core.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val aiServiceManager: AiServiceManager,
    private val contextBuilder: AdvisorContextBuilder
) : ViewModel() {

    data class ChatMessageUi(
        val id: String = UUID.randomUUID().toString(),
        val role: ChatRole,
        val content: String,
        val timestamp: Long = System.currentTimeMillis(),
        val isStreaming: Boolean = false,
        val isError: Boolean = false
    )

    data class ChatUiState(
        val messages: List<ChatMessageUi> = emptyList(),
        val isAiTyping: Boolean = false,
        val aiConfigured: Boolean = true,   // assume true until checked; avoids flicker
        val modelLabel: String = "",
        val userName: String = "Shawn"
    )

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null

    init {
        viewModelScope.launch {
            aiServiceManager.clients.collect { clients ->
                _uiState.update {
                    it.copy(aiConfigured = clients.hasChat, modelLabel = clients.chatLabel)
                }
            }
        }
    }

    val suggestedQueries = listOf(
        "分析我今天的饮食",
        "本周饮食有什么可以改进的？",
        "推荐一份高蛋白晚餐",
        "减脂期能吃米饭吗？"
    )

    fun sendMessage(text: String, userName: String = _uiState.value.userName) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _uiState.value.isAiTyping) return

        val userMsg = ChatMessageUi(role = ChatRole.USER, content = trimmed)
        val aiPlaceholder = ChatMessageUi(role = ChatRole.ASSISTANT, content = "", isStreaming = true)
        _uiState.update {
            it.copy(messages = it.messages + userMsg + aiPlaceholder, isAiTyping = true)
        }

        streamJob = viewModelScope.launch {
            val hasData = contextBuilder.hasAnyData()
            val systemPrompt = if (hasData) {
                Prompts.buildAdvisorSystemPrompt(contextBuilder.build(userName))
            } else {
                Prompts.ADVISOR_FALLBACK_SYSTEM
            }

            // Rolling window: last 10 exchanges keep context without blowing up tokens.
            val history = _uiState.value.messages
                .filter { !it.isStreaming && !it.isError }
                .dropLast(1) // exclude the placeholder we just added
                .takeLast(20)
                .map { ChatMessage(role = it.role, content = it.content) }

            aiServiceManager.chatStream(history, systemPrompt)
                .catch { e -> markStreamFailed(e) }
                .collect { chunk -> appendToStream(chunk) }
            finalizeStream()
        }
    }

    fun retryLast(userName: String = _uiState.value.userName) {
        val lastUser = _uiState.value.messages.lastOrNull { it.role == ChatRole.USER } ?: return
        // Drop the failed/last AI bubble, then resend.
        _uiState.update { state ->
            val msgs = state.messages.toMutableList()
            while (msgs.isNotEmpty() && msgs.last().role == ChatRole.ASSISTANT) msgs.removeLast()
            state.copy(messages = msgs, isAiTyping = false)
        }
        sendMessage(lastUser.content, userName)
    }

    fun stopGenerating() {
        streamJob?.cancel()
        finalizeStream()
    }

    fun clearHistory() {
        streamJob?.cancel()
        _uiState.update { it.copy(messages = emptyList(), isAiTyping = false) }
    }

    // ==================== stream helpers ====================

    private fun appendToStream(chunk: String) {
        _uiState.update { state ->
            val msgs = state.messages.toMutableList()
            val last = msgs.lastOrNull() ?: return@update state
            if (last.role != ChatRole.ASSISTANT || !last.isStreaming) return@update state
            msgs[msgs.lastIndex] = last.copy(content = last.content + chunk)
            state.copy(messages = msgs)
        }
    }

    private fun finalizeStream() {
        _uiState.update { state ->
            val msgs = state.messages.toMutableList()
            val last = msgs.lastOrNull()
            if (last != null && last.isStreaming) {
                msgs[msgs.lastIndex] = if (last.content.isBlank()) {
                    last.copy(content = "（回复为空，请重试）", isStreaming = false, isError = true)
                } else {
                    last.copy(isStreaming = false)
                }
            }
            state.copy(messages = msgs, isAiTyping = false)
        }
    }

    private fun markStreamFailed(e: Throwable) {
        val msg = when (e) {
            is AiException -> e.message
            else -> AiException.Unknown(cause = e).message
        } ?: "AI 调用失败，请重试"
        _uiState.update { state ->
            val msgs = state.messages.toMutableList()
            val last = msgs.lastOrNull()
            if (last != null && last.isStreaming) {
                msgs[msgs.lastIndex] = last.copy(content = "❌ $msg", isStreaming = false, isError = true)
            }
            state.copy(messages = msgs, isAiTyping = false)
        }
    }
}
