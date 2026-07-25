package com.shiji.core.ai.usage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.YearMonth

private val Context.aiUsageStore: DataStore<Preferences> by preferencesDataStore(name = "ai_usage")

/**
 * Local AI usage tracker — call counts + token consumption per provider/model/day.
 * Everything stays on-device; used for the monthly usage summary in settings.
 */
class AiUsageTracker(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    enum class Feature { CHAT, FOOD_PHOTO, FOOD_PARSE, INSIGHT, CONNECTION_TEST }

    @Serializable
    data class DailyUsage(
        val date: String,
        val providerId: String,
        val model: String,
        val callCount: Int = 0,
        val inputTokens: Long = 0,
        val outputTokens: Long = 0,
        val successCount: Int = 0,
        val failCount: Int = 0,
        val byFeature: Map<String, Int> = emptyMap()
    )

    data class UsageSummary(
        val totalCalls: Int,
        val successCalls: Int,
        val inputTokens: Long,
        val outputTokens: Long,
        val byProvider: Map<String, Int>
    )

    suspend fun record(
        providerId: String,
        model: String,
        feature: Feature,
        inputTokens: Int,
        outputTokens: Int,
        success: Boolean
    ) {
        val date = LocalDate.now().toString()
        val key = stringPreferencesKey("$date|$providerId|$model")
        context.aiUsageStore.edit { prefs ->
            val current = prefs[key]?.let { runCatching { json.decodeFromString<DailyUsage>(it) }.getOrNull() }
                ?: DailyUsage(date = date, providerId = providerId, model = model)
            val updated = current.copy(
                callCount = current.callCount + 1,
                inputTokens = current.inputTokens + inputTokens,
                outputTokens = current.outputTokens + outputTokens,
                successCount = current.successCount + if (success) 1 else 0,
                failCount = current.failCount + if (!success) 1 else 0,
                byFeature = current.byFeature.toMutableMap().apply {
                    put(feature.name, (get(feature.name) ?: 0) + 1)
                }
            )
            prefs[key] = json.encodeToString(DailyUsage.serializer(), updated)
        }
    }

    /** This month's aggregate, exposed as a Flow so the settings UI stays live. */
    val monthlySummary: Flow<UsageSummary> = context.aiUsageStore.data.map { prefs ->
        val month = YearMonth.now()
        var calls = 0
        var success = 0
        var input = 0L
        var output = 0L
        val byProvider = mutableMapOf<String, Int>()
        prefs.asMap().forEach { (_, value) ->
            val usage = (value as? String)?.let {
                runCatching { json.decodeFromString<DailyUsage>(it) }.getOrNull()
            } ?: return@forEach
            val date = runCatching { LocalDate.parse(usage.date) }.getOrNull() ?: return@forEach
            if (YearMonth.from(date) == month) {
                calls += usage.callCount
                success += usage.successCount
                input += usage.inputTokens
                output += usage.outputTokens
                byProvider[usage.providerId] = (byProvider[usage.providerId] ?: 0) + usage.callCount
            }
        }
        UsageSummary(calls, success, input, output, byProvider)
    }

    suspend fun getMonthlySummaryOnce(): UsageSummary = monthlySummary.first()
}
