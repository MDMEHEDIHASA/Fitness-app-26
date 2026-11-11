package com.unh.fitnesstracker.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unh.fitnesstracker.data.model.ProgressMetric
import com.unh.fitnesstracker.data.model.ProgressRange
import com.unh.fitnesstracker.data.model.UnitSystem
import com.unh.fitnesstracker.data.model.Units
import com.unh.fitnesstracker.ui.components.BarChart
import com.unh.fitnesstracker.ui.components.LineChart
import com.unh.fitnesstracker.ui.main.MainScreenScaffold
import com.unh.fitnesstracker.ui.main.TopBarNav

/** S19 / S20 Progress: week bars, month/year line, metric menu, records. */
@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel,
    onMenuClick: () -> Unit,
    onOpenRecords: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MainScreenScaffold(title = "Progress", nav = TopBarNav.Menu(onMenuClick)) { padding ->
        if (state.empty) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No progress yet", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Log a workout or your body weight to see charts here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@MainScreenScaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ProgressRange.entries.forEachIndexed { index, value ->
                        SegmentedButton(
                            selected = state.range == value,
                            onClick = { viewModel.setRange(value) },
                            shape = SegmentedButtonDefaults.itemShape(index, ProgressRange.entries.size),
                        ) { Text(value.label) }
                    }
                }
            }
            item { MetricMenu(state.metric, viewModel::setMetric) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val selected = state.selected
                        Text(
                            if (selected != null) "${selected.label}: ${state.metric.formatValue(selected.value, state.units)}"
                            else "Tap a ${if (state.range == ProgressRange.WEEK) "bar" else "point"} for the value",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (state.range == ProgressRange.WEEK) {
                            BarChart(state.points, state.selectedIndex, viewModel::select, goal = state.goalLine)
                        } else {
                            LineChart(state.points, state.selectedIndex, viewModel::select, goal = state.goalLine)
                        }
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SummaryCard("Workouts", state.workouts.toString(), Modifier.weight(1f))
                    SummaryCard("Resting HR", state.restingHr?.let { "$it" } ?: "--", Modifier.weight(1f))
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SummaryCard("Distance", Units.formatDistance(state.distanceM, state.units), Modifier.weight(1f))
                    SummaryCard("Active min", state.activeMinutes.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Personal records", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenRecords) {
                        Text("See all")
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
            if (state.records.isEmpty()) {
                item { Text("Break a record by logging a heavier set or more volume.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(state.records.take(4), key = { it.id }) { record ->
                    RecordRow(record.type.label, formatRecord(record, state.units))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricMenu(selected: ProgressMetric, onSelect: (ProgressMetric) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Metric") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ProgressMetric.entries.forEach { metric ->
                DropdownMenuItem(
                    text = { Text(metric.label) },
                    onClick = {
                        onSelect(metric)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun RecordRow(title: String, value: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

internal fun formatRecord(record: com.unh.fitnesstracker.data.model.PersonalRecord, units: UnitSystem): String {
    val name = record.exerciseName.takeIf { it.isNotEmpty() }?.let { "$it · " }.orEmpty()
    val amount = when (record.type) {
        com.unh.fitnesstracker.data.model.RecordType.HEAVIEST_LIFT,
        com.unh.fitnesstracker.data.model.RecordType.MOST_VOLUME,
        -> Units.formatWeight(record.value, units) ?: "0"
        com.unh.fitnesstracker.data.model.RecordType.LONGEST_RUN -> Units.formatDistance(record.value, units)
        com.unh.fitnesstracker.data.model.RecordType.FASTEST_5K -> {
            val total = record.value.toLong()
            "%d:%02d".format(total / 60, total % 60)
        }
    }
    return name + amount
}
