package com.shiji.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shiji.app.ui.home.HomeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `home screen shows today label`() {
        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                HomeScreen()
            }
        }
        composeTestRule.onNodeWithText("今天").assertExists()
    }

    @Test
    fun `home screen shows quick entry buttons`() {
        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                HomeScreen()
            }
        }
        composeTestRule.onNodeWithText("拍照识食").assertExists()
        composeTestRule.onNodeWithText("语音记录").assertExists()
        composeTestRule.onNodeWithText("手动记录").assertExists()
    }

    @Test
    fun `home screen shows today diet section`() {
        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                HomeScreen()
            }
        }
        composeTestRule.onNodeWithText("今日饮食").assertExists()
        composeTestRule.onNodeWithText("查看全部 →").assertExists()
    }

    @Test
    fun `home screen shows meal groups`() {
        val records = listOf(
            com.shiji.core.data.entity.FoodRecordEntity(
                id = 1, mealType = "LUNCH", recordDate = "2026-07-25", recordTime = "12:00",
                foodName = "宫保鸡丁盖饭", portion = 1.0, portionUnit = "份",
                calories = 650.0, proteinGrams = 25.0, carbsGrams = 70.0, fatGrams = 20.0,
                source = "CAMERA"
            ),
            com.shiji.core.data.entity.FoodRecordEntity(
                id = 2, mealType = "LUNCH", recordDate = "2026-07-25", recordTime = "12:05",
                foodName = "紫菜汤", portion = 1.0, portionUnit = "碗",
                calories = 45.0, proteinGrams = 3.0, carbsGrams = 5.0, fatGrams = 1.0,
                source = "CAMERA"
            )
        )
        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                HomeScreen(todayRecords = records)
            }
        }
        composeTestRule.onNodeWithText("宫保鸡丁盖饭").assertExists()
        composeTestRule.onNodeWithText("紫菜汤").assertExists()
    }
}
