package com.unh.fitnesstracker.data.model

import com.google.firebase.firestore.Exclude

/**
 * Stored in Firestore at users/{uid}.
 * Firestore needs a no-argument constructor, so every field has a default value.
 * Body stats are always stored in metric; [units] only changes how they are displayed.
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
    val units: String = UnitSystem.METRIC.id,
    val defaultRestSec: Int = DEFAULT_REST_SEC,
) {
    @get:Exclude
    val firstName: String
        get() = fullName.trim().substringBefore(' ')

    @get:Exclude
    val unitSystem: UnitSystem
        get() = UnitSystem.fromId(units)

    companion object {
        const val DEFAULT_REST_SEC = 90
    }
}
