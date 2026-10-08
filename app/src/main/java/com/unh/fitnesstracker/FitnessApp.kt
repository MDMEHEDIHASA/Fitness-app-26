package com.unh.fitnesstracker

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unh.fitnesstracker.data.repository.AuthRepository
import com.unh.fitnesstracker.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
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
class AppContainer(app: Application) {

    /** False when app/google-services.json is missing, so Firebase never initialized. */
    val isFirebaseConfigured: Boolean = FirebaseApp.getApps(app).isNotEmpty()

    /** Lives as long as the app, for writes that must finish even if the screen closes. */
    val appScope = CoroutineScope(SupervisorJob())

    val authRepository by lazy { AuthRepository(FirebaseAuth.getInstance()) }
    val userRepository by lazy { UserRepository(FirebaseFirestore.getInstance()) }

    fun markOnboardingComplete(uid: String) {
        appScope.launch {
            runCatching { userRepository.setOnboardingComplete(uid) }
                .onFailure { Log.w("AppContainer", "Could not save onboarding flag", it) }
        }
    }
}
