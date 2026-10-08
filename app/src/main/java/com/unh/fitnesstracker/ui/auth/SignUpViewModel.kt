package com.unh.fitnesstracker.ui.auth

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

data class SignUpUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val age: String = "",
    val height: String = "",
    val weight: String = "",
    val agreedToTerms: Boolean = false,
    val errors: Map<SignUpField, String> = emptyMap(),
    val bannerError: String? = null,
    val isLoading: Boolean = false,
    val accountCreated: Boolean = false,
)

enum class SignUpField { NAME, EMAIL, PASSWORD, CONFIRM, AGE, HEIGHT, WEIGHT, TERMS }

class SignUpViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onFieldChange(field: SignUpField, value: String) = _uiState.update {
        val updated = when (field) {
            SignUpField.NAME -> it.copy(fullName = value)
            SignUpField.EMAIL -> it.copy(email = value)
            SignUpField.PASSWORD -> it.copy(password = value)
            SignUpField.CONFIRM -> it.copy(confirmPassword = value)
            SignUpField.AGE -> it.copy(age = value.filter(Char::isDigit))
            SignUpField.HEIGHT -> it.copy(height = value.filter { c -> c.isDigit() || c == '.' })
            SignUpField.WEIGHT -> it.copy(weight = value.filter { c -> c.isDigit() || c == '.' })
            SignUpField.TERMS -> it
        }
        updated.copy(errors = it.errors - field, bannerError = null)
    }

    fun onTermsChange(agreed: Boolean) = _uiState.update {
        it.copy(agreedToTerms = agreed, errors = it.errors - SignUpField.TERMS, bannerError = null)
    }

    fun createAccount() {
        val state = _uiState.value
        val errors = validate(state)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors, bannerError = "Please fix the highlighted fields") }
            return
        }

        _uiState.update { it.copy(isLoading = true, bannerError = null) }
        viewModelScope.launch {
            runCatching {
                val user = authRepository.signUp(state.fullName.trim(), state.email, state.password)
                userRepository.createProfile(
                    UserProfile(
                        uid = user.uid,
                        fullName = state.fullName.trim(),
                        email = state.email.trim(),
                        age = state.age.toIntOrNull(),
                        heightCm = state.height.toDoubleOrNull(),
                        weightKg = state.weight.toDoubleOrNull(),
                    )
                )
            }
                .onSuccess { _uiState.update { it.copy(isLoading = false, accountCreated = true) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, bannerError = e.toAuthMessage()) }
                }
        }
    }

    fun onNavigated() = _uiState.update { it.copy(accountCreated = false) }

    private fun validate(s: SignUpUiState): Map<SignUpField, String> = buildMap {
        if (s.fullName.isBlank()) put(SignUpField.NAME, "Enter your name")
        validateEmail(s.email)?.let { put(SignUpField.EMAIL, it) }
        validatePassword(s.password)?.let { put(SignUpField.PASSWORD, it) }
        if (s.confirmPassword != s.password) put(SignUpField.CONFIRM, "Passwords don't match")
        if (s.age.isNotEmpty() && s.age.toInt() !in 10..120) put(SignUpField.AGE, "10–120")
        if (s.height.isNotEmpty() && s.height.toDoubleOrNull()?.let { it in 50.0..250.0 } != true) {
            put(SignUpField.HEIGHT, "50–250 cm")
        }
        if (s.weight.isNotEmpty() && s.weight.toDoubleOrNull()?.let { it in 20.0..400.0 } != true) {
            put(SignUpField.WEIGHT, "20–400 kg")
        }
        if (!s.agreedToTerms) put(SignUpField.TERMS, "You must agree to continue")
    }
}
