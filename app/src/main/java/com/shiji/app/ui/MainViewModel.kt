package com.shiji.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiji.core.data.dao.UserGoalDao
import com.shiji.core.data.datastore.UserPreferences
import com.shiji.core.data.entity.CachedFoodItemEntity
import com.shiji.core.data.entity.FoodRecordEntity
import com.shiji.core.data.entity.UserGoalEntity
import com.shiji.core.data.repository.FoodRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * App-level data hub for the NavGraph.
 * Screens stay stateless; this ViewModel exposes Room/DataStore as hot StateFlows.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val foodRepository: FoodRepositoryImpl,
    private val userGoalDao: UserGoalDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    // ---------- date selection ----------

    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    fun setDate(date: LocalDate) {
        _currentDate.value = date
    }

    // ---------- food records (Room is the single source of truth) ----------

    val currentDateRecords: StateFlow<List<FoodRecordEntity>> = _currentDate
        .flatMapLatest { foodRepository.getRecordsByDate(it.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Last 7 days including today, for trends and AI suggestions. */
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
                // Grow the personal food library organically.
                if (record.portion > 0 && cachedFoods.value.none { it.name == record.foodName }) {
                    val factor = 100.0 / record.portion
                    foodRepository.addToCache(
                        CachedFoodItemEntity(
                            name = record.foodName,
                            caloriesPer100g = record.calories * factor,
                            proteinPer100g = record.proteinGrams * factor,
                            carbsPer100g = record.carbsGrams * factor,
                            fatPer100g = record.fatGrams * factor,
                            defaultPortion = record.portion,
                            defaultUnit = record.portionUnit
                        )
                    )
                }
            }
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch { foodRepository.deleteRecord(id) }
    }

    fun addCachedFood(food: CachedFoodItemEntity) {
        viewModelScope.launch { foodRepository.addToCache(food) }
    }

    fun deleteCachedFood(id: Long) {
        viewModelScope.launch { foodRepository.deleteCachedFood(id) }
    }

    // ---------- user goal ----------

    val userGoal: StateFlow<UserGoalEntity?> = userGoalDao.getGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveGoal(goal: UserGoalEntity) {
        viewModelScope.launch { userGoalDao.upsert(goal) }
    }

    // ---------- preferences (theme / profile / onboarding) ----------

    val isDarkTheme: StateFlow<Boolean?> = userPreferences.isDarkTheme
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val onboardingDone: StateFlow<Boolean?> = userPreferences.isOnboardingDone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userName: StateFlow<String> = userPreferences.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Shawn")

    val userAvatar: StateFlow<String> = userPreferences.userAvatar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "👤")

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setDarkTheme(enabled) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { userPreferences.setOnboardingDone() }
    }

    fun updateProfile(name: String, avatar: String) {
        viewModelScope.launch {
            userPreferences.setUserName(name)
            userPreferences.setUserAvatar(avatar)
        }
    }
}
