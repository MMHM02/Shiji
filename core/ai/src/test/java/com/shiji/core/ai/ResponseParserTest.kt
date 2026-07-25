package com.shiji.core.ai.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseParserTest {

    @Test
    fun `parses clean camelCase json`() {
        val raw = """
        {
          "items": [
            {"name": "宫保鸡丁盖饭", "portion": 1, "portionUnit": "份",
             "calories": 650, "proteinGrams": 25, "carbsGrams": 70, "fatGrams": 20,
             "confidence": 0.9}
          ],
          "totalCalories": 650,
          "confidence": 0.9
        }
        """.trimIndent()
        val result = ResponseParser.parseFoodAnalysis(raw).getOrThrow()
        assertEquals(1, result.items.size)
        assertEquals("宫保鸡丁盖饭", result.items[0].name)
        assertEquals(650.0, result.totalCalories, 0.01)
        assertEquals(25.0, result.items[0].proteinGrams, 0.01)
    }

    @Test
    fun `parses snake_case json inside markdown fences`() {
        val raw = """
        好的，这是分析结果：
        ```json
        {
          "items": [
            {"name": "珍珠奶茶", "portion": 1, "portion_unit": "杯",
             "calories": 350, "protein_grams": 2, "carbs_grams": 60, "fat_grams": 10,
             "confidence": 0.85}
          ],
          "total_calories": 350,
          "general_confidence": 0.8
        }
        ```
        """.trimIndent()
        val result = ResponseParser.parseFoodAnalysis(raw).getOrThrow()
        assertEquals(1, result.items.size)
        assertEquals("珍珠奶茶", result.items[0].name)
        assertEquals("杯", result.items[0].portionUnit)
        assertEquals(60.0, result.items[0].carbsGrams, 0.01)
        assertEquals(0.8f, result.confidence, 0.01f)
    }

    @Test
    fun `empty items means no food detected`() {
        val raw = """{"items": [], "totalCalories": 0, "confidence": 0}"""
        val result = ResponseParser.parseFoodAnalysis(raw).getOrThrow()
        assertTrue(ResponseParser.hasNoFood(result))
    }

    @Test
    fun `garbage input returns failure`() {
        assertTrue(ResponseParser.parseFoodAnalysis("not json at all").isFailure)
    }

    @Test
    fun `missing fields fall back to defaults`() {
        val raw = """{"items": [{"name": "苹果"}]}"""
        val result = ResponseParser.parseFoodAnalysis(raw).getOrThrow()
        assertEquals("苹果", result.items[0].name)
        assertEquals(1.0, result.items[0].portion, 0.01)
        assertEquals("份", result.items[0].portionUnit)
    }
}
