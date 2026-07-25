package com.shiji.app.domain

import com.shiji.core.common.util.PortionConverter
import com.shiji.core.common.util.PortionConverter.PortionUnit
import org.junit.Assert.*
import org.junit.Test

class PortionConverterTest {

    @Test
    fun `estimate grams from grams returns same value`() {
        val result = PortionConverter.estimateGrams("米饭", 100.0, PortionUnit.GRAMS)
        assertEquals(100.0, result, 0.01)
    }

    @Test
    fun `estimate grams from serving`() {
        val result = PortionConverter.estimateGrams("宫保鸡丁", 1.0, PortionUnit.SERVING)
        assertEquals(200.0, result, 0.01)
    }

    @Test
    fun `estimate grams from bowl`() {
        val result = PortionConverter.estimateGrams("面条", 2.0, PortionUnit.BOWL)
        assertEquals(500.0, result, 0.01)
    }

    @Test
    fun `estimate grams from piece`() {
        val result = PortionConverter.estimateGrams("鸡蛋", 3.0, PortionUnit.PIECE)
        assertEquals(300.0, result, 0.01)
    }

    @Test
    fun `meal type display returns correct labels`() {
        assertEquals("🥣 早餐", PortionConverter.mealTypeDisplay("BREAKFAST"))
        assertEquals("🍱 午餐", PortionConverter.mealTypeDisplay("LUNCH"))
        assertEquals("🍽️ 晚餐", PortionConverter.mealTypeDisplay("DINNER"))
        assertEquals("🍎 加餐", PortionConverter.mealTypeDisplay("SNACK"))
    }

    @Test
    fun `infer meal type returns valid type`() {
        val type = PortionConverter.inferMealType()
        assertTrue(type in listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK"))
    }

    @Test
    fun `portion unit from string parses correctly`() {
        assertEquals(PortionUnit.GRAMS, PortionUnit.fromString("g"))
        assertEquals(PortionUnit.SERVING, PortionUnit.fromString("份"))
        assertEquals(PortionUnit.BOWL, PortionUnit.fromString("碗"))
        assertEquals(PortionUnit.PIECE, PortionUnit.fromString("个"))
        assertEquals(PortionUnit.GRAMS, PortionUnit.fromString("unknown"))
    }
}
