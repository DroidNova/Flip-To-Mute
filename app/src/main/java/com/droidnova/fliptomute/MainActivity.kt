package com.droidnova.fliptomute

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.droidnova.fliptomute.ui.navigation.FlipToMuteNavHost
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlipToMuteTheme {
                FlipToMuteNavHost()
            }
        }
    }
}
