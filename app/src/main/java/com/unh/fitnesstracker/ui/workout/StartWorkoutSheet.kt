package com.unh.fitnesstracker.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.unh.fitnesstracker.data.model.WorkoutTemplate
import com.unh.fitnesstracker.ui.navigation.OutdoorType

/** S7 Start Workout bottom sheet, opened by the + button on Home or Workouts. Swipe down to close. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartWorkoutSheet(
    watchConnected: Boolean,
    templates: List<WorkoutTemplate>,
    workoutInProgress: Boolean,
    onDismiss: () -> Unit,
    onStrengthClick: () -> Unit,
    onTemplateClick: (WorkoutTemplate) -> Unit,
    onOutdoorClick: (OutdoorType) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                "Start a workout",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )

            // ① Strength
            WorkoutTypeRow(
                Icons.Filled.FitnessCenter,
                "Strength training",
                if (workoutInProgress) "In progress · tap to resume" else "Log sets, reps & weight",
                onStrengthClick,
            )
            // ② Outdoor
            WorkoutTypeRow(Icons.AutoMirrored.Filled.DirectionsRun, OutdoorType.RUN.label, "GPS route, pace, heart rate") {
                onOutdoorClick(OutdoorType.RUN)
            }
            WorkoutTypeRow(Icons.AutoMirrored.Filled.DirectionsWalk, OutdoorType.WALK.label, "GPS route, steps") {
                onOutdoorClick(OutdoorType.WALK)
            }
            WorkoutTypeRow(Icons.AutoMirrored.Filled.DirectionsBike, OutdoorType.CYCLE.label, "GPS route, speed") {
                onOutdoorClick(OutdoorType.CYCLE)
            }

            // ③ Templates, saved from the S9 ⋮ menu
            Text(
                "From a template",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
            )
            if (templates.isEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp),
                ) {
                    Text(
                        "No templates yet · save one from a workout's ⋮ menu",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    NewChip(onStrengthClick)
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(templates, key = { it.id }) { template ->
                        AssistChip(
                            onClick = { onTemplateClick(template) },
                            label = { Text(template.name) },
                            enabled = !workoutInProgress,
                        )
                    }
                    item { NewChip(onStrengthClick) }
                }
            }

            Spacer(Modifier.height(8.dp))
            // Grayed out until a watch is paired (Sprint 10).
            ListItem(
                leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                headlineContent = { Text("Track heart rate with watch") },
                supportingContent = if (!watchConnected) {
                    { Text("Pair a watch to turn this on") }
                } else null,
                trailingContent = { Switch(checked = watchConnected, onCheckedChange = null, enabled = watchConnected) },
            )
        }
    }
}

@Composable
private fun NewChip(onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text("New") },
        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
    )
}

@Composable
private fun WorkoutTypeRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}
