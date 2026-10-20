package com.unh.fitnesstracker.ui.profile

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unh.fitnesstracker.data.model.UnitSystem
import com.unh.fitnesstracker.data.model.Units
import com.unh.fitnesstracker.ui.components.InitialsAvatar
import com.unh.fitnesstracker.ui.main.MainScreenScaffold
import com.unh.fitnesstracker.ui.main.TopBarNav
import com.unh.fitnesstracker.ui.navigation.Routes

private val restOptions = listOf(30, 45, 60, 90, 120, 150, 180, 240, 300)

private enum class ProfileDialog { EDIT, UNITS, REST }

/** S28 Profile & Settings. */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onMenuClick: () -> Unit,
    onNavigate: (String) -> Unit,
    onLogOutClick: () -> Unit,
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var openDialog by rememberSaveable { mutableStateOf<ProfileDialog?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    MainScreenScaffold(
        title = "Profile",
        nav = TopBarNav.Menu(onMenuClick),
        actions = {
            TextButton(onClick = { openDialog = ProfileDialog.EDIT }, enabled = profile != null) { Text("Edit") }
        },
        snackbarHostState = snackbarHostState,
    ) { padding ->
        val p = profile
        if (p == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }
            return@MainScreenScaffold
        }

        val system = p.unitSystem
        val bodyStats = listOfNotNull(
            Units.formatHeight(p.heightCm, system),
            Units.formatWeight(p.weightKg, system),
            p.age?.let { "$it yrs" },
        ).joinToString(" · ").ifEmpty { "Not set" }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                InitialsAvatar(name = p.fullName, size = 80.dp)
                Spacer(Modifier.height(12.dp))
                Text(p.fullName.ifEmpty { "Your name" }, style = MaterialTheme.typography.titleLarge)
                Text(p.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Stats (filled in by workout history in Sprints 3–5)
            Card(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Row(Modifier.padding(vertical = 16.dp)) {
                    StatColumn("0", "Workouts", Modifier.weight(1f))
                    StatColumn("0 days", "Streak", Modifier.weight(1f))
                    StatColumn(if (system == UnitSystem.METRIC) "0 km" else "0 mi", "Total", Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(8.dp))

            // ② Settings rows
            SettingsRow("Body stats", bodyStats) { openDialog = ProfileDialog.EDIT }
            SettingsRow("Units", system.label) { openDialog = ProfileDialog.UNITS }
            SettingsRow("Default rest timer", "${p.defaultRestSec} s") { openDialog = ProfileDialog.REST }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            SettingsRow("Goals") { onNavigate(Routes.GOALS) }
            SettingsRow("Reminders") { onNavigate(Routes.REMINDERS) }
            SettingsRow("Smartwatch") { onNavigate(Routes.SMARTWATCH) }
            SettingsRow("Permissions", "Location, health, notifications") {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            }
            SettingsRow("Export / delete my data") { viewModel.showMessage("Export and delete arrive in Sprint 11") }

            // ③ Log out
            OutlinedButton(
                onClick = onLogOutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) { Text("Log Out") }
        }

        when (openDialog) {
            ProfileDialog.EDIT -> EditProfileDialog(
                profile = p,
                onSave = {
                    viewModel.saveProfile(it)
                    openDialog = null
                },
                onDismiss = { openDialog = null },
            )
            ProfileDialog.UNITS -> ChoiceDialog(
                title = "Units",
                options = UnitSystem.entries,
                selected = system,
                label = { it.label },
                onSelect = {
                    viewModel.setUnits(it)
                    openDialog = null
                },
                onDismiss = { openDialog = null },
            )
            ProfileDialog.REST -> ChoiceDialog(
                title = "Default rest timer",
                options = restOptions,
                selected = p.defaultRestSec,
                label = { "$it seconds" },
                onSelect = {
                    viewModel.setDefaultRest(it)
                    openDialog = null
                },
                onDismiss = { openDialog = null },
            )
            null -> Unit
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsRow(title: String, value: String? = null, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = value?.let { { Text(it) } },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
