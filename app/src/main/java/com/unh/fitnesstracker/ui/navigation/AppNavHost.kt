package com.unh.fitnesstracker.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unh.fitnesstracker.AppContainer
import com.unh.fitnesstracker.ui.auth.LoginScreen
import com.unh.fitnesstracker.ui.auth.LoginViewModel
import com.unh.fitnesstracker.ui.auth.SignUpScreen
import com.unh.fitnesstracker.ui.auth.SignUpViewModel
import com.unh.fitnesstracker.ui.home.HomeScreen
import com.unh.fitnesstracker.ui.onboarding.PermissionsScreen

@Composable
fun AppNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    val startDestination =
        if (container.authRepository.currentUser == null) Routes.LOGIN else Routes.HOME

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            val vm: LoginViewModel = viewModel {
                LoginViewModel(container.authRepository, container.userRepository)
            }
            LoginScreen(
                viewModel = vm,
                onLoggedIn = { navController.navigateClearingBackStack(Routes.HOME) },
                onNewGoogleUser = { navController.navigateClearingBackStack(Routes.PERMISSIONS) },
                onSignUpClick = { navController.navigate(Routes.SIGN_UP) },
            )
        }

        composable(Routes.SIGN_UP) {
            val vm: SignUpViewModel = viewModel {
                SignUpViewModel(container.authRepository, container.userRepository)
            }
            SignUpScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onAccountCreated = { navController.navigateClearingBackStack(Routes.PERMISSIONS) },
            )
        }

        composable(Routes.PERMISSIONS) {
            PermissionsScreen(
                onFinished = {
                    container.authRepository.currentUser?.uid?.let { uid ->
                        container.markOnboardingComplete(uid)
                    }
                    navController.navigateClearingBackStack(Routes.HOME)
                },
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                displayName = container.authRepository.currentUser?.displayName,
                onLogOut = {
                    container.authRepository.signOut()
                    navController.navigateClearingBackStack(Routes.LOGIN)
                },
            )
        }
    }
}

/** Navigates to [route] and removes everything behind it, so Back can't return to a logged-out screen. */
private fun NavHostController.navigateClearingBackStack(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun FirebaseMissingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Firebase is not set up", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Add app/google-services.json from the Firebase console, then sync Gradle and run again.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
