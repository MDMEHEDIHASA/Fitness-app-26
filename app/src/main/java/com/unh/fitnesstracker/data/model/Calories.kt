package com.unh.fitnesstracker.data.model

import kotlin.math.roundToInt

/** Calorie estimate: MET × body weight (kg) × hours. */
object Calories {
    /** Compendium of Physical Activities: resistance training, vigorous effort. */
    const val STRENGTH_MET = 5.0
    const val DEFAULT_BODY_WEIGHT_KG = 70.0

    fun estimate(met: Double, bodyWeightKg: Double, durationSec: Long): Int =
        (met * bodyWeightKg * durationSec / 3600.0).roundToInt()
}
