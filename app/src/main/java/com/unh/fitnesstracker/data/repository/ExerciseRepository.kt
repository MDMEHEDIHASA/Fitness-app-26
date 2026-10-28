package com.unh.fitnesstracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.unh.fitnesstracker.data.model.Exercise
import com.unh.fitnesstracker.data.model.ExerciseLibrary
import com.unh.fitnesstracker.data.model.MuscleGroup
import com.unh.fitnesstracker.data.model.TrackedBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class ExerciseRepository(private val db: FirebaseFirestore) {

    private fun customExercises(uid: String) =
        db.collection(UserRepository.USERS).document(uid).collection(CUSTOM_EXERCISES)

    /** Built-in library plus the user's custom exercises, sorted by name. */
    fun observeAll(uid: String): Flow<List<Exercise>> =
        customExercises(uid)
            .observe { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Exercise::class.java)?.copy(id = doc.id, custom = true)
                }
            }
            .onStart { emit(emptyList()) }
            .catch { emit(emptyList()) }
            .map { custom -> (ExerciseLibrary.all + custom).sortedBy { it.name.lowercase() } }

    fun addCustom(uid: String, name: String, muscleGroup: MuscleGroup, trackedBy: TrackedBy): Exercise {
        val doc = customExercises(uid).document()
        doc.set(mapOf("name" to name, "muscleGroup" to muscleGroup.name, "trackedBy" to trackedBy.name))
            .logFailure("save custom exercise")
        return Exercise(doc.id, name, muscleGroup, trackedBy, custom = true)
    }

    companion object {
        const val CUSTOM_EXERCISES = "customExercises"
    }
}
