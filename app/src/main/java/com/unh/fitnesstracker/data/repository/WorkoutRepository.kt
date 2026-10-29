package com.unh.fitnesstracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.unh.fitnesstracker.data.model.Workout
import com.unh.fitnesstracker.data.model.WorkoutType
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

    /** Live view of one workout. Emits null if it was deleted. */
    fun observe(uid: String, workoutId: String): Flow<Workout?> =
        workouts(uid).document(workoutId).observe { snapshot ->
            if (!snapshot.exists()) null
            else snapshot.toObject(Workout::class.java)?.copy(id = snapshot.id)
        }

    /** Workouts that started at or after [sinceMillis], newest first. */
    fun observeSince(uid: String, sinceMillis: Long): Flow<List<Workout>> =
        workouts(uid)
            .whereGreaterThanOrEqualTo("startedAt", sinceMillis)
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .observe(::mapWorkouts)

    /**
     * Newest [limit] workouts, optionally of one [type]. Filtering by type needs the composite
     * index in `docs/firestore.indexes.json` (type + startedAt).
     */
    fun observeHistory(uid: String, type: WorkoutType?, limit: Long): Flow<List<Workout>> {
        var query: Query = workouts(uid)
        if (type != null) query = query.whereEqualTo("type", type.name)
        return query
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .limit(limit)
            .observe(::mapWorkouts)
    }

    private fun mapWorkouts(snapshot: QuerySnapshot): List<Workout> =
        snapshot.documents.mapNotNull { it.toObject(Workout::class.java)?.copy(id = it.id) }

    companion object {
        const val WORKOUTS = "workouts"
        const val PAGE_SIZE = 20L
    }
}
