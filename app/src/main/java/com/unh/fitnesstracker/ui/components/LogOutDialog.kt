package com.unh.fitnesstracker.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** S29 Log out confirmation, opened from the drawer (S6) or Profile (S28). */
@Composable
fun LogOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log out?") },
        text = {
            Text("Your workouts are saved to your account. The watch will stop syncing until you log back in.")
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Log Out") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
