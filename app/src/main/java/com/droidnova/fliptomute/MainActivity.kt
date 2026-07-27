package com.droidnova.fliptomute

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.droidnova.fliptomute.app.FlipToMuteApplication
import com.droidnova.fliptomute.app.ViewModelFactories
import com.droidnova.fliptomute.ui.navigation.FlipToMuteNavHost
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factories = ViewModelFactories((application as FlipToMuteApplication).container)
        setContent {
            FlipToMuteTheme {
                FlipToMuteNavHost(viewModelFactories = factories)
            }
        }
    }
}
