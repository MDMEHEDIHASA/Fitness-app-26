package com.unh.fitnesstracker.ui.main

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val FEEDBACK_EMAIL = "mmehedihasan514@gmail.com"

/** Help & Feedback, opened from the drawer (S6). */
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    MainScreenScaffold(title = "Help & Feedback", nav = TopBarNav.Back(onBack)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("How do I start a workout?", style = MaterialTheme.typography.titleMedium)
            Text("Tap the + button on Home or Workouts and pick a workout type.")
            Text("Where is my data stored?", style = MaterialTheme.typography.titleMedium)
            Text("In your Firebase account, so it syncs to any phone you log in on.")
            Text("Can I use the app without a watch?", style = MaterialTheme.typography.titleMedium)
            Text("Yes. Steps come from the phone's sensor; heart rate shows \"--\" until a watch is paired.")

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$FEEDBACK_EMAIL"))
                        .putExtra(Intent.EXTRA_SUBJECT, "Fitness Tracker feedback")
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, "No email app installed", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Send feedback") }
        }
    }
}
