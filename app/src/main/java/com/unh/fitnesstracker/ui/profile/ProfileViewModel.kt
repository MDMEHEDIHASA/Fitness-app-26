package com.unh.fitnesstracker.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unh.fitnesstracker.data.model.UnitSystem
import com.unh.fitnesstracker.data.model.UserProfile
import com.unh.fitnesstracker.data.repository.AuthRepository
import com.unh.fitnesstracker.data.repository.ProgressRepository
import com.unh.fitnesstracker.data.repository.UserRepository
import com.unh.fitnesstracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch

/** Body stats from the Edit Profile dialog, already converted to metric. */
data class ProfileEdit(val fullName: String, val age: Int?, val heightCm: Double?, val weightKg: Double?)

/** Shared by the drawer header (S6) and Profile & Settings (S28). */
data class ProfileStats(val workouts: Int = 0, val streakDays: Int = 0, val distanceM: Double = 0.0)

class ProfileViewModel(
    private val uid: String,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val progressRepository: ProgressRepository,
    workoutRepository: WorkoutRepository,
) : ViewModel() {

    val profile: StateFlow<UserProfile?> = userRepository.observeProfile(uid)
        // A listener error (e.g. right after sign-out) shouldn't crash the app.
        .catch { e ->
            Log.w(TAG, "Profile listener failed", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<String?>(null)
    /** One-time snackbar text. */
    val message: StateFlow<String?> = _message.asStateFlow()

    val stats: StateFlow<ProfileStats> = workoutRepository.observeSince(uid, 0)
        .catch { e ->
            Log.w(TAG, "Workouts failed", e)
            emit(emptyList())
        }
        .combine(profile) { workouts, _ ->
            ProfileStats(
                workouts = workouts.size,
                streakDays = streak(workouts.map { it.startedAt }),
                distanceM = workouts.sumOf { it.distanceM },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileStats())

    fun saveProfile(edit: ProfileEdit) = runAndReport("Profile updated") {
        userRepository.updateBasics(uid, edit.fullName, edit.age, edit.heightCm, edit.weightKg)
        authRepository.updateDisplayName(edit.fullName)
        edit.weightKg?.let { progressRepository.saveBodyWeight(uid, it) }
    }

    fun setUnits(system: UnitSystem) = runAndReport("Units set to ${system.label}") {
        userRepository.updateUnits(uid, system.id)
    }

    fun setDefaultRest(seconds: Int) = runAndReport("Default rest set to $seconds s") {
        userRepository.updateDefaultRest(uid, seconds)
    }

    fun showMessage(text: String) {
        _message.value = text
    }

    fun onMessageShown() {
        _message.value = null
    }

    private fun runAndReport(successText: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { _message.value = successText }
                .onFailure { e ->
                    Log.w(TAG, "Profile update failed", e)
                    _message.value = "Couldn't save. Check your connection and try again."
                }
        }
    }

    private fun streak(startedAt: List<Long>): Int {
        if (startedAt.isEmpty()) return 0
        val zone = ZoneId.systemDefault()
        val days = startedAt.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.toSet()
        var cursor = LocalDate.now()
        if (cursor !in days) cursor = cursor.minusDays(1)
        var count = 0
        while (cursor in days) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    private companion object {
        const val TAG = "ProfileViewModel"
    }
}
