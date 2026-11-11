package com.unh.fitnesstracker.ui.goals

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unh.fitnesstracker.data.model.Goal
import com.unh.fitnesstracker.data.model.GoalStatus
import com.unh.fitnesstracker.ui.main.MainScreenScaffold
import com.unh.fitnesstracker.ui.main.TopBarNav

/** S21 Goals list. */
@Composable
fun GoalsScreen(viewModel: GoalsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var editor by rememberSaveable { mutableStateOf<String?>(null) }
    var showCompleted by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    val editing = editor?.let { id ->
        if (id == NEW) null else state.active.plus(state.completed).firstOrNull { it.goal.id == id }?.goal
    }

    MainScreenScaffold(
        title = "Goals",
        nav = TopBarNav.Back(onBack),
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            FloatingActionButton(onClick = { editor = NEW }) {
                Icon(Icons.Filled.Add, contentDescription = "New goal")
            }
        },
    ) { padding ->
        if (state.active.isEmpty() && state.completed.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("No goals yet", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Set a weekly workout goal and it will update as you train.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { editor = NEW }) { Text("New goal") }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.active, key = { it.goal.id }) { item ->
                    GoalCard(
                        item = item,
                        paused = item.goal.status == GoalStatus.PAUSED,
                        targetText = GoalsViewModel.displayTarget(item.goal, state.units),
                        onEdit = { editor = item.goal.id },
                        onPause = { viewModel.pause(item.goal) },
                        onDelete = { viewModel.delete(item.goal) },
                    )
                }
                if (state.completed.isNotEmpty()) {
                    item {
                        TextButton(onClick = { showCompleted = !showCompleted }) {
                            Icon(
                                if (showCompleted) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                            )
                            Text("Completed (${state.completed.size})")
                        }
                    }
                    if (showCompleted) {
                        items(state.completed, key = { it.goal.id }) { item ->
                            GoalCard(
                                item = item,
                                paused = false,
                                targetText = GoalsViewModel.displayTarget(item.goal, state.units),
                                onEdit = { editor = item.goal.id },
                                onPause = {},
                                onDelete = { viewModel.delete(item.goal) },
                                showPause = false,
                            )
                        }
                    }
                }
            }
        }
    }

    if (editor != null) {
        GoalEditorDialog(
            title = if (editor == NEW) "New goal" else "Edit goal",
            initial = if (editor == NEW) null else editing,
            units = state.units,
            onSave = { draft -> viewModel.save(draft, if (editor == NEW) "" else editor.orEmpty()) },
            onDismiss = { editor = null },
        )
    }
}

@Composable
private fun GoalCard(
    item: GoalItem,
    paused: Boolean,
    targetText: String,
    onEdit: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit,
    showPause: Boolean = true,
) {
    var menu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${item.goal.type.label} · ${item.goal.period.label}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(targetText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit() })
                    if (showPause) {
                        DropdownMenuItem(
                            text = { Text(if (paused) "Resume" else "Pause") },
                            onClick = { menu = false; onPause() },
                        )
                    }
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                }
            }
            if (paused) {
                Text("Paused", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LinearProgressIndicator(progress = { item.progress.ratio }, modifier = Modifier.fillMaxWidth())
                Text(item.progress.caption, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private const val NEW = "new"
