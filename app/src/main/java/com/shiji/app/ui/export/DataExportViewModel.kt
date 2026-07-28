package com.shiji.app.ui.export

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiji.core.data.dao.AiProviderDao
import com.shiji.core.data.dao.FoodRecordDao
import com.shiji.core.data.dao.HealthMetricDao
import com.shiji.core.data.dao.UserGoalDao
import com.shiji.core.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class DataExportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val foodRecordDao: FoodRecordDao,
    private val healthMetricDao: HealthMetricDao,
    private val userGoalDao: UserGoalDao,
    private val cachedFoodDao: com.shiji.core.data.dao.CachedFoodDao,
    private val aiProviderDao: AiProviderDao
) : ViewModel() {

    private val exportManager = DataExportManager()

    sealed interface UiState {
        data object Idle : UiState
        data object Exporting : UiState
        data class ExportDone(val filePath: String, val recordCount: Int) : UiState
        data object Importing : UiState
        data class ImportDone(val recordCount: Int) : UiState
        data class Error(val message: String) : UiState
    }

    data class ScreenState(
        val uiState: UiState = UiState.Idle,
        val backupFolder: String = "",
        val existingBackup: File? = null
    )

    private val _state = MutableStateFlow(ScreenState(
        backupFolder = exportManager.backupDir(context).absolutePath
    ))
    val state: StateFlow<ScreenState> = _state.asStateFlow()

    init {
        viewModelScope.launch { refreshBackupList() }
    }

    fun refreshBackupList() {
        val files = exportManager.listBackupFiles(context)
        _state.value = _state.value.copy(existingBackup = files.firstOrNull())
    }

    fun exportData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(uiState = UiState.Exporting)
            try {
                val foods = foodRecordDao.getRecordsByDateRange("2000-01-01", "2099-12-31").first()
                val metrics = healthMetricDao.getByType("WEIGHT").first() +
                        healthMetricDao.getByType("WATER").first()
                val goal = userGoalDao.getGoal().first()
                val cached = cachedFoodDao.getAll().first()
                val providers = aiProviderDao.getAll().first()

                val data = FitnessExportData(
                    version = 1,
                    exportDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    userProfile = goal?.let {
                        UserProfileData(it.heightCm ?: 0.0, it.currentWeightKg ?: 0.0, it.goalType,
                            it.dailyCalories, it.proteinTargetGrams, it.carbsTargetGrams, it.fatTargetGrams)
                    },
                    foodRecords = foods.map { FoodRecordExportItem(
                        it.mealType, it.recordDate, it.recordTime, it.foodName,
                        it.portion, it.portionUnit, it.calories, it.proteinGrams,
                        it.carbsGrams, it.fatGrams, it.source, it.note) },
                    healthMetrics = metrics.map { HealthMetricExportItem(
                        it.metricType, it.value, it.unit, it.recordDate, it.recordTime, "") },
                    cachedFoods = cached.map { CachedFoodExportItem(
                        it.name, it.caloriesPer100g, it.proteinPer100g, it.carbsPer100g, it.fatPer100g) },
                    aiProviders = providers.map { AiProviderExportItem(
                        it.id, it.displayName, it.baseUrl, it.isEnabled,
                        it.defaultVisionModel, it.defaultChatModel) }
                )

                val file = exportManager.exportToFile(context, data)
                if (file != null) {
                    _state.value = _state.value.copy(
                        uiState = UiState.ExportDone(file.absolutePath, foods.size),
                        existingBackup = file
                    )
                } else {
                    _state.value = _state.value.copy(uiState = UiState.Error("写入文件失败"))
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    uiState = UiState.Error("导出失败: ${e.message?.take(80)}"))
            }
        }
    }

    fun importData() {
        viewModelScope.launch {
            val backup = _state.value.existingBackup
            if (backup == null) {
                _state.value = _state.value.copy(
                    uiState = UiState.Error("未找到 .fitness 文件\n请将备份文件放到：\n${_state.value.backupFolder}"))
                return@launch
            }
            _state.value = _state.value.copy(uiState = UiState.Importing)
            try {
                val data = exportManager.readBackupFile(backup)
                // Restore food records
                data.foodRecords.forEach { item ->
                    foodRecordDao.insert(com.shiji.core.data.entity.FoodRecordEntity(
                        mealType = item.mealType, recordDate = item.recordDate,
                        recordTime = item.recordTime, foodName = item.foodName,
                        portion = item.portion, portionUnit = item.portionUnit,
                        calories = item.calories, proteinGrams = item.proteinGrams,
                        carbsGrams = item.carbsGrams, fatGrams = item.fatGrams,
                        source = item.source, note = item.note))
                }
                // Restore health metrics
                data.healthMetrics.forEach { item ->
                    healthMetricDao.insert(com.shiji.core.data.entity.HealthMetricEntity(
                        metricType = item.metricType, value = item.value, unit = item.unit,
                        recordDate = item.recordDate, recordTime = item.recordTime))
                }
                // Restore user goal
                data.userProfile?.let { profile ->
                    userGoalDao.upsert(com.shiji.core.data.entity.UserGoalEntity(
                        heightCm = profile.heightCm, currentWeightKg = profile.weightKg,
                        goalType = profile.goalType, dailyCalories = profile.dailyCalories,
                        proteinTargetGrams = profile.proteinTarget, carbsTargetGrams = profile.carbsTarget,
                        fatTargetGrams = profile.fatTarget, updatedAt = System.currentTimeMillis()))
                }
                // Restore cached foods
                data.cachedFoods.forEach { item ->
                    cachedFoodDao.upsert(com.shiji.core.data.entity.CachedFoodItemEntity(
                        name = item.name, caloriesPer100g = item.caloriesPer100g,
                        proteinPer100g = item.proteinPer100g, carbsPer100g = item.carbsPer100g,
                        fatPer100g = item.fatPer100g))
                }
                _state.value = _state.value.copy(
                    uiState = UiState.ImportDone(data.foodRecords.size))
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    uiState = UiState.Error("导入失败，文件可能损坏: ${e.message?.take(80)}"))
            }
        }
    }
}
