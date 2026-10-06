package com.droidnova.fliptomute.data

import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import java.io.File
import okio.FileSystem
import okio.Path.Companion.toOkioPath

/**
 * Preferences storage for JVM tests. DataStore's file storage replaces the file with
 * File.renameTo, which cannot overwrite an existing file on Windows, so a second write
 * fails there. Okio moves with replace-existing on every OS. Android itself is unaffected.
 */
internal fun okioStorage(file: File): Storage<Preferences> =
    OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) { file.absoluteFile.toOkioPath() }
