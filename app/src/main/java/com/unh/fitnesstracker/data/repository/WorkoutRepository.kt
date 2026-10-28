package com.unh.fitnesstracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.unh.fitnesstracker.data.model.Workout
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val db: FirebaseFirestore) {

    private fun workouts(uid: String) =
        db.collection(UserRepository.USERS).document(uid).collection(WORKOUTS)

    /** Saves a new workout (or overwrites one with the same id) and returns it with its id. */
    fun save(uid: String, workout: Workout): Workout {
        val doc = if (workout.id.isEmpty()) workouts(uid).document() else workouts(uid).document(workout.id)
        val saved = workout.copy(id = doc.id)
        doc.set(saved).logFailure("save workout")
        return saved
    }

    fun delete(uid: String, workoutId: String) {
        workouts(uid).document(workoutId).delete().logFailure("delete workout")
    }

    /** Workouts that started at or after [sinceMillis], newest first. */
    fun observeSince(uid: String, sinceMillis: Long): Flow<List<Workout>> =
        workouts(uid)
            .whereGreaterThanOrEqualTo("startedAt", sinceMillis)
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .observe { snapshot -> snapshot.documents.mapNotNull { it.toObject(Workout::class.java)?.copy(id = it.id) } }

    companion object {
        const val WORKOUTS = "workouts"
    }
}
