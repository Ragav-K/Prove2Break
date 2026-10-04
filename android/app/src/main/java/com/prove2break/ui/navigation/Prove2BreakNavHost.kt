package com.prove2break.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.prove2break.ui.analytics.AnalyticsScreen
import com.prove2break.ui.apprestriction.AppSelectionScreen
import com.prove2break.ui.apprestriction.BlockedAppScreen
import com.prove2break.ui.apprestriction.EmergencyUnlockScreen
import com.prove2break.ui.apprestriction.PermissionRationaleScreen
import com.prove2break.ui.auth.LoginScreen
import com.prove2break.ui.auth.RegisterScreen
import com.prove2break.ui.auth.SessionObserverViewModel
import com.prove2break.ui.auth.SplashScreen
import com.prove2break.ui.breaks.BreakActiveScreen
import com.prove2break.ui.breaks.RapidFireQuizScreen
import com.prove2break.ui.breaks.TopicChecklistScreen
import com.prove2break.ui.common.PlaceholderAction
import com.prove2break.ui.common.PlaceholderScreen
import com.prove2break.ui.focus.FocusModeScreen
import com.prove2break.ui.focus.FocusSetupScreen
import com.prove2break.ui.material.SubjectDetailScreen
import com.prove2break.ui.material.TopicListScreen
import com.prove2break.ui.settings.SettingsScreen
import com.prove2break.ui.subject.SubjectEditScreen
import com.prove2break.ui.subject.SubjectsListScreen

/**
 * Root navigation graph. Placeholder screens stand in for the full feature
 * screens (see UX-UI.md Screen Inventory); each will be replaced by its own
 * Composable + ViewModel as that feature is implemented. Buttons wire real
 * navigation between them so the app is click-through-able end to end.
 *
 * Mirrors the top-level graphs described in ANDROID-ARCHITECTURE.md
 * (AuthGraph, MainGraph, FocusGraph) as a single flat NavHost for this
 * scaffolding pass; these will be split into nested nav graphs as the
 * corresponding features are built out.
 */
@Composable
fun Prove2BreakNavHost(navController: NavHostController) {
    // Reacts to AuthAuthenticator/AuthSessionManager declaring the session unrecoverable
    // (refresh token missing/expired/rejected) by clearing the whole back stack back to Login,
    // per AUTHENTICATION.md's Session Expiration Handling — no silently-failing screens left behind.
    val sessionObserverViewModel: SessionObserverViewModel = hiltViewModel()
    LaunchedEffect(Unit) {
        sessionObserverViewModel.forceLogoutEvents.collect {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onAuthenticated = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onUnauthenticated = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() },
            )
        }
        composable(Screen.Dashboard.route) {
            PlaceholderScreen(
                title = "Dashboard",
                actions = listOf(
                    PlaceholderAction("View Subjects") { navController.navigate(Screen.Subjects.route) },
                    PlaceholderAction("Start Focus Session") { navController.navigate(Screen.FocusSetup.route) },
                    PlaceholderAction("Restrict Apps") { navController.navigate(Screen.AppRestrictionRationale.route) },
                    PlaceholderAction("Progress") { navController.navigate(Screen.Analytics.route) },
                    PlaceholderAction("Settings") { navController.navigate(Screen.Settings.route) },
                ),
            )
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Subjects.route) {
            SubjectsListScreen(
                onSubjectClick = { subject ->
                    navController.navigate(Screen.SubjectDetail.createRoute(subject.id))
                },
                onCreateSubject = { navController.navigate(Screen.SubjectCreate.route) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.SubjectCreate.route) {
            SubjectEditScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.SubjectEdit.route,
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType }),
        ) {
            SubjectEditScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.SubjectDetail.route,
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType }),
        ) {
            SubjectDetailScreen(
                onEditSubject = { subjectId -> navController.navigate(Screen.SubjectEdit.createRoute(subjectId)) },
                onViewTopics = { subjectId -> navController.navigate(Screen.TopicList.createRoute(subjectId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.TopicList.route,
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType }),
        ) {
            TopicListScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.FocusSetup.route) {
            FocusSetupScreen(
                onSessionStarted = { sessionId ->
                    navController.navigate(Screen.FocusMode.createRoute(sessionId)) {
                        popUpTo(Screen.FocusSetup.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
                onCreateSubject = { navController.navigate(Screen.SubjectCreate.route) },
            )
        }
        composable(
            route = Screen.FocusMode.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            FocusModeScreen(
                sessionId = sessionId,
                onSessionEnded = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onTakeBreak = {
                    navController.navigate(Screen.BreakActive.createRoute(sessionId))
                },
                onBreakNow = { subjectId ->
                    navController.navigate(Screen.BreakTopicChecklist.createRoute(sessionId, subjectId))
                },
            )
        }
        composable(
            route = Screen.BreakActive.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            BreakActiveScreen(
                onBreakEnded = {
                    navController.navigate(Screen.FocusMode.createRoute(sessionId)) {
                        popUpTo(Screen.FocusMode.createRoute(sessionId)) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Screen.BreakTopicChecklist.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.LongType },
                navArgument("subjectId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            val subjectId = backStackEntry.arguments?.getString("subjectId").orEmpty()
            TopicChecklistScreen(
                onVerify = { topicIds ->
                    navController.navigate(Screen.Quiz.createRoute(sessionId, subjectId, topicIds))
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.Quiz.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.LongType },
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("topicIds") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            RapidFireQuizScreen(
                onResolved = { passed, _ ->
                    // BreakManager.resolveQuizResult already unlocked/started the break on
                    // pass (BREAK_ACTIVE) or returned the session to FOCUSING on fail
                    // (QUIZ-SYSTEM.md "Scoring") — navigate to match.
                    val destination = if (passed) Screen.BreakActive.createRoute(sessionId) else Screen.FocusMode.createRoute(sessionId)
                    navController.navigate(destination) {
                        popUpTo(Screen.FocusMode.createRoute(sessionId)) { inclusive = true }
                    }
                },
                onError = {
                    navController.navigate(Screen.FocusMode.createRoute(sessionId)) {
                        popUpTo(Screen.FocusMode.createRoute(sessionId)) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.AppRestrictionRationale.route) {
            PermissionRationaleScreen(
                onContinue = { navController.navigate(Screen.AppSelection.route) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.AppSelection.route) {
            AppSelectionScreen(
                onDone = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = "blocked_app/{packageName}/{sessionId}",
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType },
                navArgument("sessionId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName").orEmpty()
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            BlockedAppScreen(
                blockedAppLabel = packageName,
                onReturnToProve2Break = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onRequestEmergencyAccess = {
                    navController.navigate("emergency_unlock/$packageName/$sessionId")
                },
            )
        }
        composable(
            route = "emergency_unlock/{packageName}/{sessionId}",
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType },
                navArgument("sessionId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName").orEmpty()
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            EmergencyUnlockScreen(
                packageName = packageName,
                sessionId = sessionId,
                onGranted = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
