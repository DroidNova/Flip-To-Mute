package com.droidnova.fliptomute.ui.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.droidnova.fliptomute.MainActivity
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchRequestParser
import com.droidnova.fliptomute.ui.screens.onboarding.AccessCommand
import com.droidnova.fliptomute.ui.screens.onboarding.OnboardingActions
import com.droidnova.fliptomute.ui.screens.onboarding.OnboardingExit
import com.droidnova.fliptomute.ui.screens.onboarding.OnboardingScreen
import com.droidnova.fliptomute.ui.screens.onboarding.OnboardingViewModel
import com.droidnova.fliptomute.ui.screens.onboarding.RuntimeSetupPermission
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import com.droidnova.fliptomute.utils.SettingsLaunchResult
import com.droidnova.fliptomute.utils.openAppDetailsSettings
import com.droidnova.fliptomute.utils.openAppNotificationSettings
import com.droidnova.fliptomute.utils.openNotificationPolicySettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * First run, and the Access step on its own when opened from Home, Settings or a test screen. Built
 * like Secret Calculator's OnBoardingActivity: it owns the permission launchers and system settings
 * intents, and the view model decides what happens next. No ads here (design spec 1).
 */
@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private val viewModel: OnboardingViewModel by viewModels()

    private val phoneLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onPermissionResult(
            RuntimeSetupPermission.PHONE,
            granted,
            permanentlyDenied = !granted && !shouldShowRationale(Manifest.permission.READ_PHONE_STATE),
        )
    }

    private val notificationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onPermissionResult(
            RuntimeSetupPermission.NOTIFICATIONS,
            granted,
            permanentlyDenied = !granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !shouldShowRationale(Manifest.permission.POST_NOTIFICATIONS),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exit.filterNotNull().collect(::leave)
            }
        }
        setContent {
            FlipToMuteTheme {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                    ) {
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        OnboardingScreen(state, actions)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Access may have been granted in system settings
        viewModel.onVisibilityChanged(true)
    }

    override fun onPause() {
        viewModel.onVisibilityChanged(false)
        super.onPause()
    }

    private val actions = object : OnboardingActions {
        override fun getStarted() = viewModel.onGetStarted()

        override fun primaryAccess() {
            val notificationRuntimeNeeded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this@OnboardingActivity, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            val command = viewModel.onPrimaryAccess(
                phoneRationale = shouldShowRationale(Manifest.permission.READ_PHONE_STATE),
                notificationRationale = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    shouldShowRationale(Manifest.permission.POST_NOTIFICATIONS),
                notificationRuntimeNeeded = notificationRuntimeNeeded,
            )
            when (command) {
                is AccessCommand.RequestPermission -> when (command.permission) {
                    RuntimeSetupPermission.PHONE -> phoneLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                    RuntimeSetupPermission.NOTIFICATIONS -> notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                AccessCommand.OpenAppDetails -> report(openAppDetailsSettings())
                AccessCommand.OpenNotificationSettings -> report(openAppNotificationSettings())
                AccessCommand.None -> Unit
            }
        }

        override fun confirmSoundHint() {
            viewModel.dismissSoundHint()
            report(openNotificationPolicySettings())
        }

        override fun dismissSoundHint() = viewModel.dismissSoundHint()
        override fun later() = viewModel.later()
        override fun finish() = viewModel.finish()
        override fun back() = viewModel.back()
        override fun messageShown() = viewModel.messageShown()
    }

    private fun report(result: SettingsLaunchResult) {
        if (result == SettingsLaunchResult.UNAVAILABLE) viewModel.onSettingsUnavailable()
    }

    private fun shouldShowRationale(permission: String) =
        ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

    private fun leave(exit: OnboardingExit) {
        when (exit) {
            is OnboardingExit.OpenHome -> {
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        // The same request the tile uses: Home turns Flip to Mute on (M4-01)
                        if (exit.enableMonitoring) action = MainActivityLaunchRequestParser.OPEN_SETUP_AND_ENABLE_ACTION
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    },
                )
                finish()
            }
            OnboardingExit.CloseAccess, OnboardingExit.Close -> finish()
        }
    }

    companion object {
        /** The Access step alone, for Home, Settings and the test screens (M4-06). */
        fun accessIntent(context: Context): Intent =
            Intent(context, OnboardingActivity::class.java).putExtra(OnboardingViewModel.EXTRA_ACCESS_ONLY, true)
    }
}
