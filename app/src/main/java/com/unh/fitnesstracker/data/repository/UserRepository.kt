package com.unh.fitnesstracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.unh.fitnesstracker.data.model.UserProfile
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore) {

    private fun userDoc(uid: String) = db.collection(USERS).document(uid)

    suspend fun createProfile(profile: UserProfile) {
        userDoc(profile.uid).set(profile).await()
    }

    suspend fun getProfile(uid: String): UserProfile? =
        userDoc(uid).get().await().toObject(UserProfile::class.java)

    suspend fun setOnboardingComplete(uid: String) {
        userDoc(uid).set(mapOf("onboardingComplete" to true), SetOptions.merge()).await()
    }

    companion object {
        const val USERS = "users"
    }
}
