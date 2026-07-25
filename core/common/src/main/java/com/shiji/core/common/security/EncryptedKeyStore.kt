package com.shiji.core.common.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage for API Keys using Android Keystore + EncryptedSharedPreferences.
 */
class EncryptedKeyStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_ai_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveKey(providerId: String, apiKey: String) {
        prefs.edit().putString("key_$providerId", apiKey).apply()
    }

    fun getKey(providerId: String): String? {
        return prefs.getString("key_$providerId", null)
    }

    fun deleteKey(providerId: String) {
        prefs.edit().remove("key_$providerId").apply()
    }

    fun hasKey(providerId: String): Boolean {
        return prefs.contains("key_$providerId")
    }

    fun getAllProviderIds(): List<String> {
        return prefs.all.keys
            .filter { it.startsWith("key_") }
            .map { it.removePrefix("key_") }
    }
}
