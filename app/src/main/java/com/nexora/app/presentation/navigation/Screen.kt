package com.nexora.app.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash_screen")
    object Onboarding : Screen("onboarding_screen?hasToken={hasToken}") {
        fun createRoute(hasToken: Boolean) = "onboarding_screen?hasToken=$hasToken"
    }
    object Discovery : Screen("discovery_screen")
    object CreatePool : Screen("create_pool_screen")
    object Login : Screen("login_screen")
    object Signup : Screen("signup_screen")
    object PoolDetail : Screen("pool_detail_screen/{poolId}") {
        fun createRoute(poolId: String) = "pool_detail_screen/$poolId"
    }
    object ActivePool : Screen("active_pool_screen")
}
