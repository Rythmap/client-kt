package com.mvnh.rythmap.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {

    companion object {
        private const val ENCRYPTED_PREFS_FILE_NAME = "encrypted_prefs"
        private const val ACCESS_TOKEN_KEY = "access_token"
    }

    private val encryptedSharedPreferences: SharedPreferences = try {
        val masterKeyAlias = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_FILE_NAME,
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.d("Rythmap", e.toString())
        context.getSharedPreferences(ENCRYPTED_PREFS_FILE_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        val editor = encryptedSharedPreferences.edit()
        editor.putString(ACCESS_TOKEN_KEY, token)
        editor.apply()
    }

    fun getToken(): String? {
        return encryptedSharedPreferences.getString(ACCESS_TOKEN_KEY, null)
    }

    fun clearToken() {
        val editor = encryptedSharedPreferences.edit()
        editor.remove(ACCESS_TOKEN_KEY)
        editor.apply()
    }
}