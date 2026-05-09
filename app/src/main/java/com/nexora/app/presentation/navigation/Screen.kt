package com.nexora.app.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash_screen")
    object Onboarding : Screen("onboarding_screen")
    object Discovery : Screen("discovery_screen")
    object CreatePool : Screen("create_pool_screen")
}
