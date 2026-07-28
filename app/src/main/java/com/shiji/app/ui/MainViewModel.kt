package com.shiji.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiji.core.ai.usage.AiUsageTracker
import com.shiji.core.data.dao.HealthMetricDao
import com.shiji.core.data.dao.UserGoalDao
import com.shiji.core.data.datastore.UserPreferences
import com.shiji.core.data.entity.CachedFoodItemEntity
import com.shiji.core.data.entity.FoodRecordEntity
import com.shiji.core.data.entity.HealthMetricEntity
import com.shiji.core.data.entity.UserGoalEntity
import com.shiji.core.data.repository.FoodRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val foodRepository: FoodRepositoryImpl,
    private val userGoalDao: UserGoalDao,
    private val healthMetricDao: HealthMetricDao,
    private val userPreferences: UserPreferences,
    private val aiUsageTracker: AiUsageTracker
) : ViewModel() {

    // ---------- date ----------
    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()
    fun setDate(date: LocalDate) { _currentDate.value = date }

    // ---------- food ----------
    val currentDateRecords: StateFlow<List<FoodRecordEntity>> = _currentDate
        .flatMapLatest { foodRepository.getRecordsByDate(it.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weekRecords: StateFlow<List<FoodRecordEntity>> =
        foodRepository.getRecordsByDateRange(
            LocalDate.now().minusDays(6).toString(), LocalDate.now().toString()
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cachedFoods: StateFlow<List<CachedFoodItemEntity>> =
        foodRepository.getAllCachedFoods()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveRecords(records: List<FoodRecordEntity>) {
        if (records.isEmpty()) return
        viewModelScope.launch {
            records.forEach { record ->
                foodRepository.saveRecord(record)
                if (record.portion > 0 && cachedFoods.value.none { it.name == record.foodName }) {
                    val factor = 100.0 / record.portion
                    foodRepository.addToCache(CachedFoodItemEntity(
                        name = record.foodName,
                        caloriesPer100g = record.calories * factor,
                        proteinPer100g = record.proteinGrams * factor,
                        carbsPer100g = record.carbsGrams * factor,
                        fatPer100g = record.fatGrams * factor,
                        defaultPortion = record.portion,
                        defaultUnit = record.portionUnit
                    ))
                }
            }
        }
    }

    fun deleteRecord(id: Long) = viewModelScope.launch { foodRepository.deleteRecord(id) }
    fun addCachedFood(food: CachedFoodItemEntity) = viewModelScope.launch { foodRepository.addToCache(food) }
    fun deleteCachedFood(id: Long) = viewModelScope.launch { foodRepository.deleteCachedFood(id) }

    // ---------- goals ----------
    val userGoal: StateFlow<UserGoalEntity?> = userGoalDao.getGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    fun saveGoal(goal: UserGoalEntity) = viewModelScope.launch { userGoalDao.upsert(goal) }

    // ---------- water (daily total, persisted, with customizable goal) ----------
    private val _waterMl = MutableStateFlow(0)
    val waterMl: StateFlow<Int> = _waterMl.asStateFlow()

    private val _waterGoal = MutableStateFlow(2000)
    val waterGoal: StateFlow<Int> = _waterGoal.asStateFlow()

    init {
        // Restore today's water from Room + goal from prefs
        viewModelScope.launch {
            _waterGoal.value = userPreferences.waterGoal.first()
            val todayEntity = healthMetricDao.getByTypeAndDate("WATER", _currentDate.value.toString())
            _waterMl.value = todayEntity?.value?.toInt() ?: 0
        }
        // Reload water when date changes
        viewModelScope.launch {
            _currentDate.collect { date ->
                val entity = healthMetricDao.getByTypeAndDate("WATER", date.toString())
                _waterMl.value = entity?.value?.toInt() ?: 0
            }
        }
    }

    fun addWater(ml: Int) {
        viewModelScope.launch {
            val today = _currentDate.value.toString()
            val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            val current = healthMetricDao.getByTypeAndDate("WATER", today)
            val newTotal = (current?.value?.toInt() ?: 0) + ml
            healthMetricDao.insert(HealthMetricEntity(
                id = current?.id ?: 0,
                metricType = "WATER", value = newTotal.toDouble(), unit = "ml",
                recordDate = today, recordTime = time
            ))
            _waterMl.value = newTotal
        }
    }

    fun setWaterGoal(ml: Int) {
        viewModelScope.launch {
            userPreferences.setWaterGoal(ml)
            _waterGoal.value = ml
        }
    }

    // ---------- weight ----------
    fun saveWeight(kg: Double) {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            healthMetricDao.insert(HealthMetricEntity(
                metricType = "WEIGHT", value = kg, unit = "kg",
                recordDate = today, recordTime = time
            ))
            val goal = userGoalDao.getGoal().first() ?: UserGoalEntity()
            userGoalDao.upsert(goal.copy(currentWeightKg = kg, updatedAt = System.currentTimeMillis()))
        }
    }

    fun weightHistoryFlow(startDate: String, endDate: String): Flow<List<HealthMetricEntity>> =
        healthMetricDao.getByTypeAndDateRange("WEIGHT", startDate, endDate)

    val latestWeight: StateFlow<Double?> = healthMetricDao.getByType("WEIGHT")
        .map { it.firstOrNull()?.value }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ---------- preferences ----------
    val isDarkTheme: StateFlow<Boolean?> = userPreferences.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val onboardingDone: StateFlow<Boolean?> = userPreferences.isOnboardingDone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val userName: StateFlow<String> = userPreferences.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Shawn")
    val userAvatar: StateFlow<String> = userPreferences.userAvatar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "👤")

    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch { userPreferences.setDarkTheme(enabled) }
    fun completeOnboarding() = viewModelScope.launch { userPreferences.setOnboardingDone() }
    fun updateProfile(name: String, avatar: String) = viewModelScope.launch {
        userPreferences.setUserName(name)
        userPreferences.setUserAvatar(avatar)
    }

    // ---------- AI usage ----------
    val aiUsageSummary: StateFlow<AiUsageTracker.UsageSummary> = aiUsageTracker.monthlySummary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            AiUsageTracker.UsageSummary(0, 0, 0, 0, emptyMap()))

    // ---------- calendar: dates with records (last 60 days) ----------
    val last60DaysWithRecords: StateFlow<Set<LocalDate>> = _currentDate.flatMapLatest { today ->
        foodRepository.getDistinctDatesWithRecords(
            today.minusDays(59).toString(), today.toString()
        )
    }.map { list -> list.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet() }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
}
