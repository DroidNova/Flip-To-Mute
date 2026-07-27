package com.droidnova.fliptomute.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val PREFERENCES_FILE_NAME = "flip_to_mute_preferences"

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_FILE_NAME)
