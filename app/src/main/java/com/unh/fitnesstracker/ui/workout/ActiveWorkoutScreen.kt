package com.unh.fitnesstracker.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unh.fitnesstracker.data.model.TrackedBy
import com.unh.fitnesstracker.data.model.UnitSystem
import com.unh.fitnesstracker.data.session.ActiveExercise
import com.unh.fitnesstracker.data.session.ActiveSet
import com.unh.fitnesstracker.data.session.ActiveWorkout
import com.unh.fitnesstracker.ui.main.MainScreenScaffold
import com.unh.fitnesstracker.ui.main.TopBarNav

private enum class NameDialogMode { RENAME, TEMPLATE }

/**
 * S9 Active strength workout. System back leaves the screen with the workout still running
 * (Home shows a Resume card); ✕ discards it.
 */
@Composable
fun ActiveWorkoutScreen(
    viewModel: ActiveWorkoutViewModel,
    onAddExercise: () -> Unit,
    onFinished: () -> Unit,
    onClose: () -> Unit,
) {
    val workout by viewModel.workout.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val session = viewModel.session
    val snackbarHostState = remember { SnackbarHostState() }

    // Set right before finishing or discarding, so the null workout that follows doesn't navigate twice.
    var leaving by remember { mutableStateOf(false) }
    var showDiscard by rememberSaveable { mutableStateOf(false) }
    var showFinish by rememberSaveable { mutableStateOf(false) }
    var nameDialog by rememberSaveable { mutableStateOf<NameDialogMode?>(null) }
    var reorderMode by rememberSaveable { mutableStateOf(false) }
    var hiddenRestId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    val current = workout
    if (current == null) {
        if (!leaving) LaunchedEffect(Unit) { onClose() }
        return
    }

    MainScreenScaffold(
        title = current.name,
        nav = TopBarNav.Close({ showDiscard = true }, description = "Discard workout"),
        snackbarHostState = snackbarHostState,
        actions = {
            Text(
                WorkoutFormat.clock(current.elapsedSec),
                style = MaterialTheme.typography.titleMedium,
                color = if (current.paused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            WorkoutMenu(
                paused = current.paused,
                reorderMode = reorderMode,
                onPauseToggle = { if (current.paused) session.resume() else session.pause() },
                onReorderToggle = { reorderMode = !reorderMode },
                onRename = { nameDialog = NameDialogMode.RENAME },
                onSaveTemplate = { nameDialog = NameDialogMode.TEMPLATE },
            )
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
            item { LiveStrip(current) }

            if (current.paused) {
                item {
                    FilledTonalButton(onClick = session::resume, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Paused · tap to resume")
                    }
                }
            }

            val rest = current.rest
            if (rest != null && rest.id == hiddenRestId) {
                item {
                    FilledTonalButton(onClick = { hiddenRestId = null }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Timer, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Resting · ${WorkoutFormat.clock(rest.remainingSec.toLong())} · show timer")
                    }
                }
            }

            if (current.exercises.isEmpty()) {
                item {
                    Text(
                        "No exercises yet. Add one to start logging sets.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }

            itemsIndexed(current.exercises, key = { _, exercise -> exercise.key }) { index, exercise ->
                ExerciseCard(
                    exercise = exercise,
                    units = current.units,
                    reorderMode = reorderMode,
                    canMoveUp = index > 0,
                    canMoveDown = index < current.exercises.lastIndex,
                    onToggleCollapsed = { session.toggleCollapsed(exercise.key) },
                    onMove = { delta -> session.moveExercise(exercise.key, delta) },
                    onRemove = { session.removeExercise(exercise.key) },
                    onAddSet = { session.addSet(exercise.key) },
                    onRemoveSet = { set -> session.removeSet(exercise.key, set.key) },
                    onWeightChange = { set, value -> session.updateSet(exercise.key, set.key, weight = value) },
                    onRepsChange = { set, value -> session.updateSet(exercise.key, set.key, reps = value) },
                    onToggleSet = { set -> session.toggleSetCompleted(exercise.key, set.key) },
                    modifier = Modifier.animateItem(),
                )
            }

            item {
                OutlinedButton(onClick = onAddExercise, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Exercise")
                }
            }
            item {
                Button(onClick = { showFinish = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Finish Workout")
                }
            }
        }
    }

    val rest = current.rest
    if (rest != null && rest.id != hiddenRestId) {
        RestTimerSheet(
            rest = rest,
            onAdjust = session::adjustRest,
            onSkip = session::skipRest,
            onHide = { hiddenRestId = rest.id },
        )
    }

    if (showFinish) {
        FinishWorkoutDialog(
            workout = current,
            onKeepGoing = { showFinish = false },
            onFinish = { rating, notes ->
                leaving = true
                if (viewModel.finish(rating, notes)) {
                    showFinish = false
                    onFinished()
                } else {
                    leaving = false
                }
            },
        )
    }

    if (showDiscard) {
        DiscardWorkoutDialog(
            onConfirm = {
                showDiscard = false
                leaving = true
                session.discard()
                onClose()
            },
            onDismiss = { showDiscard = false },
        )
    }

    when (nameDialog) {
        NameDialogMode.RENAME -> NameDialog(
            title = "Rename workout",
            initialName = current.name,
            confirmLabel = "Rename",
            onConfirm = {
                session.rename(it)
                nameDialog = null
            },
            onDismiss = { nameDialog = null },
        )
        NameDialogMode.TEMPLATE -> NameDialog(
            title = "Save as template",
            initialName = current.name,
            confirmLabel = "Save",
            onConfirm = {
                viewModel.saveAsTemplate(it)
                nameDialog = null
            },
            onDismiss = { nameDialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun WorkoutMenu(
    paused: Boolean,
    reorderMode: Boolean,
    onPauseToggle: () -> Unit,
    onReorderToggle: () -> Unit,
    onRename: () -> Unit,
    onSaveTemplate: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Workout options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuItem(if (paused) "Resume" else "Pause", if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause) {
                expanded = false
                onPauseToggle()
            }
            MenuItem(if (reorderMode) "Done reordering" else "Reorder", Icons.Filled.SwapVert) {
                expanded = false
                onReorderToggle()
            }
            MenuItem("Rename", Icons.Filled.Edit) {
                expanded = false
                onRename()
            }
            MenuItem("Save as template", Icons.Filled.BookmarkAdd) {
                expanded = false
                onSaveTemplate()
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}

/** Heart rate and steps need sensors (Sprint 9) or a watch (Sprint 10); calories are estimated now. */
@Composable
private fun LiveStrip(workout: ActiveWorkout) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            LiveStat(Icons.Filled.Favorite, "--", "bpm")
            LiveStat(Icons.AutoMirrored.Filled.DirectionsWalk, "--", "steps")
            LiveStat(Icons.Filled.LocalFireDepartment, workout.calories.toString(), "kcal")
        }
    }
}

@Composable
private fun LiveStat(icon: ImageVector, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExerciseCard(
    exercise: ActiveExercise,
    units: UnitSystem,
    reorderMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleCollapsed: () -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (ActiveSet) -> Unit,
    onWeightChange: (ActiveSet, String) -> Unit,
    onRepsChange: (ActiveSet, String) -> Unit,
    onToggleSet: (ActiveSet) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackedBy = exercise.exercise.trackedBy
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !reorderMode, onClick = onToggleCollapsed)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                ) {
                    Text(exercise.exercise.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${exercise.completedSets} / ${exercise.sets.size} sets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (reorderMode) {
                    IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up")
                    }
                    IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down")
                    }
                } else {
                    Icon(
                        if (exercise.collapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                        contentDescription = if (exercise.collapsed) "Show sets" else "Hide sets",
                    )
                    ExerciseMenu(onRemove)
                }
            }

            if (!exercise.collapsed && !reorderMode) {
                SetHeader(trackedBy, units)
                exercise.sets.forEachIndexed { index, set ->
                    SetRow(
                        number = index + 1,
                        set = set,
                        onWeightChange = { onWeightChange(set, it) },
                        onRepsChange = { onRepsChange(set, it) },
                        onToggle = { onToggleSet(set) },
                        onRemove = { onRemoveSet(set) },
                    )
                }
                TextButton(onClick = onAddSet, modifier = Modifier.padding(start = 8.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add set")
                }
            }
        }
    }
}

@Composable
private fun ExerciseMenu(onRemove: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Exercise options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuItem("Remove exercise", Icons.Filled.Delete) {
                expanded = false
                onRemove()
            }
        }
    }
}

@Composable
private fun SetHeader(trackedBy: TrackedBy, units: UnitSystem) {
    val style = MaterialTheme.typography.labelMedium
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Set", style = style, color = color, textAlign = TextAlign.Center, modifier = Modifier.width(SET_NUMBER_WIDTH))
        Text(WorkoutFormat.unitLabel(units), style = style, color = color, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Text(
            if (trackedBy == TrackedBy.TIME) "Sec" else "Reps",
            style = style, color = color, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
        )
        Text("✓", style = style, color = color, textAlign = TextAlign.Center, modifier = Modifier.width(CHECK_WIDTH))
        Spacer(Modifier.width(REMOVE_WIDTH))
    }
}

@Composable
private fun SetRow(
    number: Int,
    set: ActiveSet,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    val background = if (set.completed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            number.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(SET_NUMBER_WIDTH),
        )
        NumberField(set.weight, onWeightChange, decimal = true, label = "Weight for set $number", modifier = Modifier.weight(1f))
        NumberField(set.reps, onRepsChange, decimal = false, label = "Reps for set $number", modifier = Modifier.weight(1f))
        Box(Modifier.width(CHECK_WIDTH), contentAlignment = Alignment.Center) {
            Checkbox(checked = set.completed, onCheckedChange = { onToggle() })
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(REMOVE_WIDTH)) {
            Icon(Icons.Filled.Close, contentDescription = "Remove set $number", modifier = Modifier.size(18.dp))
        }
    }
}

/** Opens the number keypad and only accepts digits (plus one decimal point for weight). */
@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    decimal: Boolean,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val cleaned = input.replace(',', '.')
            val valid = if (decimal) cleaned.matches(DECIMAL_INPUT) else cleaned.matches(INTEGER_INPUT)
            if (valid) onValueChange(cleaned)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
        placeholder = { Text("0", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        modifier = modifier.semantics { contentDescription = label },
    )
}

private val DECIMAL_INPUT = Regex("""^\d{0,4}(\.\d{0,2})?$""")
private val INTEGER_INPUT = Regex("""^\d{0,4}$""")
private val SET_NUMBER_WIDTH = 32.dp
private val CHECK_WIDTH = 48.dp
private val REMOVE_WIDTH = 32.dp
