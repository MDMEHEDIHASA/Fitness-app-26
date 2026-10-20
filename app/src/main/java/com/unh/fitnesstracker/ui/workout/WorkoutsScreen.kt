package com.unh.fitnesstracker.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unh.fitnesstracker.ui.main.MainScreenScaffold
import com.unh.fitnesstracker.ui.main.TopBarNav

/** Workouts tab. Shows the S31 empty state until saved workouts and the S17 history list arrive (Sprints 3–4). */
@Composable
fun WorkoutsScreen(onMenuClick: () -> Unit, onStartWorkout: () -> Unit) {
    MainScreenScaffold(
        title = "Workouts",
        nav = TopBarNav.Menu(onMenuClick),
        floatingActionButton = {
            FloatingActionButton(onClick = onStartWorkout) {
                Icon(Icons.Filled.Add, contentDescription = "Start a workout")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Text("No workouts yet", style = MaterialTheme.typography.titleLarge)
            Text(
                "Your saved workouts will show up here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onStartWorkout) { Text("Start First Workout") }
        }
    }
}
