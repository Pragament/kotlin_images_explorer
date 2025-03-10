package com.pragament.kotlin_images_explorer.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    private val SCAN_MODE = intPreferencesKey("scan_mode")

    val scanMode: Flow<ScanMode> = context.dataStore.data.map { preferences ->
        ScanMode.fromInt(preferences[SCAN_MODE] ?: ScanMode.ALL_DEVICE_IMAGES.ordinal)
    }

    suspend fun setScanMode(mode: ScanMode) {
        context.dataStore.edit { preferences ->
            preferences[SCAN_MODE] = mode.ordinal
        }
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