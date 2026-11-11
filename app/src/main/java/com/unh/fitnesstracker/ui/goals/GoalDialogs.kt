package com.unh.fitnesstracker.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.unh.fitnesstracker.data.model.Goal
import com.unh.fitnesstracker.data.model.GoalPeriod
import com.unh.fitnesstracker.data.model.GoalType
import com.unh.fitnesstracker.data.model.UnitSystem
import com.unh.fitnesstracker.data.model.Units

/** S22 New / edit goal. */
@Composable
fun GoalEditorDialog(
    title: String,
    initial: Goal?,
    units: UnitSystem,
    onSave: (GoalDraft) -> String?,
    onDismiss: () -> Unit,
) {
    var type by rememberSaveable { mutableStateOf(initial?.type ?: GoalType.WORKOUTS) }
    var period by rememberSaveable { mutableStateOf(initial?.period ?: GoalPeriod.WEEKLY) }
    var target by rememberSaveable {
        mutableStateOf(initial?.let { targetForEdit(it, units) } ?: "4")
    }
    var remind by rememberSaveable { mutableStateOf(initial?.remindIfBehind ?: false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Type", style = MaterialTheme.typography.labelLarge)
                FlowChips(GoalType.entries, type, { it.label }) { type = it }
                Text("Period", style = MaterialTheme.typography.labelLarge)
                FlowChips(GoalPeriod.entries, period, { it.label }) { period = it }
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it.take(8); error = null },
                    label = { Text("Target (${GoalsViewModel.targetHint(type, units)})") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = remind, onCheckedChange = { remind = it })
                    Text("Remind me if I'm behind")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val message = onSave(GoalDraft(type, period, target, remind))
                if (message == null) onDismiss() else error = message
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> FlowChips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

private fun targetForEdit(goal: Goal, units: UnitSystem): String = when (goal.type) {
    GoalType.DISTANCE -> {
        val v = if (units == UnitSystem.IMPERIAL) Units.metersToMiles(goal.target) else Units.metersToKm(goal.target)
        trimNumber(v)
    }
    GoalType.BODY_WEIGHT -> {
        val v = if (units == UnitSystem.IMPERIAL) Units.kgToLb(goal.target) else goal.target
        trimNumber(v)
    }
    else -> trimNumber(goal.target)
}

private fun trimNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.1f".format(value)
