package com.unh.fitnesstracker.ui.workout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unh.fitnesstracker.data.model.UnitSystem
import com.unh.fitnesstracker.data.model.Workout
import com.unh.fitnesstracker.ui.main.MainScreenScaffold
import com.unh.fitnesstracker.ui.main.TopBarNav
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

private const val UNDO_MS = 5_000L

/** S17 history list, with the S31 empty state when nothing is saved yet. */
@Composable
fun WorkoutsScreen(
    viewModel: WorkoutsViewModel,
    onMenuClick: () -> Unit,
    onStartWorkout: () -> Unit,
    onOpenWorkout: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var undoToken by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()

    LaunchedEffect(listState, state.canLoadMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { last ->
                if (state.canLoadMore && last >= listState.layoutInfo.totalItemsCount - 3) {
                    viewModel.loadMore()
                }
            }
    }

    LaunchedEffect(undoToken) {
        if (undoToken == 0) return@LaunchedEffect
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
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
    }

    val pendingWorkout = pendingDeleteId?.let { id ->
        state.sections.asSequence().flatMap { it.workouts.asSequence() }.firstOrNull { it.id == id }
    }

    MainScreenScaffold(
        title = "Workouts",
        nav = TopBarNav.Menu(onMenuClick),
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            FloatingActionButton(onClick = onStartWorkout) {
                Icon(Icons.Filled.Add, contentDescription = "Start a workout")
            }
        },
    ) { padding ->
        if (state.emptyHistory) {
            EmptyHistory(Modifier.padding(padding), onStartWorkout)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true,
                        label = { Text("Search") },
                        placeholder = { Text("Name, exercise or notes") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(HistoryFilter.entries.toList(), key = { it.name }) { chip ->
                            FilterChip(
                                selected = state.filter == chip,
                                onClick = { viewModel.setFilter(chip) },
                                label = { Text(chip.label) },
                            )
                        }
                    }
                }
                if (state.noMatches) {
                    item {
                        Text(
                            "No matching workouts",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
                state.sections.forEach { section ->
                    item(key = "month-${section.title}") {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                        )
                    }
                    items(section.workouts, key = { it.id }) { workout ->
                        WorkoutRow(
                            workout = workout,
                            units = state.units,
                            onClick = { onOpenWorkout(workout.id) },
                            onLongClick = { pendingDeleteId = workout.id },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }

    if (pendingWorkout != null) {
        DeleteWorkoutDialog(
            workoutName = pendingWorkout.name,
            onConfirm = {
                viewModel.delete(pendingWorkout)
                pendingDeleteId = null
                undoToken += 1
            },
            onDismiss = { pendingDeleteId = null },
        )
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier, onStartWorkout: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkoutRow(
    workout: Workout,
    units: UnitSystem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(workout.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    WorkoutFormat.typeLabel(workout.type),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                WorkoutFormat.listSubtitle(workout, units),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
