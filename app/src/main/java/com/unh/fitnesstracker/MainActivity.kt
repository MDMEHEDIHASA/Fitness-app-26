package com.unh.fitnesstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.unh.fitnesstracker.ui.navigation.AppNavHost
import com.unh.fitnesstracker.ui.theme.FitnessTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FitnessApp).container
        setContent {
            FitnessTrackerTheme {
                AppNavHost(container)
            }
        }
    }
}
