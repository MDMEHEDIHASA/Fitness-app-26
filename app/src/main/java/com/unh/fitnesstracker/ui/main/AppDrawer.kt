package com.unh.fitnesstracker.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.unh.fitnesstracker.data.model.UserProfile
import com.unh.fitnesstracker.ui.components.InitialsAvatar
import com.unh.fitnesstracker.ui.navigation.Routes

private data class DrawerEntry(val label: String, val icon: ImageVector, val route: String)

private val drawerEntries = listOf(
    DrawerEntry("Home", Icons.Filled.Home, Routes.HOME),
    DrawerEntry("Goals", Icons.Filled.Flag, Routes.GOALS),
    DrawerEntry("Reminders", Icons.Filled.Notifications, Routes.REMINDERS),
    DrawerEntry("Smartwatch", Icons.Filled.Watch, Routes.SMARTWATCH),
    DrawerEntry("Settings", Icons.Filled.Settings, Routes.PROFILE),
    DrawerEntry("Help & Feedback", Icons.AutoMirrored.Filled.HelpOutline, Routes.HELP),
)

/** S6 Navigation drawer. */
@Composable
fun AppDrawer(
    profile: UserProfile?,
    currentRoute: String?,
    watchConnected: Boolean,
    onItemClick: (String) -> Unit,
    onLogOutClick: () -> Unit,
) {
    ModalDrawerSheet {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(24.dp)) {
                InitialsAvatar(name = profile?.fullName.orEmpty(), size = 56.dp)
                Spacer(Modifier.height(12.dp))
                Text(profile?.fullName.orEmpty(), style = MaterialTheme.typography.titleMedium)
                Text(
                    profile?.email.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            drawerEntries.forEach { entry ->
                NavigationDrawerItem(
                    label = { Text(entry.label) },
                    icon = { Icon(entry.icon, contentDescription = null) },
                    selected = currentRoute == entry.route,
                    onClick = { onItemClick(entry.route) },
                    badge = if (entry.route == Routes.SMARTWATCH) {
                        { Text(if (watchConnected) "● On" else "Off") }
                    } else null,
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            NavigationDrawerItem(
                label = { Text("Log out") },
                icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                selected = false,
                onClick = onLogOutClick,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }
    }
}
