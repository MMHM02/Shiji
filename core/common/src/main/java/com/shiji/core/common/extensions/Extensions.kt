package com.shiji.core.common.extensions

/**
 * Common Kotlin extension functions used across the app.
 * This file will be expanded as needed during Phase 1+.
 */

/**
 * Returns the string or a default value if null or blank.
 */
fun String?.orDefault(default: String): String =
    if (this.isNullOrBlank()) default else this
