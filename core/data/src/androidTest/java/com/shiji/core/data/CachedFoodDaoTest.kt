package com.shiji.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shiji.core.data.dao.CachedFoodDao
import com.shiji.core.data.database.ShiJiDatabase
import com.shiji.core.data.entity.CachedFoodItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CachedFoodDaoTest {

    private lateinit var db: ShiJiDatabase
    private lateinit var dao: CachedFoodDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), ShiJiDatabase::class.java
        ).build()
        dao = db.cachedFoodDao()
    }

    @After fun teardown() { db.close() }

    @Test fun insertAndGetAll() = runBlocking {
        dao.upsert(CachedFoodItemEntity(name = "米饭", caloriesPer100g = 116.0))
        dao.upsert(CachedFoodItemEntity(name = "鸡蛋", caloriesPer100g = 144.0))
        val all = dao.getAll().first()
        assertEquals(2, all.size)
    }

    @Test fun searchMatchesName() = runBlocking {
        dao.upsert(CachedFoodItemEntity(name = "宫保鸡丁", caloriesPer100g = 200.0))
        dao.upsert(CachedFoodItemEntity(name = "鸡蛋炒饭", caloriesPer100g = 180.0))
        dao.upsert(CachedFoodItemEntity(name = "牛奶", caloriesPer100g = 65.0))
        val results = dao.search("鸡").first()
        assertEquals(2, results.size)
    }

    @Test fun incrementUseCount() = runBlocking {
        val id = dao.upsert(CachedFoodItemEntity(name = "测试食物", caloriesPer100g = 100.0))
        dao.incrementUseCount(id)
        val item = dao.getById(id)
        assertEquals(1, item?.useCount)
    }

    @Test fun deleteById() = runBlocking {
        val id = dao.upsert(CachedFoodItemEntity(name = "待删除", caloriesPer100g = 50.0))
        dao.deleteById(id)
        assertNull(dao.getById(id))
    }

    @Test fun upsertReplacesExisting() = runBlocking {
        val id = dao.upsert(CachedFoodItemEntity(name = "测试", caloriesPer100g = 100.0))
        dao.upsert(CachedFoodItemEntity(id = id, name = "测试更新", caloriesPer100g = 120.0))
        val item = dao.getById(id)
        assertEquals("测试更新", item?.name)
        assertEquals(120.0, item?.caloriesPer100g ?: 0.0, 0.01)
    }
}
