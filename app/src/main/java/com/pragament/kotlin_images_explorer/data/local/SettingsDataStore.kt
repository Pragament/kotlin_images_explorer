package com.pragament.kotlin_images_explorer.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    private val SCAN_MODE = intPreferencesKey("scan_mode")
    private val SELECTED_MODEL = stringPreferencesKey("selected_model")

    val scanMode: Flow<ScanMode> = context.dataStore.data.map { preferences ->
        ScanMode.fromInt(preferences[SCAN_MODE] ?: ScanMode.ALL_DEVICE_IMAGES.ordinal)
    }

    val selectedModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_MODEL] ?: DEFAULT_MODEL
    }

    suspend fun setScanMode(mode: ScanMode) {
        context.dataStore.edit { preferences ->
            preferences[SCAN_MODE] = mode.ordinal
        }
    }

    suspend fun setSelectedModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_MODEL] = model
        }
    }

    suspend fun getSelectedModel(): String {
        return context.dataStore.data.map { preferences ->
            preferences[SELECTED_MODEL] ?: DEFAULT_MODEL
        }.firstOrNull() ?: DEFAULT_MODEL
    }

    companion object {
        private const val DEFAULT_MODEL = "mobilenet_v1"
    }
}

enum class ScanMode {
    ALL_DEVICE_IMAGES,
    MULTIPLE_IMAGES,
    SINGLE_IMAGE;

    companion object {
        fun fromInt(value: Int) = entries.getOrNull(value) ?: ALL_DEVICE_IMAGES
    }
}
