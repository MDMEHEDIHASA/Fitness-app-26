package com.unh.fitnesstracker.ui.workout

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unh.fitnesstracker.data.model.ExerciseEntry
import com.unh.fitnesstracker.data.model.TrackedBy
import com.unh.fitnesstracker.data.model.UnitSystem
import com.unh.fitnesstracker.data.model.Workout
import com.unh.fitnesstracker.ui.components.HeartRateChart
import com.unh.fitnesstracker.ui.main.MainScreenScaffold
import com.unh.fitnesstracker.ui.main.TopBarNav
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val UNDO_MS = 5_000L

/** S18 Workout detail. Edit updates Firestore; Repeat opens S9 pre-filled. */
@Composable
fun WorkoutDetailScreen(
    viewModel: WorkoutDetailViewModel,
    onBack: () -> Unit,
    onRepeat: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var editing by rememberSaveable { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var showTemplate by rememberSaveable { mutableStateOf(false) }
    var deleted by rememberSaveable { mutableStateOf(false) }
    var notesDraft by rememberSaveable { mutableStateOf<String?>(null) }
    var setDrafts by remember { mutableStateOf<List<List<EditableSet>>>(emptyList()) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    val workout = state.workout
    if (workout == null) {
        LaunchedEffect(deleted, state.missing) {
            if (deleted) {
                val job = launch {
                    delay(UNDO_MS)
                    snackbarHostState.currentSnackbarData?.dismiss()
                }
                val result = snackbarHostState.showSnackbar(
                    message = "Workout deleted",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Indefinite,
                    withDismissAction = true,
                )
                job.cancel()
                if (result == SnackbarResult.ActionPerformed && viewModel.undoDelete()) {
                    deleted = false
                } else {
                    onBack()
                }
            } else if (state.missing) {
                onBack()
            }
        }
        MainScreenScaffold(
            title = "Workout",
            nav = TopBarNav.Back(onBack),
            snackbarHostState = snackbarHostState,
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (!deleted) CircularProgressIndicator()
            }
        }
        return
    }

    LaunchedEffect(editing, workout.id) {
        if (editing) {
            notesDraft = workout.notes
            setDrafts = workout.exercises.map { entry ->
                entry.sets.map { it.toEditable(entry.trackedBy, state.units) }
            }
        } else {
            notesDraft = null
            setDrafts = emptyList()
        }
    }

    MainScreenScaffold(
        title = workout.name,
        nav = TopBarNav.Back(onBack),
        snackbarHostState = snackbarHostState,
        actions = {
            if (editing) {
                TextButton(onClick = {
                    val notes = notesDraft ?: workout.notes
                    if (viewModel.saveEdits(notes, setDrafts)) editing = false
                }) { Text("Save") }
            } else {
                TextButton(onClick = { editing = true }) { Text("Edit") }
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Save as template") },
                        leadingIcon = { Icon(Icons.Filled.BookmarkAdd, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showTemplate = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            val send = Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, WorkoutFormat.shareText(workout, state.units))
                            context.startActivity(Intent.createChooser(send, "Share workout"))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showDelete = true
                        },
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(WorkoutFormat.dateTime(workout.startedAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { DetailStats(workout, state.units) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Heart rate", style = MaterialTheme.typography.titleSmall)
                        HeartRateChart(samples = emptyList())
                    }
                }
            }
            item {
                if (editing) {
                    OutlinedTextField(
                        value = notesDraft ?: workout.notes,
                        onValueChange = { notesDraft = it.take(500) },
                        label = { Text("Notes") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (workout.rating != null || workout.notes.isNotEmpty()) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            workout.rating?.let { Text("Felt: $it / 5", style = MaterialTheme.typography.titleSmall) }
                            if (workout.notes.isNotEmpty()) Text(workout.notes)
                        }
                    }
                }
            }
            if (editing) {
                itemsIndexed(workout.exercises) { exerciseIndex, entry ->
                    val drafts = setDrafts.getOrNull(exerciseIndex).orEmpty()
                    EditExerciseCard(
                        entry = entry,
                        drafts = drafts,
                        units = state.units,
                        onChange = { setIndex, draft ->
                            setDrafts = setDrafts.mapIndexed { ei, sets ->
                                if (ei != exerciseIndex) sets
                                else sets.mapIndexed { si, old -> if (si == setIndex) draft else old }
                            }
                        },
                    )
                }
            } else {
                itemsIndexed(workout.exercises) { _, entry ->
                    ExerciseCard(entry, state.units)
                }
            }
            if (!editing) {
                item {
                    Button(
                        onClick = { if (viewModel.repeat()) onRepeat() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Repeat Workout") }
                    OutlinedButton(
                        onClick = { showDelete = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) { Text("Delete") }
                }
            }
        }
    }

    if (showTemplate) {
        NameDialog(
            title = "Save as template",
            initialName = workout.name,
            confirmLabel = "Save",
            onConfirm = {
                viewModel.saveAsTemplate(it)
                showTemplate = false
            },
            onDismiss = { showTemplate = false },
        )
    }

    if (showDelete) {
        DeleteWorkoutDialog(
            workoutName = workout.name,
            onConfirm = {
                showDelete = false
                deleted = true
                viewModel.delete()
            },
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
private fun DetailStats(workout: Workout, units: UnitSystem) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat(WorkoutFormat.clock(workout.durationSec), "Duration")
                Stat(WorkoutFormat.volume(workout.totalVolumeKg, units), "Volume")
                Stat(workout.avgHr?.toString() ?: "--", "Avg bpm")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat(workout.totalSets.toString(), "Sets")
                Stat(workout.calories.toString(), "kcal")
                Stat(workout.exercises.size.toString(), "Exercises")
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExerciseCard(entry: ExerciseEntry, units: UnitSystem) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entry.name, style = MaterialTheme.typography.titleMedium)
            entry.sets.forEachIndexed { index, set ->
                Row {
                    Text("Set ${index + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(WorkoutFormat.set(set, entry.trackedBy, units))
                }
            }
        }
    }
}

@Composable
private fun EditExerciseCard(
    entry: ExerciseEntry,
    drafts: List<EditableSet>,
    units: UnitSystem,
    onChange: (Int, EditableSet) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(entry.name, style = MaterialTheme.typography.titleMedium)
            drafts.forEachIndexed { index, draft ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Set ${index + 1}", modifier = Modifier.weight(0.8f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = draft.weight,
                        onValueChange = { onChange(index, draft.copy(weight = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' })) },
                        label = { Text(WorkoutFormat.unitLabel(units)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = draft.reps,
                        onValueChange = { onChange(index, draft.copy(reps = it.filter(Char::isDigit))) },
                        label = { Text(if (entry.trackedBy == TrackedBy.TIME) "sec" else "reps") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
