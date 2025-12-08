package com.enertrack.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

// Nama file penyimpanan: "session"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

class SessionManager(private val context: Context) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        // === TAMBAHAN KEY USER ID ===
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        // ============================
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val EMAIL_KEY = stringPreferencesKey("email")
    }

    // === SAVE FUNCTIONS ===
    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    // Fungsi simpan User ID
    suspend fun saveUserId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = id
        }
    }

    suspend fun saveUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
        }
    }

    suspend fun saveEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[EMAIL_KEY] = email
        }
    }

    // === GET FUNCTIONS (FLOW) ===
    val usernameFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USERNAME_KEY]
    }

    val emailFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[EMAIL_KEY]
    }

    // === GET FUNCTIONS (DIRECT) ===

    // Fungsi ambil User ID
    fun getUserId(): String? = runBlocking {
        context.dataStore.data.map { preferences ->
            preferences[USER_ID_KEY]
        }.first()
    }

    fun getUsername(): String? = runBlocking {
        context.dataStore.data.map { preferences ->
            preferences[USERNAME_KEY]
        }.first()
    }

    fun getEmail(): String? = runBlocking {
        context.dataStore.data.map { preferences ->
            preferences[EMAIL_KEY]
        }.first()
    }

    suspend fun getAuthToken(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }.first()
    }

    // === CLEAR SESSION ===
    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }

        try {
            context.getSharedPreferences("CookiePersistence", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}