package com.unh.fitnesstracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.unh.fitnesstracker.data.model.Goal
import com.unh.fitnesstracker.data.model.GoalStatus
import kotlinx.coroutines.flow.Flow

class GoalRepository(private val db: FirebaseFirestore) {

    private fun goals(uid: String) =
        db.collection(UserRepository.USERS).document(uid).collection(GOALS)

    fun observeGoals(uid: String): Flow<List<Goal>> =
        goals(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .observe { snapshot ->
                snapshot.documents.mapNotNull { it.toData(Goal::class.java)?.copy(id = it.id) }
            }

    fun save(uid: String, goal: Goal): Goal {
        val doc = if (goal.id.isEmpty()) goals(uid).document() else goals(uid).document(goal.id)
        val saved = goal.copy(id = doc.id)
        doc.set(saved).logFailure("save goal")
        return saved
    }

    fun delete(uid: String, goalId: String) {
        goals(uid).document(goalId).delete().logFailure("delete goal")
    }

    fun setStatus(uid: String, goalId: String, status: GoalStatus, completedAt: Long? = null) {
        val data = mutableMapOf<String, Any>("status" to status.name)
        if (completedAt != null) data["completedAt"] = completedAt
        goals(uid).document(goalId).set(data, SetOptions.merge()).logFailure("update goal")
    }

    companion object {
        const val GOALS = "goals"
    }
}
