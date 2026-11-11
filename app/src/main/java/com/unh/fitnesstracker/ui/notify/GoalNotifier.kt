package com.unh.fitnesstracker.ui.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.unh.fitnesstracker.R
import com.unh.fitnesstracker.data.model.Goal

/** Posts a one-shot notification when a goal is marked completed. Reminders themselves are Sprint 7. */
class GoalNotifier(context: Context) {

    private val app = context.applicationContext

    init {
        val manager = app.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Goals", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "When you reach a goal target"
            },
        )
    }

    fun goalCompleted(goal: Goal) {
        val notification = NotificationCompat.Builder(app, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Goal reached")
            .setContentText("${goal.type.label} · ${goal.period.label.lowercase()} target hit")
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(app).notify(goal.id.hashCode(), notification)
        }
    }

    private companion object {
        const val CHANNEL = "goals"
    }
}
