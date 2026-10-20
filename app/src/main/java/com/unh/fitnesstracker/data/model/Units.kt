package com.unh.fitnesstracker.data.model

import kotlin.math.roundToInt

enum class UnitSystem(val id: String, val label: String) {
    METRIC("metric", "kg / km"),
    IMPERIAL("imperial", "lb / mi");

    companion object {
        fun fromId(id: String?): UnitSystem = entries.firstOrNull { it.id == id } ?: METRIC
    }
}

object Units {
    private const val CM_PER_INCH = 2.54
    private const val LB_PER_KG = 2.20462

    fun cmToInches(cm: Double) = cm / CM_PER_INCH
    fun inchesToCm(inches: Double) = inches * CM_PER_INCH
    fun kgToLb(kg: Double) = kg * LB_PER_KG
    fun lbToKg(lb: Double) = lb / LB_PER_KG

    fun formatHeight(cm: Double?, system: UnitSystem): String? = cm?.let {
        when (system) {
            UnitSystem.METRIC -> "${it.roundToInt()} cm"
            UnitSystem.IMPERIAL -> {
                val totalInches = cmToInches(it).roundToInt()
                "${totalInches / 12}'${totalInches % 12}\""
            }
        }
    }

    fun formatWeight(kg: Double?, system: UnitSystem): String? = kg?.let {
        when (system) {
            UnitSystem.METRIC -> "${it.roundToInt()} kg"
            UnitSystem.IMPERIAL -> "${kgToLb(it).roundToInt()} lb"
        }
    }

    /** Height in the unit the user edits it in: cm, or total inches for imperial. */
    fun heightForInput(cm: Double?, system: UnitSystem): String = cm?.let {
        when (system) {
            UnitSystem.METRIC -> it.roundToInt().toString()
            UnitSystem.IMPERIAL -> cmToInches(it).roundToInt().toString()
        }
    }.orEmpty()

    fun weightForInput(kg: Double?, system: UnitSystem): String = kg?.let {
        when (system) {
            UnitSystem.METRIC -> it.roundToInt().toString()
            UnitSystem.IMPERIAL -> kgToLb(it).roundToInt().toString()
        }
    }.orEmpty()
}
