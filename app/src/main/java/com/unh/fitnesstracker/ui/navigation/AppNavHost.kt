package com.unh.fitnesstracker.ui.navigation

import androidx.compose.runtime.Composable
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
import com.unh.fitnesstracker.ui.main.MainScreen
import com.unh.fitnesstracker.ui.onboarding.PermissionsScreen

/** Top-level navigation: the log-in screens, then [MainScreen] for everything behind log-in. */
@Composable
fun AppNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    val startDestination =
        if (container.authRepository.currentUser == null) Routes.LOGIN else Routes.MAIN

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            val vm: LoginViewModel = viewModel {
                LoginViewModel(container.authRepository, container.userRepository)
            }
            LoginScreen(
                viewModel = vm,
                onLoggedIn = { navController.navigateClearingBackStack(Routes.MAIN) },
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
                    navController.navigateClearingBackStack(Routes.MAIN)
                },
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                container = container,
                onLoggedOut = { navController.navigateClearingBackStack(Routes.LOGIN) },
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
