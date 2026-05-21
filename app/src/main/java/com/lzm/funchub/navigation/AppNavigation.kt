package com.lzm.funchub.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lzm.funchub.home.HomeScreen
import com.lzm.funchub.registry.FeatureRegistry

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onFeatureClick = { feature ->
                    navController.navigate(feature.route)
                }
            )
        }

        for (feature in FeatureRegistry.features) {
            composable(feature.route) {
                feature.Screen(onBack = { navController.popBackStack() })
            }
        }
    }
}
