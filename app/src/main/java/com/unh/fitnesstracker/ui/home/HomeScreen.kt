package com.unh.fitnesstracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Temporary S5 Home. The full dashboard, drawer and bottom tabs arrive in Sprint 2. */
@Composable
fun HomeScreen(displayName: String?, onLogOut: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val firstName = displayName?.substringBefore(' ')?.takeIf { it.isNotBlank() }
            Text(
                text = if (firstName != null) "Hi, $firstName" else "Hi there",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text("You're logged in. The dashboard is coming in Sprint 2.")
            Button(onClick = onLogOut) { Text("Log out") }
        }
    }
}
