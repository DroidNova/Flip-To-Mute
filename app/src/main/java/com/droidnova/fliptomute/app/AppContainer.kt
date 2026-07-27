package com.droidnova.fliptomute.app

import android.content.Context
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.preferences.DataStoreAppPreferencesRepository

interface AppContainer {
    val appPreferencesRepository: AppPreferencesRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    override val appPreferencesRepository: AppPreferencesRepository =
        DataStoreAppPreferencesRepository(context.applicationContext)
}
