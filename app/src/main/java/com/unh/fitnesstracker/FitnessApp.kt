package com.unh.fitnesstracker

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unh.fitnesstracker.data.repository.AuthRepository
import com.unh.fitnesstracker.data.repository.ExerciseRepository
import com.unh.fitnesstracker.data.repository.TemplateRepository
import com.unh.fitnesstracker.data.repository.UserRepository
import com.unh.fitnesstracker.data.repository.WorkoutRepository
import com.unh.fitnesstracker.data.session.ActiveWorkoutSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FitnessApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Creates each repository once and shares it with every screen. */
class AppContainer(context: Context) {

    /** Lives as long as the app, for writes that must finish even if the screen closes. */
    val appScope = CoroutineScope(SupervisorJob())

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    val authRepository by lazy { AuthRepository(FirebaseAuth.getInstance()) }
    val userRepository by lazy { UserRepository(firestore) }
    val workoutRepository by lazy { WorkoutRepository(firestore) }
    val exerciseRepository by lazy { ExerciseRepository(firestore) }
    val templateRepository by lazy { TemplateRepository(firestore) }

    /** The strength workout in progress, shared by S9–S12 and Home. */
    val workoutSession = ActiveWorkoutSession(context.applicationContext, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    fun markOnboardingComplete(uid: String) {
        appScope.launch {
            runCatching { userRepository.setOnboardingComplete(uid) }
                .onFailure { Log.w("AppContainer", "Could not save onboarding flag", it) }
        }
    }
}
