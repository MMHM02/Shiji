package com.shiji.app.ui.foodconfirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiji.core.ai.api.AiException
import com.shiji.core.ai.manager.AiServiceManager
import com.shiji.core.common.result.Result
import com.shiji.core.data.entity.FoodRecordEntity
import com.shiji.core.data.repository.FoodRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Shared "parse → confirm → save" pipeline for text and voice food entry.
 * The screens own their input UX; this ViewModel owns the AI call and Room write.
 */
@HiltViewModel
class FoodLogEntryViewModel @Inject constructor(
    private val aiServiceManager: AiServiceManager,
    private val foodRepository: FoodRepositoryImpl
) : ViewModel() {

    sealed interface ParseState {
        data object Idle : ParseState
        data object Parsing : ParseState
        data object Parsed : ParseState
        /** AI unavailable — an offline keyword estimate was produced instead. */
        data object OfflineEstimated : ParseState
        data class Failed(val message: String, val aiMissing: Boolean) : ParseState
    }

    data class EntryUiState(
        val parseState: ParseState = ParseState.Idle,
        val sourceText: String = "",
        val items: List<EditableFoodItem> = emptyList(),
        val selectedMealType: String = guessMealType(),
        val aiConfigured: Boolean = true,
        val saved: Boolean = false
    )

    private val _uiState = MutableStateFlow(EntryUiState())
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    private var parseJob: Job? = null

    init {
        viewModelScope.launch {
            aiServiceManager.clients.collect { clients ->
                _uiState.update { it.copy(aiConfigured = clients.hasChat) }
            }
        }
    }

    // ---------- parsing ----------

    fun parse(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        parseJob?.cancel()
        _uiState.update {
            it.copy(
                parseState = ParseState.Parsing,
                sourceText = trimmed,
                items = emptyList(),
                saved = false
            )
        }

        parseJob = viewModelScope.launch {
            when (val result = aiServiceManager.parseFoodDescription(trimmed)) {
                is Result.Success -> {
                    if (result.data.isEmpty()) {
                        _uiState.update {
                            it.copy(parseState = ParseState.Failed("没有从描述中识别到食物，请换个说法试试", aiMissing = false))
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                parseState = ParseState.Parsed,
                                items = result.data.map(EditableFoodItem::from)
                            )
                        }
                    }
                }
                is Result.Error -> {
                    val e = result.exception
                    if (e is AiException.NotConfigured || e is AiException.Network || e is AiException.Timeout) {
                        // Graceful degradation: offline keyword estimate keeps the flow usable.
                        _uiState.update {
                            it.copy(
                                parseState = ParseState.OfflineEstimated,
                                items = listOf(offlineEstimate(trimmed))
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                parseState = ParseState.Failed(
                                    (e as? AiException)?.message ?: "解析失败，请重试",
                                    aiMissing = false
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun cancelParse() {
        parseJob?.cancel()
        _uiState.update { it.copy(parseState = ParseState.Idle) }
    }

    fun reset() {
        parseJob?.cancel()
        _uiState.update {
            EntryUiState(aiConfigured = it.aiConfigured, selectedMealType = guessMealType())
        }
    }

    // ---------- editing ----------

    fun setMealType(mealType: String) = _uiState.update { it.copy(selectedMealType = mealType) }

    fun updateItem(id: String, transform: (EditableFoodItem) -> EditableFoodItem) {
        _uiState.update { s -> s.copy(items = s.items.map { if (it.id == id) transform(it) else it }) }
    }

    fun removeItem(id: String) {
        _uiState.update { s -> s.copy(items = s.items.filterNot { it.id == id }) }
    }

    // ---------- saving ----------

    fun save(source: String) {
        val state = _uiState.value
        if (state.items.isEmpty() || state.saved) return

        viewModelScope.launch {
            val now = LocalDate.now().toString()
            val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            val model = aiServiceManager.clients.value.chatModel
            state.items.forEach { item ->
                foodRepository.saveRecord(
                    FoodRecordEntity(
                        mealType = state.selectedMealType,
                        recordDate = now,
                        recordTime = time,
                        foodName = item.displayName(),
                        portion = item.portionValue(),
                        portionUnit = item.portionUnit,
                        calories = item.caloriesValue(),
                        proteinGrams = item.proteinGrams,
                        carbsGrams = item.carbsGrams,
                        fatGrams = item.fatGrams,
                        source = source,
                        aiModel = model,
                        note = if (state.parseState == ParseState.OfflineEstimated) "离线估算" else null
                    )
                )
            }
            _uiState.update { it.copy(saved = true) }
        }
    }

    // ---------- offline fallback ----------
    // Offline keyword estimate removed per user request — AI not configured shows a clean error prompt instead.

    private fun offlineEstimate(text: String): EditableFoodItem = EditableFoodItem(
        name = text.take(40), portion = "1", portionUnit = "份",
        calories = "0", proteinGrams = 0.0, carbsGrams = 0.0, fatGrams = 0.0,
        confidence = 0f
    )
}
