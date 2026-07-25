package com.shiji.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shiji.core.data.dao.FoodRecordDao
import com.shiji.core.data.database.ShiJiDatabase
import com.shiji.core.data.entity.FoodRecordEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoodRecordDaoTest {

    private lateinit var db: ShiJiDatabase
    private lateinit var dao: FoodRecordDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ShiJiDatabase::class.java
        ).build()
        dao = db.foodRecordDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndGetByDate() = runBlocking {
        val record = FoodRecordEntity(
            mealType = "BREAKFAST",
            recordDate = "2026-07-14",
            recordTime = "08:30",
            foodName = "牛奶面包",
            portion = 200.0,
            portionUnit = "g",
            calories = 350.0,
            proteinGrams = 12.0,
            carbsGrams = 45.0,
            fatGrams = 8.0,
            source = "MANUAL"
        )
        dao.insert(record)
        val records = dao.getRecordsByDate("2026-07-14").first()
        assertEquals(1, records.size)
        assertEquals("牛奶面包", records[0].foodName)
        assertEquals(350.0, records[0].calories, 0.01)
    }

    @Test
    fun deleteById() = runBlocking {
        val id = dao.insert(FoodRecordEntity(
            mealType = "LUNCH", recordDate = "2026-07-14", recordTime = "12:00",
            foodName = "Test", portion = 100.0, portionUnit = "g",
            calories = 100.0, proteinGrams = 5.0, carbsGrams = 10.0, fatGrams = 3.0,
            source = "MANUAL"
        ))
        dao.deleteById(id)
        val records = dao.getRecordsByDate("2026-07-14").first()
        assertTrue(records.isEmpty())
    }

    @Test
    fun dailyCaloriesSum() = runBlocking {
        dao.insert(FoodRecordEntity(
            mealType = "BREAKFAST", recordDate = "2026-07-14", recordTime = "08:00",
            foodName = "A", portion = 100.0, portionUnit = "g",
            calories = 300.0, proteinGrams = 10.0, carbsGrams = 20.0, fatGrams = 5.0,
            source = "MANUAL"
        ))
        dao.insert(FoodRecordEntity(
            mealType = "LUNCH", recordDate = "2026-07-14", recordTime = "12:00",
            foodName = "B", portion = 200.0, portionUnit = "g",
            calories = 500.0, proteinGrams = 20.0, carbsGrams = 40.0, fatGrams = 10.0,
            source = "MANUAL"
        ))
        val total = dao.getDailyCalories("2026-07-14").first()
        assertEquals(800.0, total ?: 0.0, 0.01)
    }

    @Test
    fun getByMealType() = runBlocking {
        dao.insert(FoodRecordEntity(
            mealType = "BREAKFAST", recordDate = "2026-07-14", recordTime = "08:00",
            foodName = "早餐", portion = 100.0, portionUnit = "g",
            calories = 200.0, proteinGrams = 5.0, carbsGrams = 10.0, fatGrams = 3.0,
            source = "MANUAL"
        ))
        dao.insert(FoodRecordEntity(
            mealType = "LUNCH", recordDate = "2026-07-14", recordTime = "12:00",
            foodName = "午餐", portion = 200.0, portionUnit = "g",
            calories = 500.0, proteinGrams = 20.0, carbsGrams = 40.0, fatGrams = 10.0,
            source = "MANUAL"
        ))
        val breakfast = dao.getRecordsByDateAndMeal("2026-07-14", "BREAKFAST").first()
        assertEquals(1, breakfast.size)
        assertEquals("早餐", breakfast[0].foodName)
    }
}
