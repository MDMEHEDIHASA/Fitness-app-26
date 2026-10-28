package com.unh.fitnesstracker.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.unh.fitnesstracker.AppContainer
import com.unh.fitnesstracker.data.model.UnitSystem
import com.unh.fitnesstracker.ui.components.LogOutDialog
import com.unh.fitnesstracker.ui.home.HomeScreen
import com.unh.fitnesstracker.ui.home.HomeViewModel
import com.unh.fitnesstracker.ui.navigation.OutdoorType
import com.unh.fitnesstracker.ui.navigation.Routes
import com.unh.fitnesstracker.ui.profile.ProfileScreen
import com.unh.fitnesstracker.ui.profile.ProfileViewModel
import com.unh.fitnesstracker.ui.workout.ActiveWorkoutScreen
import com.unh.fitnesstracker.ui.workout.ActiveWorkoutViewModel
import com.unh.fitnesstracker.ui.workout.AddExerciseScreen
import com.unh.fitnesstracker.ui.workout.AddExerciseViewModel
import com.unh.fitnesstracker.ui.workout.StartWorkoutSheet
import com.unh.fitnesstracker.ui.workout.StartWorkoutViewModel
import com.unh.fitnesstracker.ui.workout.WorkoutSummaryScreen
import com.unh.fitnesstracker.ui.workout.WorkoutsScreen
import kotlinx.coroutines.launch

/**
 * The logged-in part of the app: drawer (S6), bottom tabs, start-workout sheet (S7) and
 * log-out dialog (S29) around an inner NavHost holding every main screen.
 */
