package com.unh.fitnesstracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.unh.fitnesstracker.data.model.BodyWeightEntry
import com.unh.fitnesstracker.data.model.PersonalRecord
import kotlinx.coroutines.flow.Flow

class ProgressRepository(private val db: FirebaseFirestore) {

    private fun records(uid: String) =
        db.collection(UserRepository.USERS).document(uid).collection(RECORDS)

    private fun weights(uid: String) =
        db.collection(UserRepository.USERS).document(uid).collection(BODY_WEIGHT)

    fun observeRecords(uid: String): Flow<List<PersonalRecord>> =
        records(uid)
            .orderBy("achievedAt", Query.Direction.DESCENDING)
            .observe { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(PersonalRecord::class.java)?.copy(id = it.id) }
            }

    fun saveRecord(uid: String, record: PersonalRecord) {
        records(uid).document(record.key).set(record.copy(id = record.key)).logFailure("save record")
    }

    fun observeBodyWeight(uid: String): Flow<List<BodyWeightEntry>> =
        weights(uid)
            .orderBy("recordedAt", Query.Direction.DESCENDING)
            .observe { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(BodyWeightEntry::class.java)?.copy(id = it.id) }
            }

    fun saveBodyWeight(uid: String, weightKg: Double, recordedAt: Long = System.currentTimeMillis()) {
        val doc = weights(uid).document()
        doc.set(BodyWeightEntry(id = doc.id, weightKg = weightKg, recordedAt = recordedAt))
            .logFailure("save body weight")
    }

    companion object {
        const val RECORDS = "personalRecords"
        const val BODY_WEIGHT = "bodyWeight"
    }
}
