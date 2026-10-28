package com.unh.fitnesstracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unh.fitnesstracker.ui.components.InitialsAvatar
import com.unh.fitnesstracker.ui.main.MainScreenScaffold
import com.unh.fitnesstracker.ui.main.TopBarNav
import com.unh.fitnesstracker.ui.workout.WorkoutFormat
import java.text.NumberFormat

/** S5 Home dashboard. */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMenuClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onWatchClick: () -> Unit,
    onMetricClick: () -> Unit,
    onGoalsClick: () -> Unit,
    onRemindersClick: () -> Unit,
    onStartWorkout: () -> Unit,
    onResumeWorkout: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val numbers = NumberFormat.getIntegerInstance()

    MainScreenScaffold(
        title = "Today",
        nav = TopBarNav.Menu(onMenuClick),
        actions = {
            IconButton(onClick = onAvatarClick) {
                InitialsAvatar(name = state.firstName, size = 32.dp)
            }
        },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (state.firstName.isNotEmpty()) "Hi, ${state.firstName}" else "Hi there",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(state.dateText, color = MaterialTheme.colorScheme.onSurfaceVariant)

            state.workoutInProgress?.let { active ->
                Card(
                    onClick = onResumeWorkout,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Workout in progress", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "${active.name} · ${WorkoutFormat.clock(active.elapsedSec)}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Button(onClick = onResumeWorkout) { Text("Resume") }
                    }
                }
            }

            // ② Watch status chip
            AssistChip(
                onClick = onWatchClick,
                label = { Text(if (state.watchConnected) "Watch connected" else "No watch connected · tap to pair") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Circle,
                        contentDescription = null,
                        tint = if (state.watchConnected) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(AssistChipDefaults.IconSize / 2),
                    )
                },
            )

            // ③ Metric cards
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    label = "Steps",
                    value = state.steps?.let(numbers::format) ?: "--",
                    caption = "Goal ${numbers.format(state.stepGoal)}",
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    progress = state.steps?.let { it.toFloat() / state.stepGoal },
                    onClick = onMetricClick,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = "Heart rate",
                    value = state.heartRateBpm?.let { "$it bpm" } ?: "-- bpm",
                    caption = if (state.watchConnected) "Live from watch" else "Needs a watch",
                    icon = Icons.Filled.Favorite,
                    onClick = onMetricClick,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    label = "Active min",
                    value = state.activeMinutes.toString(),
                    caption = "Today",
                    icon = Icons.Filled.Timer,
                    onClick = onMetricClick,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = "Calories",
                    value = numbers.format(state.calories),
                    caption = "kcal today",
                    icon = Icons.Filled.LocalFireDepartment,
                    onClick = onMetricClick,
                    modifier = Modifier.weight(1f),
                )
            }

            // ④ Today's plan
            Card(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Today's plan", style = MaterialTheme.typography.labelLarge)
                        val plan = state.todaysPlan
                        Text(
                            if (plan != null) "${plan.name} · ${plan.timeText}" else "Nothing planned",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    if (state.todaysPlan != null) {
                        Button(onClick = onStartWorkout) { Text("Start") }
                    } else {
                        OutlinedButton(onClick = onRemindersClick) { Text("Plan") }
                    }
                }
            }

            // Weekly goal
            Card(onClick = onGoalsClick, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    val goal = state.weeklyWorkoutGoal
                    Text(
                        if (goal != null) "Weekly goal · ${state.workoutsThisWeek} of $goal workouts"
                        else "This week · ${state.workoutsThisWeek} ${if (state.workoutsThisWeek == 1) "workout" else "workouts"}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (goal != null && goal > 0) state.workoutsThisWeek.toFloat() / goal else 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (goal == null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No weekly goal yet · tap to set one",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Leaves room so the + button doesn't cover the last card.
            Spacer(Modifier.height(72.dp))
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    caption: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    Card(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (progress != null) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
