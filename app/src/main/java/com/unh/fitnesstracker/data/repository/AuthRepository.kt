package com.unh.fitnesstracker.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

/** Result of a Google sign-in. [isNewUser] decides whether to show S4 onboarding. */
data class GoogleSignInResult(val user: FirebaseUser, val isNewUser: Boolean)

class AuthRepository(private val auth: FirebaseAuth) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signIn(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        return requireNotNull(result.user)
    }

    suspend fun signUp(fullName: String, email: String, password: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = requireNotNull(result.user)
        val profileUpdate = UserProfileChangeRequest.Builder().setDisplayName(fullName).build()
        user.updateProfile(profileUpdate).await()
        return user
    }

    suspend fun signInWithGoogle(idToken: String): GoogleSignInResult {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return GoogleSignInResult(
            user = requireNotNull(result.user),
            isNewUser = result.additionalUserInfo?.isNewUser == true,
        )
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    fun signOut() = auth.signOut()
}

/** Turns Firebase exceptions into the messages shown in the wireframes (S2). */
fun Throwable.toAuthMessage(): String = when (this) {
    is FirebaseNetworkException -> "No internet connection. Try again."
    is FirebaseAuthWeakPasswordException -> "Password is too weak. Use at least 8 characters."
    is FirebaseAuthUserCollisionException -> "An account with this email already exists."
    is FirebaseAuthInvalidUserException,
    is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password"
    is FirebaseAuthException if errorCode == "ERROR_OPERATION_NOT_ALLOWED" ->
        "This sign-in method is turned off. Enable it in Firebase Authentication > Sign-in method."
    else -> localizedMessage ?: "Something went wrong. Try again."
}
