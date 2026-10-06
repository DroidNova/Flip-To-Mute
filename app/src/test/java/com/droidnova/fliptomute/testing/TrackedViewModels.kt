package com.droidnova.fliptomute.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore

/**
 * Copied from Secret Calculator. Keeps the ViewModels a test creates, so tearDown can cancel their
 * work (clear()) before resetting Dispatchers.Main. Otherwise a collector still running from the
 * test can race with resetMain and fail an unrelated test.
 */
class TrackedViewModels {
    private val store = ViewModelStore()
    private var next = 0

    fun <T : ViewModel> track(viewModel: T): T = viewModel.also { store.put("vm${next++}", it) }

    fun clear() = store.clear()
}
