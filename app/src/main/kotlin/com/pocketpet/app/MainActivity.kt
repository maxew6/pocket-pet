package com.pocketpet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pocketpet.app.navigation.AppRoutes
import com.pocketpet.app.navigation.PocketPetNavHost
import com.pocketpet.core.designsystem.theme.PocketPetTheme
import com.pocketpet.core.model.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition { viewModel.hasCompletedOnboarding.value == null }

        setContent {
            val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()
            val theme by viewModel.theme.collectAsState()
            val completed = hasCompletedOnboarding

            if (completed != null) {
                val darkTheme = when (theme) {
                    AppTheme.Light -> false
                    AppTheme.Dark -> true
                    AppTheme.System -> isSystemInDarkTheme()
                }
                PocketPetTheme(darkTheme = darkTheme) {
                    Surface {
                        PocketPetNavHost(
                            startDestination = if (completed) AppRoutes.HOME else AppRoutes.WELCOME,
                        )
                    }
                }
            }
        }
    }
}
