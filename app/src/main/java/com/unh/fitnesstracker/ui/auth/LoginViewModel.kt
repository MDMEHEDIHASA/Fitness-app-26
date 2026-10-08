package com.unh.fitnesstracker.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unh.fitnesstracker.data.model.UserProfile
import com.unh.fitnesstracker.data.repository.AuthRepository
import com.unh.fitnesstracker.data.repository.UserRepository
import com.unh.fitnesstracker.data.repository.toAuthMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoginDestination { HOME, ONBOARDING }

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    /** Message in the banner at the top of S2. */
    val bannerError: String? = null,
    val isLoading: Boolean = false,
    /** One-time message, e.g. "Check your inbox". */
    val snackbarMessage: String? = null,
    val destination: LoginDestination? = null,
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // S2: the error clears as soon as the user edits a field.
    fun onEmailChange(value: String) = _uiState.update {
        it.copy(email = value, emailError = null, bannerError = null)
    }

    fun onPasswordChange(value: String) = _uiState.update {
        it.copy(password = value, passwordError = null, bannerError = null)
    }

    fun logIn() {
        val state = _uiState.value
        val emailError = validateEmail(state.email)
        val passwordError = validatePassword(state.password)
        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    bannerError = "Please fix the highlighted fields",
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, bannerError = null) }
        viewModelScope.launch {
            runCatching { authRepository.signIn(state.email, state.password) }
                .onSuccess { _uiState.update { it.copy(isLoading = false, destination = LoginDestination.HOME) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, bannerError = e.toAuthMessage()) }
                }
        }
    }

    fun sendPasswordReset() {
        val email = _uiState.value.email
        val emailError = validateEmail(email)
        if (emailError != null) {
            _uiState.update {
                it.copy(emailError = emailError, bannerError = "Enter your email to reset your password")
            }
            return
        }
        viewModelScope.launch {
            runCatching { authRepository.sendPasswordReset(email) }
                .onSuccess {
                    _uiState.update { it.copy(snackbarMessage = "Check your inbox for a reset link") }
                }
                .onFailure { e -> _uiState.update { it.copy(bannerError = e.toAuthMessage()) } }
        }
    }

    /** Called with the ID token returned by Credential Manager. First-time users go to S4. */
    fun signInWithGoogle(idToken: String) {
        _uiState.update { it.copy(isLoading = true, bannerError = null) }
        viewModelScope.launch {
            runCatching {
                val result = authRepository.signInWithGoogle(idToken)
                val existing = userRepository.getProfile(result.user.uid)
                if (existing == null) {
                    userRepository.createProfile(
                        UserProfile(
                            uid = result.user.uid,
                            fullName = result.user.displayName.orEmpty(),
                            email = result.user.email.orEmpty(),
                        )
                    )
                }
                if (existing?.onboardingComplete == true) LoginDestination.HOME else LoginDestination.ONBOARDING
            }
                .onSuccess { dest -> _uiState.update { it.copy(isLoading = false, destination = dest) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, bannerError = e.toAuthMessage()) }
                }
        }
    }

    fun onGoogleSignInError(message: String) = _uiState.update { it.copy(bannerError = message) }

    fun onSnackbarShown() = _uiState.update { it.copy(snackbarMessage = null) }

    fun onNavigated() = _uiState.update { it.copy(destination = null) }
}

internal fun validateEmail(email: String): String? = when {
    email.isBlank() -> "Email is required"
    !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address"
    else -> null
}

internal fun validatePassword(password: String): String? = when {
    password.length < MIN_PASSWORD_LENGTH -> "Password must be at least $MIN_PASSWORD_LENGTH characters"
    else -> null
}

internal const val MIN_PASSWORD_LENGTH = 8
