package com.unh.fitnesstracker.data.progress

import com.unh.fitnesstracker.data.model.Goal
import com.unh.fitnesstracker.data.model.GoalStatus
import com.unh.fitnesstracker.data.repository.GoalRepository
import com.unh.fitnesstracker.ui.notify.GoalNotifier
import java.util.concurrent.ConcurrentHashMap

/** Marks a goal completed at most once and posts a notification. */
class GoalCompletionTracker(
    private val goalRepository: GoalRepository,
    private val notifier: GoalNotifier,
) {
    private val notified = ConcurrentHashMap.newKeySet<String>()

    fun check(uid: String, goal: Goal, progress: GoalProgress) {
        if (goal.status != GoalStatus.ACTIVE || !progress.reached || !notified.add(goal.id)) return
        goalRepository.setStatus(uid, goal.id, GoalStatus.COMPLETED, System.currentTimeMillis())
        notifier.goalCompleted(goal)
    }
}
