package com.unh.fitnesstracker.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unh.fitnesstracker.data.session.RestTimer

/** S11 Rest timer. Opens when a set is checked; swiping it down hides it while the timer keeps running. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestTimerSheet(
    rest: RestTimer,
    onAdjust: (deltaSec: Int) -> Unit,
    onSkip: () -> Unit,
    onHide: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onHide,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Rest", style = MaterialTheme.typography.titleLarge)
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { if (rest.totalSec > 0) rest.remainingSec.toFloat() / rest.totalSec else 0f },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 10.dp,
                )
                Text(WorkoutFormat.clock(rest.remainingSec.toLong()), style = MaterialTheme.typography.displayMedium)
            }
            Text(
                rest.nextLabel?.let { "Next: $it" } ?: "All sets checked",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "-- bpm · heart-rate recovery needs a watch",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onAdjust(-ADJUST_SEC) }) { Text("−15s") }
                Button(onClick = onSkip) { Text("Skip") }
                OutlinedButton(onClick = { onAdjust(ADJUST_SEC) }) { Text("+15s") }
            }
        }
    }
}

private const val ADJUST_SEC = 15
