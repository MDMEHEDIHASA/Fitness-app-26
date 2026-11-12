package com.unh.fitnesstracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.unh.fitnesstracker.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore) {

    private fun userDoc(uid: String) = db.collection(USERS).document(uid)

    suspend fun createProfile(profile: UserProfile) {
        userDoc(profile.uid).set(profile).await()
    }

    suspend fun getProfile(uid: String): UserProfile? =
        userDoc(uid).get().await().toData(UserProfile::class.java)

    /**
     * Creates `users/{uid}` if it is missing so Profile / Home are not stuck on a spinner
     * after email log-in or a failed sign-up write.
     */
    suspend fun ensureProfile(uid: String, fullName: String, email: String) {
        if (getProfile(uid) != null) return
        userDoc(uid).set(
            UserProfile(uid = uid, fullName = fullName, email = email, onboardingComplete = true),
            SetOptions.merge(),
        ).await()
    }

    /** Emits the profile now and again every time it changes in Firestore (on any device). */
    fun observeProfile(uid: String): Flow<UserProfile?> =
        userDoc(uid).observe { snapshot ->
            if (!snapshot.exists()) null
            else snapshot.toData(UserProfile::class.java)
        }

    suspend fun setOnboardingComplete(uid: String) {
        userDoc(uid).set(mapOf("onboardingComplete" to true), SetOptions.merge()).await()
    }

    suspend fun updateBasics(uid: String, fullName: String, age: Int?, heightCm: Double?, weightKg: Double?) {
        userDoc(uid).set(
            mapOf("fullName" to fullName, "age" to age, "heightCm" to heightCm, "weightKg" to weightKg),
            SetOptions.merge(),
        ).await()
    }

    suspend fun updateUnits(uid: String, units: String) {
        userDoc(uid).set(mapOf("units" to units), SetOptions.merge()).await()
    }

    suspend fun updateDefaultRest(uid: String, seconds: Int) {
        userDoc(uid).set(mapOf("defaultRestSec" to seconds), SetOptions.merge()).await()
    }

    companion object {
        const val USERS = "users"
    }
}
