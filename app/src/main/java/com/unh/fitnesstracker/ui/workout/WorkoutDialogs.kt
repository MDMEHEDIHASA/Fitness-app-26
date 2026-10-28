package com.unh.fitnesstracker.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unh.fitnesstracker.data.session.ActiveWorkout

/** S12 Finish dialog. Unchecked sets are not saved, so the dialog warns about them. */
@Composable
fun FinishWorkoutDialog(
    workout: ActiveWorkout,
    onKeepGoing: () -> Unit,
    onFinish: (rating: Int?, notes: String) -> Unit,
) {
    var rating by rememberSaveable { mutableStateOf<Int?>(null) }
    var notes by rememberSaveable { mutableStateOf("") }
    val unchecked = workout.totalSets - workout.completedSets

    AlertDialog(
        onDismissRequest = onKeepGoing,
        title = { Text("Finish workout?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DialogStat(WorkoutFormat.clock(workout.elapsedSec), "Duration")
                    DialogStat(workout.completedSets.toString(), "Sets")
                    DialogStat("--", "Avg bpm")
                }
                Text("How did it feel?", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { value ->
                        FilterChip(
                            selected = rating == value,
                            onClick = { rating = if (rating == value) null else value },
                            label = { Text(value.toString()) },
                        )
                    }
                }
                Text(
                    "1 = rough · 5 = great",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(MAX_NOTES_LENGTH) },
                    label = { Text("Notes (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                when {
                    workout.completedSets == 0 -> Warning("Check at least one set to save this workout.")
                    unchecked > 0 -> Warning(
                        "$unchecked ${if (unchecked == 1) "set isn't" else "sets aren't"} checked and won't be saved.",
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onFinish(rating, notes) }, enabled = workout.completedSets > 0) {
                Text("Finish & Save")
            }
        },
        dismissButton = { TextButton(onClick = onKeepGoing) { Text("Keep going") } },
    )
}

@Composable
fun DiscardWorkoutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discard workout?") },
        text = { Text("Everything you logged in this workout will be lost.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Discard", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** One-field dialog used for "Rename workout" and "Save as template". */
@Composable
fun NameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_NAME_LENGTH) },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DialogStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Warning(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
    }
}

private const val MAX_NOTES_LENGTH = 500
private const val MAX_NAME_LENGTH = 40
