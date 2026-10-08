package com.unh.fitnesstracker.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

private data class PermissionItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val permissions: List<String>,
)

/** Android 16 (API 36) replaced BODY_SENSORS with the Health Connect heart-rate permission. */
private val heartRatePermission =
    if (Build.VERSION.SDK_INT >= 36) "android.permission.health.READ_HEART_RATE"
    else Manifest.permission.BODY_SENSORS

private val permissionItems = listOf(
    PermissionItem("Heart rate", "Health Connect / watch", Icons.Filled.Favorite, listOf(heartRatePermission)),
    PermissionItem(
        "Steps", "Phone step counter", Icons.AutoMirrored.Filled.DirectionsWalk,
        listOf(Manifest.permission.ACTIVITY_RECOGNITION),
    ),
    PermissionItem(
        "Location", "Outdoor run & walk maps", Icons.Filled.LocationOn,
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
    ),
    PermissionItem(
        "Notifications", "Workout reminders", Icons.Filled.Notifications,
        listOf(Manifest.permission.POST_NOTIFICATIONS),
    ),
)

private fun Context.isGranted(item: PermissionItem) =
    item.permissions.any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

/** S4 Health & Permissions setup. Continue and "Skip for now" both go to Home. */
@Composable
fun PermissionsScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    // Bumped whenever a result comes back or the user returns from Settings, to re-check grants.
    var refreshKey by remember { mutableIntStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshKey++ }

    LifecycleResumeEffect(Unit) {
        refreshKey++
        onPauseOrDispose { }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Connect your health data", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Allow what you want to use now. You can change these later in Profile > Permissions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))

            permissionItems.forEach { item ->
                val granted = remember(refreshKey) { context.isGranted(item) }
                PermissionRow(
                    item = item,
                    granted = granted,
                    onAllow = { launcher.launch(item.permissions.toTypedArray()) },
                )
            }

            // Watch pairing (S26) is built in the Smartwatch sprint.
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Watch, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Pair a smartwatch (coming soon)")
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onFinished,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) { Text("Continue") }
            TextButton(onClick = onFinished, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Skip for now")
            }
        }
    }
}

@Composable
private fun PermissionRow(item: PermissionItem, granted: Boolean, onAllow: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (granted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Allowed", color = MaterialTheme.colorScheme.primary)
                }
            } else {
                FilledTonalButton(onClick = onAllow) { Text("Allow") }
            }
        }
    }
}
