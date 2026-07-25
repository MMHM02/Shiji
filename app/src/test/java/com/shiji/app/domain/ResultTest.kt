package com.shiji.app.domain

import com.shiji.core.common.result.Result
import org.junit.Assert.*
import org.junit.Test

class ResultTest {

    @Test
    fun `success returns correct data`() {
        val result = Result.success(42)
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `error returns exception`() {
        val ex = RuntimeException("test error")
        val result = Result.error(ex)
        assertTrue(result.isError)
        assertFalse(result.isSuccess)
        assertNull(result.getOrNull())
        assertEquals(ex, result.errorOrNull())
    }

    @Test
    fun `map transforms success`() {
        val result = Result.success(10).map { it * 2 }
        assertEquals(20, result.getOrNull())
    }

    @Test
    fun `map preserves error`() {
        val result: Result<Int> = Result.error(RuntimeException("err"))
        val mapped = result.map { it * 2 }
        assertTrue(mapped.isError)
    }

    @Test
    fun `getOrDefault returns default on error`() {
        val result: Result<String> = Result.error(RuntimeException())
        assertEquals("fallback", result.getOrDefault("fallback"))
    }

    @Test
    fun `getOrDefault returns data on success`() {
        val result = Result.success("hello")
        assertEquals("hello", result.getOrDefault("fallback"))
    }
}