@Composable
fun MainScreen(container: AppContainer, onLoggedOut: () -> Unit) {
    val uid = container.authRepository.currentUser?.uid
    if (uid == null) {
        LaunchedEffect(Unit) { onLoggedOut() }
        return
    }

    val profileViewModel: ProfileViewModel = viewModel {
        ProfileViewModel(uid, container.userRepository, container.authRepository)
    }
    val profile by profileViewModel.profile.collectAsStateWithLifecycle()
    val startWorkoutViewModel: StartWorkoutViewModel = viewModel {
        StartWorkoutViewModel(uid, container.templateRepository, container.workoutSession)
    }
    val templates by startWorkoutViewModel.templates.collectAsStateWithLifecycle()
    val activeWorkout by startWorkoutViewModel.activeWorkout.collectAsStateWithLifecycle()
    val units = profile?.unitSystem ?: UnitSystem.METRIC

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onTab = currentRoute in bottomTabRoutes

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showStartSheet by rememberSaveable { mutableStateOf(false) }
    var showLogOutDialog by rememberSaveable { mutableStateOf(false) }

    // Until Sprint 10 there is never a paired watch.
    val watchConnected = false

    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }
    val goBack: () -> Unit = { navController.popBackStack() }
    val openStartSheet: () -> Unit = { showStartSheet = true }
    val openStrengthWorkout: () -> Unit = { navController.navigate(Routes.STRENGTH_WORKOUT) { launchSingleTop = true } }

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Swipe-to-open only on the tab screens, so it doesn't fight with ← screens.
        gesturesEnabled = onTab || drawerState.isOpen,
        drawerContent = {
            AppDrawer(
                profile = profile,
                currentRoute = currentRoute,
                watchConnected = watchConnected,
                onItemClick = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigateFromAnywhere(route)
                },
                onLogOutClick = {
                    scope.launch { drawerState.close() }
                    showLogOutDialog = true
                },
            )
        },
    ) {
        Scaffold(
            bottomBar = {
                if (onTab) BottomBar(currentRoute) { navController.navigateToTab(it) }
            },
            contentWindowInsets = WindowInsets(0),
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier
                    .padding(padding)
                    .then(if (onTab) Modifier else Modifier.navigationBarsPadding()),
            ) {
                composable(Routes.HOME) {
                    val vm: HomeViewModel = viewModel {
                        HomeViewModel(uid, container.userRepository, container.workoutRepository, container.workoutSession)
                    }
                    HomeScreen(
                        viewModel = vm,
                        onMenuClick = openDrawer,
                        onAvatarClick = { navController.navigateToTab(Routes.PROFILE) },
                        onWatchClick = { navController.navigate(Routes.SMARTWATCH) },
                        onMetricClick = { navController.navigateToTab(Routes.PROGRESS) },
                        onGoalsClick = { navController.navigate(Routes.GOALS) },
                        onRemindersClick = { navController.navigate(Routes.REMINDERS) },
                        onStartWorkout = openStartSheet,
                        onResumeWorkout = openStrengthWorkout,
                    )
                }
                composable(Routes.WORKOUTS) {
                    WorkoutsScreen(onMenuClick = openDrawer, onStartWorkout = openStartSheet)
                }
                composable(Routes.PROGRESS) {
                    ComingSoonScreen(
                        title = "Progress",
                        icon = Icons.AutoMirrored.Filled.ShowChart,
                        sprint = 5,
                        description = "Charts for steps, heart rate, workouts, distance, weight and volume, plus personal records.",
                        nav = TopBarNav.Menu(openDrawer),
                    )
                }
                composable(Routes.PROFILE) {
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onMenuClick = openDrawer,
                        onNavigate = { navController.navigate(it) },
                        onLogOutClick = { showLogOutDialog = true },
                    )
                }
                composable(Routes.GOALS) {
                    ComingSoonScreen(
                        title = "Goals", icon = Icons.Filled.Flag, sprint = 6,
                        description = "Set step, workout, distance, heart-rate and weight goals that update on their own.",
                        nav = TopBarNav.Back(goBack),
                    )
                }
                composable(Routes.REMINDERS) {
                    ComingSoonScreen(
                        title = "Reminders", icon = Icons.Filled.Notifications, sprint = 7,
                        description = "Scheduled workout reminders with Start and Snooze, plus step and inactivity alerts.",
                        nav = TopBarNav.Back(goBack),
                    )
                }
                composable(Routes.SMARTWATCH) {
                    ComingSoonScreen(
                        title = "Smartwatch", icon = Icons.Filled.Watch, sprint = 10,
                        description = "No watch connected. Pair a Wear OS watch to stream heart rate and control workouts.",
                        nav = TopBarNav.Back(goBack),
                    )
                }
                composable(Routes.HELP) {
                    HelpScreen(onBack = goBack)
                }
                composable(Routes.STRENGTH_WORKOUT) {
                    val vm: ActiveWorkoutViewModel = viewModel {
                        ActiveWorkoutViewModel(uid, container.workoutSession, container.workoutRepository, container.templateRepository)
                    }
                    ActiveWorkoutScreen(
                        viewModel = vm,
                        onAddExercise = { navController.navigate(Routes.ADD_EXERCISE) { launchSingleTop = true } },
                        onFinished = {
                            navController.navigate(Routes.WORKOUT_SUMMARY) {
                                popUpTo(Routes.STRENGTH_WORKOUT) { inclusive = true }
                            }
                        },
                        onClose = goBack,
                    )
                }
                composable(Routes.ADD_EXERCISE) {
                    val vm: AddExerciseViewModel = viewModel {
                        AddExerciseViewModel(uid, container.exerciseRepository, container.workoutSession)
                    }
                    AddExerciseScreen(viewModel = vm, onBack = goBack)
                }
                composable(Routes.WORKOUT_SUMMARY) {
                    WorkoutSummaryScreen(
                        uid = uid,
                        session = container.workoutSession,
                        workoutRepository = container.workoutRepository,
                        units = units,
                        onDone = goBack,
                    )
                }
                composable(
                    route = Routes.OUTDOOR_WORKOUT,
                    arguments = listOf(navArgument(Routes.OUTDOOR_TYPE_ARG) { type = NavType.StringType }),
                ) { entry ->
                    val type = OutdoorType.valueOf(entry.arguments?.getString(Routes.OUTDOOR_TYPE_ARG) ?: OutdoorType.RUN.name)
                    ComingSoonScreen(
                        title = type.label, icon = Icons.AutoMirrored.Filled.DirectionsRun, sprint = 8,
                        description = "GPS tracking with a live route map, distance, pace and splits.",
                        nav = TopBarNav.Back(goBack),
                    )
                }
            }
        }
    }

    if (showStartSheet) {
        StartWorkoutSheet(
            watchConnected = watchConnected,
            templates = templates,
            workoutInProgress = activeWorkout != null,
            onDismiss = { showStartSheet = false },
            onStrengthClick = {
                showStartSheet = false
                startWorkoutViewModel.startStrength(profile)
                openStrengthWorkout()
            },
            onTemplateClick = { template ->
                showStartSheet = false
                startWorkoutViewModel.startStrength(profile, template)
                openStrengthWorkout()
            },
            onOutdoorClick = { type ->
                showStartSheet = false
                navController.navigate(Routes.outdoorWorkout(type))
            },
        )
    }

    if (showLogOutDialog) {
        LogOutDialog(
            onConfirm = {
                showLogOutDialog = false
                container.workoutSession.discard()
                container.workoutSession.clearLastSaved()
                container.authRepository.signOut()
                onLoggedOut()
            },
            onDismiss = { showLogOutDialog = false },
        )
    }
}

/** Switches bottom tabs, keeping one back stack per tab (the standard bottom-navigation behavior). */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.navigateFromAnywhere(route: String) {
    if (route in bottomTabRoutes) navigateToTab(route) else navigate(route) { launchSingleTop = true }
}
