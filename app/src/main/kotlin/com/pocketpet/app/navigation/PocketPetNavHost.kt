package com.pocketpet.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pocketpet.feature.home.HomeScreen
import com.pocketpet.feature.onboarding.PermissionsScreen
import com.pocketpet.feature.onboarding.WelcomeScreen
import com.pocketpet.feature.settings.CustomizationScreen
import com.pocketpet.feature.settings.PrivacyAboutScreen
import com.pocketpet.feature.settings.SettingsScreen

@Composable
fun PocketPetNavHost(startDestination: String, modifier: Modifier = Modifier) {
    val navController: NavHostController = rememberNavController()
    val context = LocalContext.current
    val appVersionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
    }

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable(AppRoutes.WELCOME) {
            WelcomeScreen(onGetStarted = { navController.navigate(AppRoutes.PERMISSIONS) })
        }
        composable(AppRoutes.PERMISSIONS) {
            PermissionsScreen(
                onFinished = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.WELCOME) { inclusive = true }
                    }
                },
            )
        }
        composable(AppRoutes.HOME) {
            HomeScreen(
                onOpenCustomization = { navController.navigate(AppRoutes.CUSTOMIZATION) },
                onOpenSettings = { navController.navigate(AppRoutes.SETTINGS) },
            )
        }
        composable(AppRoutes.CUSTOMIZATION) {
            CustomizationScreen()
        }
        composable(AppRoutes.SETTINGS) {
            SettingsScreen(onOpenPrivacyAbout = { navController.navigate(AppRoutes.PRIVACY_ABOUT) })
        }
        composable(AppRoutes.PRIVACY_ABOUT) {
            PrivacyAboutScreen(appVersionName = appVersionName)
        }
    }
}
