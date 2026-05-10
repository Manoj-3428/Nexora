package com.nexora.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexora.app.presentation.screens.create_pool.CreatePoolScreen
import com.nexora.app.presentation.screens.discovery.DiscoveryScreen
import com.nexora.app.presentation.screens.onboarding.OnboardingScreen
import com.nexora.app.presentation.screens.splash.SplashScreen
import com.nexora.app.presentation.screens.auth.LoginScreen
import com.nexora.app.presentation.screens.auth.SignupScreen
import com.nexora.app.presentation.screens.pool_detail.PoolDetailScreen
import com.nexora.app.presentation.screens.active_pool.ActivePoolScreen

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onNavigateToOnboarding = { hasToken ->
                    navController.navigate(Screen.Onboarding.createRoute(hasToken)) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Discovery.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = Screen.Onboarding.route,
            arguments = listOf(
                androidx.navigation.navArgument("hasToken") {
                    type = androidx.navigation.NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val hasToken = backStackEntry.arguments?.getBoolean("hasToken") ?: false
            OnboardingScreen(
                onFinishOnboarding = {
                    if (hasToken) {
                        navController.navigate(Screen.Discovery.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToSignup = { navController.navigate(Screen.Signup.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Discovery.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Signup.route) {
            SignupScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onSignupSuccess = {
                    navController.navigate(Screen.Discovery.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(route = Screen.Discovery.route) {
            DiscoveryScreen(
                onCreatePoolClick = {
                    navController.navigate(Screen.CreatePool.route)
                },
                onNavigateToPoolDetail = { poolId ->
                    navController.navigate(Screen.PoolDetail.createRoute(poolId))
                }
            )
        }

        composable(route = Screen.CreatePool.route) {
            CreatePoolScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToActivePool = {
                    navController.navigate(Screen.ActivePool.route) {
                        popUpTo(Screen.CreatePool.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.ActivePool.route) {
            ActivePoolScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PoolDetail.route,
            arguments = listOf(
                androidx.navigation.navArgument("poolId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) {
            PoolDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
