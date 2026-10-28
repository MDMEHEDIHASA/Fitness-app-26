package com.unh.fitnesstracker.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unh.fitnesstracker.data.model.MuscleGroup
import com.unh.fitnesstracker.data.model.TrackedBy
import com.unh.fitnesstracker.ui.main.MainScreenScaffold
import com.unh.fitnesstracker.ui.main.TopBarNav

/** S10 Add Exercise: search, filter by muscle group, pick several, then add them to S9 in one go. */
@Composable
fun AddExerciseScreen(viewModel: AddExerciseViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }

    MainScreenScaffold(title = "Add Exercise", nav = TopBarNav.Back(onBack)) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Search exercises") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.group == null,
                        onClick = { viewModel.onGroupSelected(null) },
                        label = { Text("All") },
                    )
                }
                items(MuscleGroup.entries) { group ->
                    FilterChip(
                        selected = state.group == group,
                        onClick = { viewModel.onGroupSelected(group) },
                        label = { Text(group.label) },
                    )
                }
            }

            LazyColumn(Modifier.weight(1f)) {
                item {
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        headlineContent = { Text("Create custom exercise", color = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { showCreate = true },
                    )
                    HorizontalDivider()
                }
                if (state.exercises.isEmpty()) {
                    item {
                        Text(
                            "No exercises match. Try another search or create a custom one.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
                items(state.exercises, key = { it.id }) { exercise ->
                    val selected = exercise.id in state.selectedIds
                    ListItem(
                        headlineContent = { Text(exercise.name) },
                        supportingContent = {
                            Text(
                                buildString {
                                    append(exercise.muscleGroup.label)
                                    if (exercise.trackedBy == TrackedBy.TIME) append(" · Timed")
                                    if (exercise.custom) append(" · Custom")
                                },
                            )
                        },
                        trailingContent = { Checkbox(checked = selected, onCheckedChange = { viewModel.toggle(exercise.id) }) },
                        modifier = Modifier.clickable { viewModel.toggle(exercise.id) },
                    )
                }
            }

            val count = state.selectedIds.size
            Button(
                onClick = {
                    viewModel.addSelected()
                    onBack()
                },
                enabled = count > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    when (count) {
                        0 -> "Select exercises"
                        1 -> "Add 1 Exercise"
                        else -> "Add $count Exercises"
                    },
                )
            }
        }
    }

    if (showCreate) {
        CreateExerciseDialog(
            initialName = state.query.trim(),
            initialGroup = state.group ?: MuscleGroup.CHEST,
            onCreate = { name, group, trackedBy ->
                viewModel.createCustom(name, group, trackedBy)
                showCreate = false
            },
            onDismiss = { showCreate = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateExerciseDialog(
    initialName: String,
    initialGroup: MuscleGroup,
    onCreate: (String, MuscleGroup, TrackedBy) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var group by rememberSaveable { mutableStateOf(initialGroup) }
    var trackedBy by rememberSaveable { mutableStateOf(TrackedBy.REPS) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Muscle group", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MuscleGroup.entries.forEach { option ->
                        FilterChip(selected = group == option, onClick = { group = option }, label = { Text(option.label) })
                    }
                }
                Text("Track by", style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    TrackedBy.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = trackedBy == option,
                            onClick = { trackedBy = option },
                            shape = SegmentedButtonDefaults.itemShape(index, TrackedBy.entries.size),
                        ) {
                            Text(if (option == TrackedBy.REPS) "Weight × reps" else "Time")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, group, trackedBy) }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
