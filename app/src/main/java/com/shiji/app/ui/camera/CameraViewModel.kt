package com.shiji.app.ui.camera

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiji.app.ui.foodconfirm.EditableFoodItem
import com.shiji.app.ui.foodconfirm.guessMealType
import com.shiji.core.ai.api.AiException
import com.shiji.core.ai.manager.AiServiceManager
import com.shiji.core.ai.parser.ResponseParser
import com.shiji.core.camera.ImageProcessor
import com.shiji.core.common.result.Result
import com.shiji.core.data.entity.CachedFoodItemEntity
import com.shiji.core.data.entity.FoodRecordEntity
import com.shiji.core.data.repository.FoodRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val aiServiceManager: AiServiceManager,
    private val foodRepository: FoodRepositoryImpl
) : ViewModel() {

    private val imageProcessor = ImageProcessor()

    sealed interface AnalysisState {
        data object Idle : AnalysisState
        data object Analyzing : AnalysisState
        data class Success(val modelLabel: String) : AnalysisState
        data object NoFood : AnalysisState
        data class Failed(val message: String, val visionMissing: Boolean) : AnalysisState
    }

    data class CameraUiState(
        val photoUri: Uri? = null,
        val analysisState: AnalysisState = AnalysisState.Idle,
        val items: List<EditableFoodItem> = emptyList(),
        val selectedMealType: String = guessMealType(),
        val hasVisionModel: Boolean = true, // assume yes until checked; avoids flicker
        val saved: Boolean = false
    )

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    /** Set by the screen from NavGraph's current date — used for retroactive logging. */
    var selectedDate: LocalDate = LocalDate.now()

    private var analysisJob: Job? = null

    init {
        viewModelScope.launch {
            aiServiceManager.clients.collect { clients ->
                _uiState.update { it.copy(hasVisionModel = clients.hasVision) }
            }
        }
    }

    fun onPhotoReady(uri: Uri) {
        _uiState.update { it.copy(photoUri = uri, analysisState = AnalysisState.Idle) }
    }

    fun retake() {
        analysisJob?.cancel()
        _uiState.update {
            it.copy(photoUri = null, analysisState = AnalysisState.Idle, items = emptyList(), saved = false)
        }
    }

    fun setMealType(mealType: String) {
        _uiState.update { it.copy(selectedMealType = mealType) }
    }

    // ---------- analysis ----------

    fun analyze(context: Context) {
        val uri = _uiState.value.photoUri ?: return
        analysisJob?.cancel()
        _uiState.update { it.copy(analysisState = AnalysisState.Analyzing) }

        analysisJob = viewModelScope.launch {
            val processed = imageProcessor.process(context, uri).getOrElse { e ->
                _uiState.update {
                    it.copy(analysisState = AnalysisState.Failed("图片处理失败：${e.message}", visionMissing = false))
                }
                return@launch
            }

            when (val result = aiServiceManager.analyzeFoodImage(processed.bytes)) {
                is Result.Success -> {
                    val analysis = result.data
                    if (ResponseParser.hasNoFood(analysis)) {
                        _uiState.update { it.copy(analysisState = AnalysisState.NoFood, items = emptyList()) }
                    } else {
                        val modelLabel = aiServiceManager.clients.value.visionModel ?: "AI"
                        _uiState.update {
                            it.copy(
                                analysisState = AnalysisState.Success(modelLabel),
                                items = analysis.items.map(EditableFoodItem::from)
                            )
                        }
                    }
                }
                is Result.Error -> {
                    val e = result.exception
                    val visionMissing = e is AiException.NotConfigured || e is AiException.FeatureNotSupported
                    _uiState.update {
                        it.copy(
                            analysisState = AnalysisState.Failed(
                                message = (e as? AiException)?.message
                                    ?: result.message
                                    ?: "分析失败，请重试",
                                visionMissing = visionMissing
                            )
                        )
                    }
                }
            }
        }
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
        _uiState.update { it.copy(analysisState = AnalysisState.Idle) }
    }

    // Library add tracking
    private val _libraryAdded = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val libraryAdded: StateFlow<Map<String, Boolean>> = _libraryAdded.asStateFlow()

    fun addToLibrary(item: EditableFoodItem) {
        if (_libraryAdded.value[item.id] == true) return
        viewModelScope.launch {
            val factor = 100.0 / item.portionValue().coerceAtLeast(1.0)
            foodRepository.addToCache(CachedFoodItemEntity(
                name = item.displayName(),
                caloriesPer100g = item.caloriesValue() * factor,
                proteinPer100g = item.proteinGrams * factor,
                carbsPer100g = item.carbsGrams * factor,
                fatPer100g = item.fatGrams * factor,
                defaultPortion = item.portionValue(),
                defaultUnit = item.portionUnit))
            _libraryAdded.update { it + (item.id to true) }
        }
    }

    // ---------- result editing ----------

    fun updateItem(id: String, transform: (EditableFoodItem) -> EditableFoodItem) {
        _uiState.update { state ->
            state.copy(items = state.items.map { if (it.id == id) transform(it) else it })
        }
    }

    fun removeItem(id: String) {
        _uiState.update { state -> state.copy(items = state.items.filterNot { it.id == id }) }
    }

    // ---------- saving ----------

    fun save(context: Context) {
        val state = _uiState.value
        if (state.items.isEmpty() || state.saved) return

        viewModelScope.launch {
            val now = selectedDate.toString()
            val time = java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            val photoPath = state.photoUri?.let { persistPhoto(context, it) }
            val model = aiServiceManager.clients.value.visionModel

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
                        imageUri = photoPath,
                        source = "CAMERA",
                        aiModel = model
                    )
                )
            }
            _uiState.update { it.copy(saved = true) }
        }
    }

    /** Copy the captured/picked photo into private storage; returns the absolute path. */
    private suspend fun persistPhoto(context: Context, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.filesDir, "food_photos").apply { mkdirs() }
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val dest = File(dir, "food_$stamp.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: throw java.io.IOException("无法读取照片")
                dest.absolutePath
            }.getOrNull()
        }
}
