package com.unh.fitnesstracker.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unh.fitnesstracker.ui.components.ErrorBanner
import com.unh.fitnesstracker.ui.components.FormTextField
import com.unh.fitnesstracker.ui.components.PasswordTextField

/** S3 Create Account. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel,
    onBack: () -> Unit,
    onAccountCreated: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.accountCreated) {
        if (state.accountCreated) {
            onAccountCreated()
            viewModel.onNavigated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.bannerError?.let { ErrorBanner(it) }

            FormTextField(
                value = state.fullName,
                onValueChange = { viewModel.onFieldChange(SignUpField.NAME, it) },
                label = "Full name",
                placeholder = "Your name",
                error = state.errors[SignUpField.NAME],
            )
            FormTextField(
                value = state.email,
                onValueChange = { viewModel.onFieldChange(SignUpField.EMAIL, it) },
                label = "Email",
                placeholder = "name@email.com",
                error = state.errors[SignUpField.EMAIL],
                keyboardType = KeyboardType.Email,
            )
            PasswordTextField(
                value = state.password,
                onValueChange = { viewModel.onFieldChange(SignUpField.PASSWORD, it) },
                label = "Password",
                placeholder = "At least $MIN_PASSWORD_LENGTH characters",
                error = state.errors[SignUpField.PASSWORD],
                imeAction = ImeAction.Next,
            )
            PasswordTextField(
                value = state.confirmPassword,
                onValueChange = { viewModel.onFieldChange(SignUpField.CONFIRM, it) },
                label = "Confirm password",
                placeholder = "Re-enter password",
                error = state.errors[SignUpField.CONFIRM],
                imeAction = ImeAction.Next,
            )

            Text(
                "Body stats (optional)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "Used for calorie estimates. You can change them later in Profile.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormTextField(
                    value = state.age,
                    onValueChange = { viewModel.onFieldChange(SignUpField.AGE, it) },
                    label = "Age",
                    error = state.errors[SignUpField.AGE],
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                FormTextField(
                    value = state.height,
                    onValueChange = { viewModel.onFieldChange(SignUpField.HEIGHT, it) },
                    label = "Height cm",
                    error = state.errors[SignUpField.HEIGHT],
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                FormTextField(
                    value = state.weight,
                    onValueChange = { viewModel.onFieldChange(SignUpField.WEIGHT, it) },
                    label = "Weight kg",
                    error = state.errors[SignUpField.WEIGHT],
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.agreedToTerms, onCheckedChange = viewModel::onTermsChange)
                Text("I agree to the Terms & Privacy Policy")
            }
            state.errors[SignUpField.TERMS]?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = viewModel::createAccount,
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create Account")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Already have an account?")
                TextButton(onClick = onBack) { Text("Log in") }
            }
        }
    }
}
