package com.unh.fitnesstracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.unh.fitnesstracker.data.model.WorkoutTemplate
import kotlinx.coroutines.flow.Flow

class TemplateRepository(private val db: FirebaseFirestore) {

    private fun templates(uid: String) =
        db.collection(UserRepository.USERS).document(uid).collection(TEMPLATES)

    fun observeTemplates(uid: String): Flow<List<WorkoutTemplate>> =
        templates(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .observe { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(WorkoutTemplate::class.java)?.copy(id = it.id) }
            }

    fun save(uid: String, template: WorkoutTemplate) {
        val doc = templates(uid).document()
        doc.set(template.copy(id = doc.id)).logFailure("save template")
    }

    companion object {
        const val TEMPLATES = "templates"
    }
}
