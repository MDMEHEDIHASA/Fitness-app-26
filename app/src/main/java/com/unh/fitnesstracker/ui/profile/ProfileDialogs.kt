package com.unh.fitnesstracker.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.unh.fitnesstracker.data.model.BodyStatsValidator
import com.unh.fitnesstracker.data.model.UnitSystem
import com.unh.fitnesstracker.data.model.UserProfile
import com.unh.fitnesstracker.data.model.Units
import com.unh.fitnesstracker.ui.components.FormTextField

/** S28 ① Edit: name and body stats, entered in the user's chosen units. */
@Composable
fun EditProfileDialog(profile: UserProfile, onSave: (ProfileEdit) -> Unit, onDismiss: () -> Unit) {
    val system = profile.unitSystem
    var name by rememberSaveable { mutableStateOf(profile.fullName) }
    var age by rememberSaveable { mutableStateOf(profile.age?.toString().orEmpty()) }
    var height by rememberSaveable { mutableStateOf(Units.heightForInput(profile.heightCm, system)) }
    var weight by rememberSaveable { mutableStateOf(Units.weightForInput(profile.weightKg, system)) }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }

    val heightLabel = if (system == UnitSystem.METRIC) "Height (cm)" else "Height (in)"
    val weightLabel = if (system == UnitSystem.METRIC) "Weight (kg)" else "Weight (lb)"

    fun heightCm() = height.toDoubleOrNull()?.let { if (system == UnitSystem.METRIC) it else Units.inchesToCm(it) }
    fun weightKg() = weight.toDoubleOrNull()?.let { if (system == UnitSystem.METRIC) it else Units.lbToKg(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                FormTextField(
                    value = name,
                    onValueChange = { name = it; errors = errors - "name" },
                    label = "Full name",
                    error = errors["name"],
                )
                FormTextField(
                    value = age,
                    onValueChange = { age = it.filter(Char::isDigit); errors = errors - "age" },
                    label = "Age",
                    error = errors["age"],
                    keyboardType = KeyboardType.Number,
                )
                FormTextField(
                    value = height,
                    onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' }; errors = errors - "height" },
                    label = heightLabel,
                    error = errors["height"],
                    keyboardType = KeyboardType.Decimal,
                )
                FormTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' }; errors = errors - "weight" },
                    label = weightLabel,
                    error = errors["weight"],
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val found = buildMap {
                    if (name.isBlank()) put("name", "Enter your name")
                    BodyStatsValidator.ageError(age)?.let { put("age", it) }
                    BodyStatsValidator.heightError(heightCm(), height)?.let { put("height", it) }
                    BodyStatsValidator.weightError(weightKg(), weight)?.let { put("weight", it) }
                }
                if (found.isEmpty()) {
                    onSave(ProfileEdit(name.trim(), age.toIntOrNull(), heightCm(), weightKg()))
                } else {
                    errors = found
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** A simple single-choice picker, used for Units and Default rest timer. */
@Composable
fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == selected, role = Role.RadioButton) { onSelect(option) },
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(label(option), modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
