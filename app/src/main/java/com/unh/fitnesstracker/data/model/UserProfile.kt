package com.unh.fitnesstracker.data.model

/**
 * Stored in Firestore at users/{uid}.
 * Firestore needs a no-argument constructor, so every field has a default value.
 */
data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val age: Int? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val onboardingComplete: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
