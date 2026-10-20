package com.unh.fitnesstracker.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.unh.fitnesstracker.ui.navigation.Routes

data class BottomTab(val route: String, val label: String, val icon: ImageVector)

val bottomTabs = listOf(
    BottomTab(Routes.HOME, "Home", Icons.Filled.Home),
    BottomTab(Routes.WORKOUTS, "Workouts", Icons.Filled.FitnessCenter),
    BottomTab(Routes.PROGRESS, "Progress", Icons.AutoMirrored.Filled.ShowChart),
    BottomTab(Routes.PROFILE, "Profile", Icons.Filled.Person),
)

val bottomTabRoutes = bottomTabs.map { it.route }.toSet()

@Composable
fun BottomBar(currentRoute: String?, onTabClick: (String) -> Unit) {
    NavigationBar {
        bottomTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onTabClick(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}
