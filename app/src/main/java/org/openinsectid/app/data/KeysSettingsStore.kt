package org.openinsectid.app.data

import android.R.attr.mode
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.datastore by preferencesDataStore("datastore")

object KeysSettingsStore {
    private val ALPHA_LLM_APK_KEY = stringPreferencesKey("alpha_llm_api_key")

    private val ALPHA_LLM_API_URL = stringPreferencesKey("alpha_llm_api_url")
    suspend fun setAlphaLLMApiKey(ctx: Context, key: String) {
        ctx.datastore.edit { it[ALPHA_LLM_APK_KEY] = key }
    }

    fun getAlphaLLMApiKey(ctx: Context): Flow<String> =
        ctx.datastore.data.map { prefs ->
            prefs[ALPHA_LLM_APK_KEY] ?: ""
        }

    suspend fun setAlphaLLMApiUrl(ctx: Context, key: String) {
        ctx.datastore.edit { it[ALPHA_LLM_API_URL] = key }
    }

    fun getAlphaLLMApiUrl(ctx: Context): Flow<String> =
        ctx.datastore.data.map { prefs ->
            prefs[ALPHA_LLM_API_URL] ?: ""
        }
}
