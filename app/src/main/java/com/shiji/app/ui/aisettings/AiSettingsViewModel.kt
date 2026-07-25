package com.shiji.app.ui.aisettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiji.core.ai.api.ProviderCatalog
import com.shiji.core.ai.config.AiConfigRepository
import com.shiji.core.ai.manager.AiServiceManager
import com.shiji.core.ai.usage.AiUsageTracker
import com.shiji.core.common.result.Result
import com.shiji.core.data.entity.AiProviderEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val configRepository: AiConfigRepository,
    private val aiServiceManager: AiServiceManager,
    private val usageTracker: AiUsageTracker
) : ViewModel() {

    sealed interface TestState {
        data object Idle : TestState
        data object Testing : TestState
        data object Success : TestState
        data class Failed(val message: String) : TestState
    }

    data class AiSettingsUiState(
        val configuredProviders: List<AiProviderEntity> = emptyList(),
        val chatProviderId: String? = null,
        val chatModel: String? = null,
        val visionProviderId: String? = null,
        val visionModel: String? = null,
        val usage: AiUsageTracker.UsageSummary? = null
    )

    private val _uiState = MutableStateFlow(AiSettingsUiState())
    val uiState: StateFlow<AiSettingsUiState> = _uiState.asStateFlow()

    private val _testState = MutableStateFlow<TestState>(TestState.Idle)
    val testState: StateFlow<TestState> = _testState.asStateFlow()

    val catalog = ProviderCatalog.all

    init {
        viewModelScope.launch {
            aiServiceManager.clients.collect { clients ->
                _uiState.update {
                    it.copy(
                        configuredProviders = clients.providers,
                        chatProviderId = clients.chatProviderId,
                        chatModel = clients.chatModel,
                        visionProviderId = clients.visionProviderId,
                        visionModel = clients.visionModel
                    )
                }
            }
        }
        viewModelScope.launch {
            usageTracker.monthlySummary.collect { summary ->
                _uiState.update { it.copy(usage = summary) }
            }
        }
    }

    // ---------- provider CRUD ----------

    fun saveProvider(
        spec: ProviderCatalog.ProviderSpec,
        apiKey: String,
        baseUrl: String,
        chatModel: String,
        visionModel: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            configRepository.saveProvider(
                providerId = spec.id,
                displayName = spec.displayName,
                baseUrl = if (spec.isCustom) baseUrl else spec.baseUrl,
                chatModel = chatModel.ifBlank { spec.defaultChatModel },
                visionModel = if (spec.isVisionCapable) {
                    visionModel?.ifBlank { spec.defaultVisionModel }
                } else null,
                isVisionCapable = spec.isVisionCapable,
                apiKey = apiKey
            )
            aiServiceManager.reload()
            onDone()
        }
    }

    fun deleteProvider(providerId: String) {
        viewModelScope.launch {
            configRepository.deleteProvider(providerId)
            aiServiceManager.reload()
        }
    }

    // ---------- slot assignment ----------

    fun setChatSlot(providerId: String, model: String) {
        viewModelScope.launch {
            configRepository.setChatSlot(providerId, model)
            aiServiceManager.reload()
        }
    }

    fun setVisionSlot(providerId: String, model: String) {
        viewModelScope.launch {
            configRepository.setVisionSlot(providerId, model)
            aiServiceManager.reload()
        }
    }

    // ---------- connection test ----------

    fun testConnection(baseUrl: String, apiKey: String, model: String) {
        viewModelScope.launch {
            _testState.value = TestState.Testing
            when (val result = aiServiceManager.testConnection(baseUrl, apiKey, model)) {
                is Result.Success -> _testState.value = TestState.Success
                is Result.Error -> _testState.value = TestState.Failed(
                    result.exception.message ?: "连接失败，请检查 Key 和网络"
                )
            }
        }
    }

    fun resetTestState() {
        _testState.value = TestState.Idle
    }

    fun providerName(providerId: String?): String =
        ProviderCatalog.byId(providerId ?: "")?.displayName ?: providerId ?: "未配置"
}
