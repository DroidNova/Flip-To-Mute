package com.droidnova.fliptomute.di

import android.content.Context
import com.droidnova.fliptomute.boot.BootMonitoringCoordinator
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.deviceadmin.DeviceAdminCapabilityRepository
import com.droidnova.fliptomute.quicksettings.QuickSettingsTileUpdateRequester
import com.droidnova.fliptomute.service.MonitoringHealthCheck
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * What broadcast receivers and the health check worker need from the object graph. A plain entry
 * point, as Secret Calculator's TrashCleanupWorker uses, keeps WorkManager's default setup and avoids
 * Hilt's receiver base-class rules.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackgroundEntryPoint {
    fun bootMonitoringCoordinator(): BootMonitoringCoordinator
    fun monitoringHealthCheck(): MonitoringHealthCheck
    fun deviceAdminCapabilityRepository(): DeviceAdminCapabilityRepository
    fun appPreferencesRepository(): AppPreferencesRepository
    fun quickSettingsTileUpdateRequester(): QuickSettingsTileUpdateRequester
}

fun Context.backgroundEntryPoint(): BackgroundEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, BackgroundEntryPoint::class.java)
