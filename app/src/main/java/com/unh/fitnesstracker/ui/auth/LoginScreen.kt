package com.unh.fitnesstracker.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unh.fitnesstracker.ui.components.ErrorBanner
import com.unh.fitnesstracker.ui.components.FormTextField
import com.unh.fitnesstracker.ui.components.PasswordTextField
import kotlinx.coroutines.launch

/** S1 Log In, and S2 when [LoginUiState.bannerError] is set. */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoggedIn: () -> Unit,
    onNewGoogleUser: () -> Unit,
    onSignUpClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.destination) {
        when (state.destination) {
            LoginDestination.HOME -> onLoggedIn()
            LoginDestination.ONBOARDING -> onNewGoogleUser()
            null -> return@LaunchedEffect
        }
        viewModel.onNavigated()
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onSnackbarShown()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Text("Fitness Tracker", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))

            state.bannerError?.let { ErrorBanner(it) }

            FormTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                placeholder = "name@email.com",
                error = state.emailError,
                keyboardType = KeyboardType.Email,
            )
            PasswordTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Password",
                error = state.passwordError,
                imeAction = ImeAction.Done,
            )

            TextButton(onClick = viewModel::sendPasswordReset, modifier = Modifier.align(Alignment.End)) {
                Text("Forgot password?")
            }

            Button(
                onClick = viewModel::logIn,
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("Log In")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f))
                Text("  or  ", style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider(Modifier.weight(1f))
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        when (val outcome = GoogleSignInHelper.requestIdToken(context)) {
                            is GoogleSignInOutcome.Success -> viewModel.signInWithGoogle(outcome.idToken)
                            is GoogleSignInOutcome.Failure -> viewModel.onGoogleSignInError(outcome.message)
                            GoogleSignInOutcome.Cancelled -> Unit
                        }
                    }
                },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("Continue with Google")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Don't have an account?")
                TextButton(onClick = onSignUpClick) { Text("Sign up") }
            }
        }
    }
}
