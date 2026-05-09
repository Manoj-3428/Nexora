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
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Discovery.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(route = Screen.Onboarding.route) {
            OnboardingScreen(
                onFinishOnboarding = {
                    navController.navigate(Screen.Discovery.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(route = Screen.Discovery.route) {
            DiscoveryScreen(
                onCreatePoolClick = {
                    navController.navigate(Screen.CreatePool.route)
                }
            )
        }

        composable(route = Screen.CreatePool.route) {
            CreatePoolScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToActivePool = {
                    navController.popBackStack()
                }
            )
        }
    }
}
