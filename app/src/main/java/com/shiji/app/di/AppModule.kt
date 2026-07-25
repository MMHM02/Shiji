package com.shiji.app.di

import android.content.Context
import androidx.room.Room
import com.shiji.core.ai.config.AiConfigRepository
import com.shiji.core.ai.manager.AiServiceManager
import com.shiji.core.ai.usage.AiUsageTracker
import com.shiji.core.common.permission.PermissionManager
import com.shiji.core.common.security.EncryptedKeyStore
import com.shiji.core.data.dao.AiProviderDao
import com.shiji.core.data.dao.CachedFoodDao
import com.shiji.core.data.dao.FoodRecordDao
import com.shiji.core.data.dao.HealthMetricDao
import com.shiji.core.data.dao.UserGoalDao
import com.shiji.core.data.database.ShiJiDatabase
import com.shiji.core.data.datastore.UserPreferences
import com.shiji.core.data.repository.FoodRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ========== Coroutines ==========

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ========== Database ==========

    @Provides
    @Singleton
    fun provideShiJiDatabase(@ApplicationContext context: Context): ShiJiDatabase {
        return Room.databaseBuilder(context, ShiJiDatabase::class.java, "shiji.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideFoodRecordDao(db: ShiJiDatabase): FoodRecordDao = db.foodRecordDao()
    @Provides fun provideCachedFoodDao(db: ShiJiDatabase): CachedFoodDao = db.cachedFoodDao()
    @Provides fun provideHealthMetricDao(db: ShiJiDatabase): HealthMetricDao = db.healthMetricDao()
    @Provides fun provideUserGoalDao(db: ShiJiDatabase): UserGoalDao = db.userGoalDao()
    @Provides fun provideAiProviderDao(db: ShiJiDatabase): AiProviderDao = db.aiProviderDao()

    // ========== Repository ==========

    @Provides
    @Singleton
    fun provideFoodRepository(foodRecordDao: FoodRecordDao, cachedFoodDao: CachedFoodDao): FoodRepositoryImpl {
        return FoodRepositoryImpl(foodRecordDao, cachedFoodDao)
    }

    // ========== DataStore ==========

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }

    // ========== Security ==========

    @Provides
    @Singleton
    fun provideEncryptedKeyStore(@ApplicationContext context: Context): EncryptedKeyStore {
        return EncryptedKeyStore(context)
    }

    // ========== Network ==========

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            // NOTE: never add a BODY-level logging interceptor here —
            // request headers carry the user's API key and must stay out of logs.
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.NONE })
            .build()
    }

    // ========== AI ==========

    @Provides
    @Singleton
    fun provideAiUsageTracker(@ApplicationContext context: Context): AiUsageTracker {
        return AiUsageTracker(context)
    }

    @Provides
    @Singleton
    fun provideAiConfigRepository(
        aiProviderDao: AiProviderDao,
        encryptedKeyStore: EncryptedKeyStore,
        userPreferences: UserPreferences
    ): AiConfigRepository {
        return AiConfigRepository(aiProviderDao, encryptedKeyStore, userPreferences)
    }

    @Provides
    @Singleton
    fun provideAiServiceManager(
        configRepository: AiConfigRepository,
        usageTracker: AiUsageTracker,
        okHttpClient: OkHttpClient,
        scope: CoroutineScope
    ): AiServiceManager {
        return AiServiceManager(configRepository, usageTracker, okHttpClient, scope)
    }

    // ========== Permissions ==========

    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext context: Context): PermissionManager {
        return PermissionManager(context)
    }
}
