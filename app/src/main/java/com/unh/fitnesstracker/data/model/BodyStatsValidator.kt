package com.unh.fitnesstracker.data.model

/** Shared by Sign Up (S3) and Edit Profile (S28). Values are checked in metric. */
object BodyStatsValidator {
    val AGE_RANGE = 10..120
    val HEIGHT_CM_RANGE = 50.0..250.0
    val WEIGHT_KG_RANGE = 20.0..400.0

    fun ageError(age: String): String? =
        if (age.isNotEmpty() && age.toIntOrNull()?.let { it in AGE_RANGE } != true) "10–120" else null

    fun heightError(heightCm: Double?, raw: String): String? =
        if (raw.isNotEmpty() && heightCm?.let { it in HEIGHT_CM_RANGE } != true) "Enter a real height" else null

    fun weightError(weightKg: Double?, raw: String): String? =
        if (raw.isNotEmpty() && weightKg?.let { it in WEIGHT_KG_RANGE } != true) "Enter a real weight" else null
}
