package com.unh.fitnesstracker.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.unh.fitnesstracker.data.model.Workout
import com.unh.fitnesstracker.data.model.WorkoutType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map

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
            else snapshot.toData(Workout::class.java)?.copy(id = snapshot.id)
        }

    /** Every workout, newest first when [startedAt] is present. */
    fun observeAll(uid: String): Flow<List<Workout>> =
        workouts(uid).observe(::mapWorkouts)

    /** Workouts that started at or after [sinceMillis], newest first. */
    fun observeSince(uid: String, sinceMillis: Long): Flow<List<Workout>> =
        workouts(uid)
            .whereGreaterThanOrEqualTo("startedAt", sinceMillis)
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .observe(::mapWorkouts)
            .catch { e ->
                Log.w(TAG, "observeSince failed, loading unfiltered: ${e.toFirestoreMessage()}", e)
                emitAll(observeAll(uid).map { list -> list.filter { it.startedAt >= sinceMillis } })
            }

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
            .catch { e ->
                Log.w(TAG, "History query failed, loading unfiltered: ${e.toFirestoreMessage()}", e)
                emitAll(
                    observeAll(uid).map { list ->
                        val typed = if (type == null) list else list.filter { it.type == type }
                        typed.sortedByDescending { it.startedAt }.take(limit.toInt())
                    },
                )
            }
    }

    private fun mapWorkouts(snapshot: QuerySnapshot): List<Workout> =
        snapshot.documents.mapNotNull { it.toData(Workout::class.java)?.copy(id = it.id) }

    companion object {
        const val WORKOUTS = "workouts"
        const val PAGE_SIZE = 20L
        private const val TAG = "WorkoutRepository"
    }
}
