package com.droidnova.fliptomute.app

import android.content.Context
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.preferences.DataStoreAppPreferencesRepository
import com.droidnova.fliptomute.data.setup.AndroidSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessRepository

interface AppContainer {
    val appPreferencesRepository: AppPreferencesRepository
    val setupAccessRepository: SetupAccessRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    override val appPreferencesRepository: AppPreferencesRepository =
        DataStoreAppPreferencesRepository(context.applicationContext)
    override val setupAccessRepository: SetupAccessRepository =
        AndroidSetupAccessRepository(context.applicationContext)
}
