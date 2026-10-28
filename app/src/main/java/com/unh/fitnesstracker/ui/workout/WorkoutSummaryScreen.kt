package com.unh.fitnesstracker.ui.workout

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unh.fitnesstracker.data.model.UnitSystem
import com.unh.fitnesstracker.data.model.Workout
import com.unh.fitnesstracker.data.repository.WorkoutRepository
import com.unh.fitnesstracker.data.session.ActiveWorkoutSession
import com.unh.fitnesstracker.ui.main.MainScreenScaffold
import com.unh.fitnesstracker.ui.main.TopBarNav

/**
 * S16 Summary, strength version. S12 has already saved the workout (so nothing is lost if the app
 * closes here); Done keeps it, Discard deletes it again.
 */
@Composable
fun WorkoutSummaryScreen(
    uid: String,
    session: ActiveWorkoutSession,
    workoutRepository: WorkoutRepository,
    units: UnitSystem,
    onDone: () -> Unit,
) {
    val workout by session.lastSaved.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDiscard by rememberSaveable { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }

    val close: () -> Unit = {
        leaving = true
        session.clearLastSaved()
        onDone()
    }
    BackHandler(onBack = close)

    val saved = workout
    if (saved == null) {
        if (!leaving) LaunchedEffect(Unit) { onDone() }
        return
    }

    MainScreenScaffold(
        title = "Workout saved",
        nav = TopBarNav.Close(close),
        actions = {
            IconButton(onClick = {
                val send = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, WorkoutFormat.shareText(saved, units))
                context.startActivity(Intent.createChooser(send, "Share workout"))
            }) {
                Icon(Icons.Filled.Share, contentDescription = "Share workout")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(saved.name, style = MaterialTheme.typography.headlineSmall)
                Text(WorkoutFormat.dateTime(saved.startedAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { SummaryStats(saved, units) }
            if (saved.rating != null || saved.notes.isNotEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            saved.rating?.let { Text("Felt: $it / 5", style = MaterialTheme.typography.titleSmall) }
                            if (saved.notes.isNotEmpty()) Text(saved.notes)
                        }
                    }
                }
            }
            items(saved.exercises) { entry ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(entry.name, style = MaterialTheme.typography.titleMedium)
                        entry.sets.forEachIndexed { index, set ->
                            Row {
                                Text(
                                    "Set ${index + 1}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(WorkoutFormat.set(set, entry.trackedBy, units))
                            }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showDiscard = true }, modifier = Modifier.weight(1f)) { Text("Discard") }
                    Button(onClick = close, modifier = Modifier.weight(1f)) { Text("Done") }
                }
            }
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Delete this workout?") },
            text = { Text("It has already been saved. Deleting removes it from your history.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscard = false
                    workoutRepository.delete(uid, saved.id)
                    close()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("Keep") } },
        )
    }
}

@Composable
private fun SummaryStats(workout: Workout, units: UnitSystem) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SummaryStat(WorkoutFormat.clock(workout.durationSec), "Duration")
                SummaryStat(workout.totalSets.toString(), "Sets")
                SummaryStat(WorkoutFormat.volume(workout.totalVolumeKg, units), "Volume")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SummaryStat(workout.calories.toString(), "kcal (est.)")
                SummaryStat(workout.avgHr?.toString() ?: "--", "Avg bpm")
                SummaryStat(workout.exercises.size.toString(), "Exercises")
            }
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
