package com.shiji.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shiji.core.data.dao.UserGoalDao
import com.shiji.core.data.database.ShiJiDatabase
import com.shiji.core.data.entity.UserGoalEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserGoalDaoTest {

    private lateinit var db: ShiJiDatabase
    private lateinit var dao: UserGoalDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), ShiJiDatabase::class.java
        ).build()
        dao = db.userGoalDao()
    }

    @After fun teardown() { db.close() }

    @Test fun `goal defaults to null when empty`() = runBlocking {
        val goal = dao.getGoal().first()
        assertNull(goal)
    }

    @Test fun `upsert and read goal`() = runBlocking {
        dao.upsert(UserGoalEntity(
            dailyCalories = 1800.0, proteinTargetGrams = 120.0,
            carbsTargetGrams = 200.0, fatTargetGrams = 60.0,
            heightCm = 175.0, currentWeightKg = 72.0, goalType = "LOSE_SLOW"
        ))
        val goal = dao.getGoal().first()
        assertNotNull(goal)
        assertEquals(1800.0, goal!!.dailyCalories, 0.01)
        assertEquals("LOSE_SLOW", goal.goalType)
    }

    @Test fun `upsert replaces existing singleton`() = runBlocking {
        dao.upsert(UserGoalEntity(dailyCalories = 2000.0, goalType = "MAINTAIN"))
        dao.upsert(UserGoalEntity(dailyCalories = 1800.0, goalType = "LOSE_SLOW"))
        val goal = dao.getGoal().first()
        assertEquals(1800.0, goal!!.dailyCalories, 0.01)
    }

    @Test fun `delete removes goal`() = runBlocking {
        dao.upsert(UserGoalEntity(dailyCalories = 1800.0))
        dao.delete()
        assertNull(dao.getGoal().first())
    }
}
