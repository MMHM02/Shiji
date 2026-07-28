package com.shiji.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_THEME_DARK = booleanPreferencesKey("theme_dark")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_API_KEY_CONFIGURED = booleanPreferencesKey("api_key_configured")
        val KEY_VISION_PROVIDER_ID = stringPreferencesKey("vision_provider_id")
        val KEY_CHAT_PROVIDER_ID = stringPreferencesKey("chat_provider_id")
        val KEY_VISION_MODEL = stringPreferencesKey("vision_model")
        val KEY_CHAT_MODEL = stringPreferencesKey("chat_model")
        val KEY_DAILY_CALORIES_TARGET = intPreferencesKey("daily_calories_target")
        val KEY_GOAL_TYPE = stringPreferencesKey("goal_type")
        val KEY_LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_AVATAR = stringPreferencesKey("user_avatar")
        val KEY_WATER_GOAL = intPreferencesKey("water_goal")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[KEY_THEME_DARK] ?: false }
    val isOnboardingDone: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }
    val isApiKeyConfigured: Flow<Boolean> = context.dataStore.data.map { it[KEY_API_KEY_CONFIGURED] ?: false }
    val visionProviderId: Flow<String> = context.dataStore.data.map { it[KEY_VISION_PROVIDER_ID] ?: "" }
    val chatProviderId: Flow<String> = context.dataStore.data.map { it[KEY_CHAT_PROVIDER_ID] ?: "" }
    val visionModel: Flow<String> = context.dataStore.data.map { it[KEY_VISION_MODEL] ?: "" }
    val chatModel: Flow<String> = context.dataStore.data.map { it[KEY_CHAT_MODEL] ?: "" }
    val dailyCaloriesTarget: Flow<Int> = context.dataStore.data.map { it[KEY_DAILY_CALORIES_TARGET] ?: 2000 }
    val goalType: Flow<String> = context.dataStore.data.map { it[KEY_GOAL_TYPE] ?: "MAINTAIN" }
    val userName: Flow<String> = context.dataStore.data.map { it[KEY_USER_NAME] ?: "Shawn" }
    val userAvatar: Flow<String> = context.dataStore.data.map { it[KEY_USER_AVATAR] ?: "👤" }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[KEY_THEME_DARK] = enabled }
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    suspend fun setApiKeyConfigured(configured: Boolean) {
        context.dataStore.edit { it[KEY_API_KEY_CONFIGURED] = configured }
    }

    suspend fun setVisionProviderId(id: String) {
        context.dataStore.edit { it[KEY_VISION_PROVIDER_ID] = id }
    }

    suspend fun setChatProviderId(id: String) {
        context.dataStore.edit { it[KEY_CHAT_PROVIDER_ID] = id }
    }

    suspend fun setVisionModel(model: String) {
        context.dataStore.edit { it[KEY_VISION_MODEL] = model }
    }

    suspend fun setChatModel(model: String) {
        context.dataStore.edit { it[KEY_CHAT_MODEL] = model }
    }

    suspend fun setDailyCaloriesTarget(calories: Int) {
        context.dataStore.edit { it[KEY_DAILY_CALORIES_TARGET] = calories }
    }

    suspend fun setGoalType(type: String) {
        context.dataStore.edit { it[KEY_GOAL_TYPE] = type }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[KEY_USER_NAME] = name }
    }

    suspend fun setUserAvatar(avatar: String) {
        context.dataStore.edit { it[KEY_USER_AVATAR] = avatar }
    }

    val waterGoal: Flow<Int> = context.dataStore.data.map { it[KEY_WATER_GOAL] ?: 2000 }

    suspend fun setWaterGoal(ml: Int) {
        context.dataStore.edit { it[KEY_WATER_GOAL] = ml }
    }
}
